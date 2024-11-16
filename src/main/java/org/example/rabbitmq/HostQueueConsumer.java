package org.example.rabbitmq;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import io.quarkus.logging.Log;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.MessageSendException;
import org.example.MessageTypes;
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
    private final UICMessageSender uicMessageSender;
    private final ExecutorService executorService;
    private final Channel channel;

    @Inject
    public HostQueueConsumer(
            HostStateService hostStateService,
            InboundMessageSender inboundMessageSender,
            RabbitMQService rabbitMQService,
            UICMessageSender uicMessageSender
    ) {
        this.hostStateService = hostStateService;
        this.inboundMessageSender = inboundMessageSender;
        this.uicMessageSender = uicMessageSender;
        this.channel = rabbitMQService.getChannel();

        ThreadFactory factory = Thread.ofVirtual().name("virtual-sender-", 0).factory();
        this.executorService = Executors.newThreadPerTaskExecutor(factory);
    }

    public void startConsuming(Host host) {
        MDC.put(MDCKeys.HOST_NAME, host.getName());

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            MDC.put(MDCKeys.HOST_NAME, host.getName());
            String messageId = delivery.getProperties().getMessageId();
            String messageType = delivery.getProperties().getType();
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            MDC.put(MDCKeys.MESSAGE_ID, messageId);

            CompletableFuture.runAsync(() -> processMessage(consumerTag, delivery, host, messageId, messageType, message), executorService)
                    .exceptionally(e -> {
                        Log.error("Unhandled exception during message processing", e);
                        return null;
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

    private void processMessage(String consumerTag, Delivery delivery, Host host, String messageId, String messageType, String message) {
        Log.debug("Processing message");
        try {
            switch (messageType) {
                case MessageTypes.UIC_MESSAGE -> uicMessageSender.sendMessage(host, messageId, message);
                case MessageTypes.INBOUND_MESSAGE -> inboundMessageSender.sendMessage(host, message);
                default -> throw new RuntimeException("Unknown message type: " + messageType);
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (MessageSendException e) {
            handleSendException(consumerTag, delivery, host, e);
        } catch (IOException e) {
            Log.errorf("Failed to acknowledge message: %s", e.getMessage());
        }
    }

    private void handleSendException(String consumerTag, Delivery delivery, Host host, MessageSendException e) {
        try {
            switch (e.getFailureType()) {
                case REQUEST_CREATION_ERROR -> {
                    Log.error("Failed to create request. Deleting message from queue", e);
                    channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                }
                case HOST_UNREACHABLE -> {
                    hostStateService.messageDeliveryFailure(host);
                    Log.error("Failed to deliver message. Host unreachable.");
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    Log.warn("Cancelling consumer for queue: " + host.getName());
                    channel.basicCancel(consumerTag);
                }
                case RESPONSE_PROCESSING_ERROR, MESSAGE_REJECTED -> {
                    Log.error("Message was delivered but there was a processing issue", e);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            }
        } catch (IOException ex) {
            Log.error("Failed to handle message send exception", ex);
            throw new RuntimeException(ex);
        }
    }

    @PreDestroy
    void destroy() {
        Log.info("Closing executorService");
        executorService.close();
    }
}
