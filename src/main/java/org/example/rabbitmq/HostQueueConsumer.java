package org.example.rabbitmq;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import io.quarkus.logging.Log;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.MessageType;
import org.example.host.Host;
import org.example.host.HostStateService;
import org.example.inbound.InboundMessageSender;
import org.example.logging.MDCKeys;
import org.example.messaging.UICMessageSender;
import org.jboss.logmanager.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class HostQueueConsumer {

    private final HostStateService hostStateService;
    private final InboundMessageSender inboundMessageSender;
    private final RabbitMQService rabbitMQService;
    private final UICMessageSender uicMessageSender;
    private final ExecutorService executorService;

    @Inject
    public HostQueueConsumer(
            HostStateService hostStateService,
            InboundMessageSender inboundMessageSender,
            RabbitMQService rabbitMQService,
            UICMessageSender uicMessageSender
    ) {
        this.hostStateService = hostStateService;
        this.inboundMessageSender = inboundMessageSender;
        this.rabbitMQService = rabbitMQService;
        this.uicMessageSender = uicMessageSender;

        ThreadFactory factory = Thread.ofVirtual().name("virtual-sender-", 0).factory();
        this.executorService = Executors.newThreadPerTaskExecutor(factory);
    }

    public void startConsuming(Host host) {
        MDC.put(MDCKeys.HOST_NAME, host.getName());
        Channel channel = rabbitMQService.getChannel();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            MDC.put(MDCKeys.HOST_NAME, host.getName());
            String messageId = delivery.getProperties().getMessageId();
            String messageType = delivery.getProperties().getType();
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            MDC.put(MDCKeys.MESSAGE_ID, messageId);
            Log.debug("Consumed message from HostQueue");

            CompletableFuture.supplyAsync(() -> {
                try {
                    if (messageType.equals(MessageType.UICMessage.name())) {
                        return uicMessageSender.sendMessage(host, messageId, message);
                    } else {
                        return inboundMessageSender.sendMessage(host, message);
                    }
                } catch (Exception e) {
                    Log.error("Error processing message", e);
                    return false;
                }
            }, executorService).thenAccept(success -> {
                try {
                    if (success) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } else {
                        channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                        hostStateService.messageDeliveryFailure(host);
                    }
                } catch (IOException e) {
                    Log.errorf("Failed to acknowledge message: %s", e.getMessage());
                }
            });
        };

        CancelCallback cancelCallback = consumerTag -> {
            MDC.put(MDCKeys.HOST_NAME, host.getName());
            Log.warn("Consumer cancelled for queue");
        };

        try {
            channel.queueDeclare(host.getName(), true, false, false, null);
            channel.basicQos(10);
            channel.basicConsume(host.getName(), false, deliverCallback, cancelCallback);
            Log.info("Started consuming messages with virtual threads");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void destroy() {
        executorService.close();
    }
}
