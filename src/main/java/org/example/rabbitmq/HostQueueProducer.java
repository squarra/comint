package org.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.host.Host;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class HostQueueProducer {

    private final ConnectionFactory connectionFactory;
    private final TransformerFactory transformerFactory;
    private final Set<Host> hostQueues;
    private Connection connection;
    private Channel channel;

    @Inject
    public HostQueueProducer(@ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String rabbitMQHost) {
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(rabbitMQHost);
        this.transformerFactory = TransformerFactory.newInstance();
        this.hostQueues = new HashSet<>();
    }

    @PostConstruct
    void init() {
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeHostQueue(Host host) {
        try {
            channel.queueDeclare(host.getName(), true, false, false, null);
            hostQueues.add(host);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean send(Host host, String messageIdentifier, Element payload) {
        MDC.put("Host", host.getName());
        MDC.put("MessageId", messageIdentifier);
        if (!hostQueues.contains(host)) {
            Log.error("Failed to find HostQueue");
            return false;
        }

        try {
            String payloadString = transformPayload(payload);
            AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                    .contentType("application/xml")
                    .messageId(messageIdentifier)
                    .build();
            channel.basicPublish("", host.getName(), basicProperties, payloadString.getBytes(StandardCharsets.UTF_8));
            Log.info("Published message to HostQueue");
        } catch (TransformerException | IOException e) {
            Log.errorf("Failed to publish message to HostQueue: %s", e.getMessage());
            return false;
        }
        return true;
    }

    private String transformPayload(Element payload) throws TransformerException {
        StringWriter stringWriter = new StringWriter();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(new DOMSource(payload), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    void onStop(@Observes ShutdownEvent ev) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            Log.info("RabbitMQ resources closed successfully");
        } catch (IOException | TimeoutException e) {
            Log.error("Error closing RabbitMQ resources", e);
        }
    }
}
