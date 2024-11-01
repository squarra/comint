package org.example;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    private final ConnectionFactory factory;
    private final TransformerFactory transformerFactory;
    private Connection connection;

    @Inject
    public RabbitMQService(@ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String rabbitMQHost) {
        this.factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        this.transformerFactory = TransformerFactory.newInstance();
        Log.info("Attempting to connect to RabbitMQ at: " + rabbitMQHost);
    }

    void onStart(@Observes StartupEvent event) {
        try {
            connection = factory.newConnection();
            try (Channel channel = connection.createChannel()) {
                channel.queueDeclare(ACCEPT_QUEUE, DURABLE, false, false, null);
            }
            Log.info("RabbitMQ connection established successfully");
        } catch (IOException | TimeoutException e) {
            Log.error("Failed to establish RabbitMQ connection", e);
            throw new RuntimeException(e);
        }
    }

    void onShutDown(@Observes ShutdownEvent event) {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            Log.error("Error closing RabbitMQ connection", e);
        }
    }

    public boolean sendMessageToAcceptQueue(Element message, String messageIdentifier, String origin) {
        if (connection == null || !connection.isOpen()) {
            Log.error("No valid RabbitMQ connection available");
            return false;
        }

        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .messageId(messageIdentifier)
                .build();

        try (Channel channel = connection.createChannel()) {
            String content = elementToString(message);
            String jsonMessage = createJsonMessage(messageIdentifier, origin, content);
            channel.basicPublish("", ACCEPT_QUEUE, basicProperties, jsonMessage.getBytes());
            Log.infof("[ %s ] Successfully sent message", messageIdentifier);
            return true;
        } catch (IOException | TransformerException | TimeoutException e) {
            Log.warnf("[ %s ] Failed to send message: %s", messageIdentifier, e.getMessage());
            return false;
        }
    }

    private String createJsonMessage(String identifier, String origin, String content) {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("identifier", identifier)
                .add("origin", origin)
                .add("content", content)
                .build();

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

    private String elementToString(Element element) throws TransformerException {
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }
}
