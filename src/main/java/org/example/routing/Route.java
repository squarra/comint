package org.example.routing;

import com.opencsv.bean.CsvBindByName;

public class Route {

    @CsvBindByName
    private String source;

    @CsvBindByName
    private String messageType;

    @CsvBindByName
    private String messageTypeVersion;

    @CsvBindByName
    private String recipient;

    @CsvBindByName
    private String destination;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageTypeVersion() {
        return messageTypeVersion;
    }

    public void setMessageTypeVersion(String messageTypeVersion) {
        this.messageTypeVersion = messageTypeVersion;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
