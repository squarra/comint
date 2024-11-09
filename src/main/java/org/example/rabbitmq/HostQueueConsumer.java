package org.example.rabbitmq;

import com.rabbitmq.client.*;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.host.Host;
import org.example.host.HostStateService;
import org.example.messaging.MessageSender;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class HostQueueConsumer {

    private final HostStateService hostStateService;
    private final MessageSender messageSender;
    private final ConnectionFactory connectionFactory;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, Channel> hostChannels;
    private Connection connection;

    @Inject
    public HostQueueConsumer(
            HostStateService hostStateService,
            MessageSender messageSender,
            @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String rabbitMQHost,
            @ConfigProperty(name = "rabbitmq.consumer.threads", defaultValue = "4") int numThreads) {
        this.hostStateService = hostStateService;
        this.messageSender = messageSender;
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(rabbitMQHost);
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.executorService = Executors.newFixedThreadPool(numThreads);
        this.hostChannels = new ConcurrentHashMap<>();
    }

    @PostConstruct
    void init() {
        try {
            connection = connectionFactory.newConnection(executorService);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    public void startConsumingHostQueue(Host host) {
        MDC.clear();
        MDC.put("Host", host.getName());
        try {
            Channel channel = connection.createChannel();
            channel.queueDeclare(host.getName(), true, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageId = delivery.getProperties().getMessageId();
                MDC.put("Host", host.getName());
                MDC.put("MessageID", messageId);
                Log.info("Consumed message from HostQueue");
                try {
                    String xmlContent = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    Document message = parseXmlContent(xmlContent);

                    boolean success = messageSender.sendMessage(host, messageId, message.getDocumentElement());
                    if (success) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } else {
                        channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                        hostStateService.messageDeliveryFailure(host);
                    }
                } catch (Exception e) {
                    channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                    Log.errorf("Failed to process message: %s", e.getMessage());
                }
            };

            CancelCallback cancelCallback = consumerTag -> Log.info("Consumer cancelled for queue");

            channel.basicQos(1);
            channel.basicConsume(host.getName(), false, deliverCallback, cancelCallback);
            hostChannels.put(host.getName(), channel);

            Log.info("Started consuming messages");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start consuming from queue: " + host.getName(), e);
        }
    }

    private Document parseXmlContent(String xmlContent) throws Exception {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlContent)));
    }

    void onStop(@Observes ShutdownEvent ev) {
        try {
            for (Channel channel : hostChannels.values()) {
                if (channel.isOpen()) {
                    channel.close();
                }
            }
            hostChannels.clear();

            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            executorService.shutdown();

            Log.info("RabbitMQ consumer resources closed successfully");
        } catch (IOException | TimeoutException e) {
            Log.error("Error closing RabbitMQ consumer resources", e);
        }
    }
}
