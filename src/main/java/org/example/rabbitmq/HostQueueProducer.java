package org.example.rabbitmq;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.MessageTypes;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

@ApplicationScoped
public class HostQueueProducer {

    private final RabbitMQService rabbitMQService;
    private final XmlUtilityService xmlUtilityService;

    public HostQueueProducer(
            RabbitMQService rabbitMQService,
            XmlUtilityService xmlUtilityService
    ) {
        this.rabbitMQService = rabbitMQService;
        this.xmlUtilityService = xmlUtilityService;
    }

    public boolean sendUICMessage(String queue, String messageIdentifier, Element message) {
        return send(queue, messageIdentifier, MessageTypes.UIC_MESSAGE, message);
    }

    public boolean sendInboundMessage(String queue, String messageIdentifier, Element message) {
        return send(queue, messageIdentifier, MessageTypes.INBOUND_MESSAGE, message);
    }

    private boolean send(String queue, String messageIdentifier, String messageType, Element message) {
        try {
            rabbitMQService.publish(queue, messageIdentifier, messageType, elementToBytes(message));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] elementToBytes(Element element) {
        try {
            Transformer transformer = xmlUtilityService.createTransformer();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(element), new StreamResult(outputStream));
            return outputStream.toByteArray();
        } catch (TransformerException e) {
            Log.errorf("Failed to transform message: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
