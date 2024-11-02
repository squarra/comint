package org.example.rabbitmq;

import com.rabbitmq.client.*;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.routing.Host;
import org.example.routing.Route;
import org.example.routing.RoutingService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Startup
@ApplicationScoped
public class AcceptQueueConsumer {

    private static final String ACCEPT_QUEUE = "accept";
    private final RoutingService routingService;
    private final ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    @Inject
    public AcceptQueueConsumer(@ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost") String rabbitMQHost, RoutingService routingService) {
        this.routingService = routingService;
        this.factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
    }

    @PostConstruct
    void init() {
        Log.infof("Attempting to connect to RabbitMQ at: %s", factory.getHost());
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(ACCEPT_QUEUE, true, false, false, null);
            channel.basicQos(1);
            startConsuming();
            Log.info("RabbitMQ connection established successfully");
        } catch (IOException | TimeoutException e) {
            Log.error("Failed to establish RabbitMQ connection: " + e.getMessage());
            Log.debug(e);
        }
    }

    private void startConsuming() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String jsonContent = new String(delivery.getBody(), StandardCharsets.UTF_8);
            AcceptQueueMessage message = AcceptQueueMessage.fromJson(jsonContent);
            String messageId = delivery.getProperties().getMessageId();

            try {
                processMessage(message, messageId);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                Log.infof("[ %s ] Successfully processed message", messageId);
            } catch (Exception e) {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                Log.errorf("[ %s ] Failed to process message: %s", messageId, e.getMessage());
            }
        };

        CancelCallback cancelCallback = consumerTag -> Log.warnf("Consumer %s was cancelled", consumerTag);
        channel.basicConsume(ACCEPT_QUEUE, false, deliverCallback, cancelCallback);
    }

    private void processMessage(AcceptQueueMessage message, String messageId) {
        if (!routingService.isKnownHost(message.origin())) {
            Log.warnf("[ %s ] %s is not a known host", messageId, message.origin());
            return;
        }
        Optional<Route> route = routingService.getRoute(message);
        if (route.isEmpty()) {
            Log.warnf("[ %s ] No route found", messageId);
            return;
        }
        String destination = route.get().getDestination();
        Optional<Host> host = routingService.getHost(destination);
        if (host.isEmpty()) {
            Log.warnf("[ %s ] Destination not registered", messageId);
            return;
        }
        Log.infof("[ %s ] Sending message to %s", messageId, host.get().getUrl());
    }

    @PreDestroy
    void destroy() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            Log.error("Error closing RabbitMQ consumer connections", e);
        }
    }
}
