package org.example.rabbitmq;

import com.rabbitmq.client.*;
import io.quarkus.logging.Log;

public class RabbitMqExceptionHandler implements ExceptionHandler {

    @Override
    public void handleUnexpectedConnectionDriverException(Connection connection, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has an exception signalled to it that it can't otherwise deal with.
        Log.error("Unexpected connection driver exception", throwable);
    }

    @Override
    public void handleReturnListenerException(Channel channel, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has called a ReturnListener's handleReturn method, and that method has thrown an exception.
        Log.error("Return Listener Exception", throwable);
    }

    @Override
    public void handleConfirmListenerException(Channel channel, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has called a ConfirmListener's handleAck or handleNack method, and that method has thrown an exception.
        Log.error("Confirm Listener Exception", throwable);
    }

    @Override
    public void handleBlockedListenerException(Connection connection, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has called a BlockedListener's method, and that method has thrown an exception.
        Log.error("Block Listener Exception", throwable);
    }

    @Override
    public void handleConsumerException(Channel channel, Throwable throwable, Consumer consumer, String s, String s1) {
        // Perform any required exception processing for the situation when the driver thread for the connection has called a method on a Consumer, and that method has thrown an exception.
        Log.error("consumer exception", throwable);
    }

    @Override
    public void handleConnectionRecoveryException(Connection connection, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has an exception during connection recovery that it can't otherwise deal with.
        Log.error("Error while trying to recover connection. Trying again...");
    }

    @Override
    public void handleChannelRecoveryException(Channel channel, Throwable throwable) {
        // Perform any required exception processing for the situation when the driver thread for the connection has an exception during channel recovery that it can't otherwise deal with.
        Log.error("Channel Recovery exception", throwable);
    }

    @Override
    public void handleTopologyRecoveryException(Connection connection, Channel channel, TopologyRecoveryException e) {
        // Perform any required exception processing for the situation when the driver thread for the connection has an exception during topology (exchanges, queues, bindings, consumers) recovery that it can't otherwise deal with.
        Log.error("topology recovery exception", e);
    }
}
