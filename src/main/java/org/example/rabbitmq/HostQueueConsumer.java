package org.example.rabbitmq;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
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

@ApplicationScoped
public class HostQueueConsumer {

    private static final String CHANNEL_ID = "consumer";

    private final HostStateService hostStateService;
    private final InboundMessageSender inboundMessageSender;
    private final RabbitMQService rabbitMQService;
    private final UICMessageSender uicMessageSender;
    private Channel channel;

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
    }

    @PostConstruct
    void init() {
        MDC.clear();
        Log.infof("+++++ Initializing %s channel +++++", CHANNEL_ID);
        channel = rabbitMQService.getChannel(CHANNEL_ID);
        Log.infof("Successfully initialized %s channel", CHANNEL_ID);
    }

    public void startConsuming(Host host) {
        MDC.put(MDCKeys.HOST_NAME, host.getName());
        try {
            channel.queueDeclare(host.getName(), true, false, false, null);
            channel.basicQos(1);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageId = delivery.getProperties().getMessageId();
                String messageType = delivery.getProperties().getType();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                MDC.put(MDCKeys.MESSAGE_ID, messageId);
                MDC.put(MDCKeys.HOST_NAME, host.getName());
                Log.info("Consumed message from HostQueue");

                boolean success;
                if (messageType.equals(MessageType.UICMessage.name())) {
                    success = uicMessageSender.sendMessage(host, messageId, message);
                } else {
                    success = inboundMessageSender.sendMessage(host, message);
                }

                if (success) {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } else {
                    channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                    hostStateService.messageDeliveryFailure(host);
                }
            };

            CancelCallback cancelCallback = consumerTag -> Log.info("Consumer cancelled for queue");

            channel.basicConsume(host.getName(), false, deliverCallback, cancelCallback);

            Log.info("Started consuming messages");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start consuming from queue: " + host.getName(), e);
        }
    }
}
