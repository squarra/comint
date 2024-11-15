package org.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.MessageType;
import org.example.host.Host;
import org.example.util.XmlUtilityService;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@ApplicationScoped
public class HostQueueProducer {

    private static final String CHANNEL_ID = "producer";
    private static final String EXCHANGE = "";
    private static final int DELIVERY_MODE_PERSISTENT = 2;

    private final RabbitMQService rabbitMQService;
    private final XmlUtilityService xmlUtilityService;
    private Channel channel;

    public HostQueueProducer(
            RabbitMQService rabbitMQService,
            XmlUtilityService xmlUtilityService
    ) {
        this.rabbitMQService = rabbitMQService;
        this.xmlUtilityService = xmlUtilityService;
    }

    @PostConstruct
    void init() {
        MDC.clear();
        Log.infof("+++++ Initializing %s channel +++++", CHANNEL_ID);
        channel = rabbitMQService.getChannel(CHANNEL_ID);
        Log.infof("Successfully initialized %s channel", CHANNEL_ID);
    }

    public void initializeHostQueue(Host host) {
        try {
            channel.queueDeclare(host.getName(), true, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean sendUICMessage(Host host, String messageIdentifier, Element message) {
        return send(host, messageIdentifier, MessageType.UICMessage, message);
    }

    public boolean sendInboundMessage(Host host, String messageIdentifier, Element message) {
        return send(host, messageIdentifier, MessageType.InboundMessage, message);
    }

    private boolean send(Host host, String messageIdentifier, MessageType messageType, Element message) {
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .deliveryMode(DELIVERY_MODE_PERSISTENT)
                .type(messageType.name())
                .messageId(messageIdentifier)
                .timestamp(new Date())
                .build();

        try {
            Log.info("Publishing message to HostQueue");
            channel.basicPublish(EXCHANGE, host.getName(), basicProperties, elementToBytes(message));
            return true;
        } catch (IOException e) {
            Log.error("Failed to publish message to HostQueue");
            return false;
        } catch (TransformerException e) {
            Log.error("Failed to transform message");
            return false;
        }
    }

    private byte[] elementToBytes(Element element) throws TransformerException {
        Transformer transformer = xmlUtilityService.createTransformer();
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(stringWriter));
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }
}
