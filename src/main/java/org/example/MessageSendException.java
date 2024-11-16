package org.example;

public class MessageSendException extends Exception {
    private final FailureType failureType;

    public MessageSendException(FailureType failureType, Throwable cause) {
        super(failureType.name(), cause);
        this.failureType = failureType;
    }

    public MessageSendException(FailureType failureType) {
        super(failureType.name());
        this.failureType = failureType;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public enum FailureType {
        REQUEST_CREATION_ERROR,
        HOST_UNREACHABLE,
        MESSAGE_REJECTED,
        RESPONSE_PROCESSING_ERROR
    }
}
