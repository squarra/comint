package org.example.validation;

public class MessageValidationException extends RuntimeException {
    public MessageValidationException(String message) {
        super("Message validation failed: " + message);
    }
}
