package org.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.MessageSendException;
import org.example.MessageTypes;
import org.example.host.Host;
import org.example.host.HostStateService;
import org.example.inbound.InboundMessageSender;
import org.example.logging.MdcKeys;
import org.example.messaging.UICMessageSender;
import org.example.util.XmlUtilityService;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class HostQueueConsumer implements Consumer {

    private final HostStateService hostStateService;
    private final InboundMessageSender inboundMessageSender;
    private final RabbitMQService rabbitMQService;
    private final UICMessageSender uicMessageSender;
    private final Map<String, Host> hostsByConsumerTags = new ConcurrentHashMap<>();
    private final XmlUtilityService xmlUtilityService;

    @Inject
    public HostQueueConsumer(
            HostStateService hostStateService,
            InboundMessageSender inboundMessageSender,
            RabbitMQService rabbitMQService,
            UICMessageSender uicMessageSender,
            XmlUtilityService xmlUtilityService
    ) {
        this.hostStateService = hostStateService;
        this.inboundMessageSender = inboundMessageSender;
        this.rabbitMQService = rabbitMQService;
        this.uicMessageSender = uicMessageSender;
        this.xmlUtilityService = xmlUtilityService;
    }

    public void startConsuming(Host host) {
        String consumerTag = rabbitMQService.consume(host.getName(), this);
        hostsByConsumerTags.put(consumerTag, host);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        long deliveryTag = envelope.getDeliveryTag();
        String messageType = basicProperties.getType();
        String messageId = basicProperties.getMessageId();
        Host host = hostsByConsumerTags.get(consumerTag);
        MDC.put(MdcKeys.HOST_NAME, host.getName());
        MDC.put(MdcKeys.MESSAGE_ID, messageId);
        Log.info("Handling delivery");

        try {
            DocumentBuilder documentBuilder = xmlUtilityService.createDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            Document document = documentBuilder.parse(input);
            String endpoint = host.getUrl() + host.getMessagingEndpoint();

            switch (messageType) {
                case MessageTypes.UIC_MESSAGE -> uicMessageSender.sendMessage(endpoint, messageId, document);
                case MessageTypes.INBOUND_MESSAGE -> inboundMessageSender.sendMessage(endpoint, document);
            }
            rabbitMQService.ack(deliveryTag);
        } catch (MessageSendException e) {
            handleSendException(deliveryTag, host, e);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleSendException(long deliveryTag, Host host, MessageSendException e) {
        switch (e.getFailureType()) {
            case REQUEST_CREATION_ERROR -> {
                Log.error("Failed to create request. Deleting message from queue", e);
                rabbitMQService.reject(deliveryTag);
            }
            case HOST_UNREACHABLE -> {
                hostStateService.messageDeliveryFailure(host);
                Log.error("Failed to deliver message. Host unreachable.");
                rabbitMQService.nack(deliveryTag);
            }
            case RESPONSE_PROCESSING_ERROR, MESSAGE_REJECTED -> {
                Log.error("Message was delivered but there was a processing issue");
                rabbitMQService.reject(deliveryTag);
            }
        }
    }

    @Override
    public void handleConsumeOk(String consumerTag) {
        Log.infof("Consumer registered successfully with tag: %s", consumerTag);
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        Host host = hostsByConsumerTags.remove(consumerTag);
        if (host != null) {
            Log.infof("Consumer cancelled for host: %s", host.getName());
        }
    }

    @Override
    public void handleCancel(String consumerTag) {
        Host host = hostsByConsumerTags.remove(consumerTag);
        if (host != null) {
            Log.warnf("Consumer externally cancelled for host: ", host.getName());
            // startConsuming(host);
        }
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException e) {
        Log.errorf("Shutdown signal received for consumer: %s", consumerTag);
        Host host = hostsByConsumerTags.get(consumerTag);
        if (host != null) {
            Log.errorf("Removed %s from active consumers", host.getName());
        }
    }

    @Override
    public void handleRecoverOk(String consumerTag) {
        Log.info("Recover ok");
    }
}
