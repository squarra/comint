package org.example.rabbitmq;

import com.rabbitmq.client.*;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Startup
@ApplicationScoped
public class RabbitMQService {

    private static final ConnectionFactory CONNECTION_FACTORY = new ConnectionFactory();
    private static final String DEFAULT_EXCHANGE = "";

    private Connection connection;
    private Channel channel;

    public RabbitMQService(
            @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String host,
            @ConfigProperty(name = "rabbitmq.port", defaultValue = "5672") int port
    ) {
        CONNECTION_FACTORY.setHost(host);
        CONNECTION_FACTORY.setPort(port);
        CONNECTION_FACTORY.setAutomaticRecoveryEnabled(true);
        CONNECTION_FACTORY.setTopologyRecoveryEnabled(true);
        CONNECTION_FACTORY.setNetworkRecoveryInterval(5000);
        CONNECTION_FACTORY.setExceptionHandler(new RabbitMqExceptionHandler());
    }

    @PostConstruct
    void init() {
        establishConnection();
    }

    private void establishConnection() {
        Log.info("***** Establishing connection *****");
        try {
            connection = CONNECTION_FACTORY.newConnection();
            connection.addShutdownListener(this::onConnectionShutdown);
            channel = connection.createChannel();
            channel.addShutdownListener(this::onChannelShutdown);
            addRecoveryListener();
            Log.info("Successfully established RabbitMQ connection and channel");
        } catch (IOException | TimeoutException e) {
            Log.error("Failed to establish RabbitMQ channel. Make sure the broker is running");
            throw new RuntimeException(e);
        }
    }

    private void onConnectionShutdown(ShutdownSignalException cause) {
        if (!cause.isInitiatedByApplication()) {
            Log.error("RabbitMQ connection was unexpectedly shut down");
        } else {
            Log.info("RabbitMQ connection was shut down by the application.");
        }
    }

    private void onChannelShutdown(ShutdownSignalException cause) {
        if (!cause.isInitiatedByApplication()) {
            Log.error("RabbitMQ connection was unexpectedly shut down");
        } else {
            Log.info("RabbitMQ connection was shut down by the application.");
        }
    }

    private void addRecoveryListener() {
        ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                Log.info("RabbitMQ connection has been recovered.");
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
                Log.info("RabbitMQ connection recovery has started.");
            }
        });
    }

    public void publish(String hostName, String messageId, String messageType, byte[] message) {
        if (isChannelDead()) {
            Log.error("RabbitMQ connection is not ready for publishing messages.");
            return;
        }

        declareQueueIfAbsent(hostName);

        try {
            AMQP.BasicProperties properties = MessageProperties.MINIMAL_PERSISTENT_BASIC.builder()
                    .type(messageType)
                    .messageId(messageId)
                    .build();
            channel.basicPublish(DEFAULT_EXCHANGE, hostName, true, properties, message);
            Log.debugf("Successfully published message to queue: %s", hostName);
        } catch (Exception e) {
            Log.errorf("Failed to publish message to queue: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String consume(String hostName, Consumer callback) {
        if (isChannelDead()) {
            Log.error("RabbitMQ channel ist not ready to add consumers");
            throw new RuntimeException();
        }

        declareQueueIfAbsent(hostName);

        try {
            return channel.basicConsume(hostName, callback);
        } catch (IOException e) {
            Log.errorf("Failed to start consumer for queue: %s", hostName);
            throw new RuntimeException(e);
        }
    }

    public void ack(long deliveryTag) {
        boolean multiple = false;
        try {
            channel.basicAck(deliveryTag, multiple);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void nack(long deliveryTag) {
        boolean multiple = false;
        boolean requeue = false;
        try {
            channel.basicNack(deliveryTag, multiple, requeue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reject(long deliveryTag) {
        boolean requeue = false;
        try {
            channel.basicReject(deliveryTag, requeue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void declareQueueIfAbsent(String queueName) {
        if (isChannelDead()) {
            Log.error("RabbitMQ channel is not ready to declare queues");
            return;
        }

        try {
            channel.queueDeclare(queueName, true, false, false, null);
        } catch (IOException e) {
            Log.errorf("Failed to declare queue: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean isChannelDead() {
        return channel == null || !channel.isOpen();
    }

    @PreDestroy
    void destroy() {
        closeChannel();
        closeConnection();
    }

    private void closeChannel() {
        if (isChannelDead()) return;
        try {
            channel.close();
        } catch (Exception e) {
            Log.errorf("Error closing channel: %s", e.getMessage());
        }
    }

    private void closeConnection() {
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
            } catch (Exception e) {
                Log.errorf("Error closing connection: %s", e.getMessage());
            }
        }
    }
}
