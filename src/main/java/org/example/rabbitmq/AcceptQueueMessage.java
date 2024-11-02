package org.example.rabbitmq;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;

public record AcceptQueueMessage(
        String sender,
        String messageType,
        String messageTypeVersion,
        String recipient,
        String content
) {
    public static AcceptQueueMessage fromJson(String json) {
        JsonObject jsonObject = Json.createReader(new StringReader(json)).readObject();
        return new AcceptQueueMessage(
                jsonObject.getString("sender"),
                jsonObject.getString("messageType"),
                jsonObject.getString("messageTypeVersion"),
                jsonObject.getString("recipient"),
                jsonObject.getString("content")
        );
    }

    public String toJson() {
        return Json.createObjectBuilder()
                .add("sender", sender)
                .add("messageType", messageType)
                .add("messageTypeVersion", messageTypeVersion)
                .add("recipient", recipient)
                .add("content", content)
                .build()
                .toString();
    }
}
