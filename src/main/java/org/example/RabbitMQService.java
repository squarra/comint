package org.example;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMQService {

    private static final String ACCEPT_QUEUE = "accept";
    private static final boolean DURABLE = true;

    public boolean sendMessageToAcceptQueue(Element message, String messageIdentifier, String origin) {
        ConnectionFactory factory = new ConnectionFactory();
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder().contentType("application/json").messageId(messageIdentifier).build();

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            String content = elementToString(message);
            String jsonMessage = createJsonMessage(messageIdentifier, origin, content);
            channel.queueDeclare(ACCEPT_QUEUE, DURABLE, false, false, null);
            channel.basicPublish("", ACCEPT_QUEUE, basicProperties, jsonMessage.getBytes());
            Log.infof("[ %s ] Successfully sent message", messageIdentifier);
            return true;
        } catch (IOException | TransformerException | TimeoutException e) {
            Log.warnf("[ %s ] Failed to send message: %s", messageIdentifier, e.getMessage());
            return false;
        }
    }

    private String createJsonMessage(String identifier, String origin, String content) {
        JsonObject jsonObject = Json.createObjectBuilder().add("identifier", identifier).add("origin", origin).add("content", content).build();

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

    private String elementToString(Element element) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }
}
