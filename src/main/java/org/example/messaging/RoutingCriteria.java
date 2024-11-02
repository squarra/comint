package org.example.messaging;

public record RoutingCriteria(String sender, String messageType, String messageTypeVersion, String recipient) {
}
