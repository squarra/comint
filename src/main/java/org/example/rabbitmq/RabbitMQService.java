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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitMQService {

    private final ConnectionFactory connectionFactory;
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private Connection connection;

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
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void destroy() {
        closeAllChannels();
        closeConnection();
    }

    private void closeAllChannels() {
        channels.forEach((key, channel) -> {
            try {
                if (channel.isOpen()) channel.close();
            } catch (IOException | TimeoutException e) {
                Log.error(e);
            }
        });
        channels.clear();
    }

    private void closeConnection() {
        try {
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel getChannel(String channelId) {
        return channels.computeIfAbsent(channelId, this::createChannel);
    }

    private Channel createChannel(String channelId) {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}