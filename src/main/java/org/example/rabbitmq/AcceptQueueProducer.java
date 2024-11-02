package org.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.messaging.MessageHeaderExtractor;
import org.example.messaging.RoutingCriteria;
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
public class AcceptQueueProducer {

    private static final String ACCEPT_QUEUE = "accept";

    private final MessageHeaderExtractor messageHeaderExtractor;
    private final ConnectionFactory factory;
    private final TransformerFactory transformerFactory;
    private Connection connection;

    @Inject
    public AcceptQueueProducer(
            @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String rabbitMQHost,
            MessageHeaderExtractor messageHeaderExtractor) {
        this.messageHeaderExtractor = messageHeaderExtractor;
        this.factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        this.transformerFactory = TransformerFactory.newInstance();
        Log.infof("Attempting to connect to RabbitMQ at: %s", rabbitMQHost);
    }

    @PostConstruct
    void init() {
        try {
            connection = factory.newConnection();
            try (Channel channel = connection.createChannel()) {
                channel.queueDeclare(ACCEPT_QUEUE, true, false, false, null);
            }
            Log.info("RabbitMQ connection established successfully");
        } catch (IOException | TimeoutException e) {
            Log.error("Failed to establish RabbitMQ connection", e);
            throw new RuntimeException(e);
        }
    }

    public boolean send(Element message, String messageIdentifier) {
        if (!isConnectionValid()) {
            Log.error("No valid RabbitMQ connection available");
            return false;
        }

        try (Channel channel = connection.createChannel()) {
            RoutingCriteria routingCriteria = messageHeaderExtractor.extractRoutingCriteria(message);
            String content = elementToString(message);

            AcceptQueueMessage queueMessage = new AcceptQueueMessage(
                    routingCriteria.sender(),
                    routingCriteria.messageType(),
                    routingCriteria.messageTypeVersion(),
                    routingCriteria.recipient(),
                    content
            );

            AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .messageId(messageIdentifier)
                    .build();

            channel.basicPublish("", ACCEPT_QUEUE, basicProperties, queueMessage.toJson().getBytes());
            Log.infof("[ %s ] Successfully sent message", messageIdentifier);
            return true;
        } catch (Exception e) {
            Log.warnf("[ %s ] Failed to send message: %s", messageIdentifier, e.getMessage());
            return false;
        }
    }

    private boolean isConnectionValid() {
        return connection != null && connection.isOpen();
    }

    private String createJsonMessage(RoutingCriteria routingCriteria, String content) {
        return Json.createObjectBuilder()
                .add("sender", routingCriteria.sender())
                .add("messageType", routingCriteria.messageType())
                .add("messageTypeVersion", routingCriteria.messageTypeVersion())
                .add("recipient", routingCriteria.recipient())
                .add("content", content)
                .build()
                .toString();
    }

    private String elementToString(Element element) throws TransformerException {
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }

    @PreDestroy
    void destroy() {
        if (isConnectionValid()) {
            try {
                connection.close();
            } catch (IOException e) {
                Log.error("Error closing RabbitMQ connection", e);
            }
        }
    }
}
