package org.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.MDC;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMQService {

    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    public RabbitMQService(
            @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String host
    ) {
        this.connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
    }

    @PostConstruct
    void init() {
        MDC.clear();
        Log.info("+++++ Initializing connection +++++");
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            Log.infof("Successfully initialized connection");
        } catch (IOException | TimeoutException e) {
            Log.error("Error while establishing connection to rabbitmq", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void destroy() {
        closeChannel();
        closeConnection();
    }

    private void closeChannel() {
        try {
            if (channel.isOpen()) channel.close();
        } catch (TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeConnection() {
        try {
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel getChannel() {
        return channel;
    }
}
