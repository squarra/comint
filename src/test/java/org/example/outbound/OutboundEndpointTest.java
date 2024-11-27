package org.example.outbound;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.example.IntegrationTestResource;
import org.example.MessageBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(IntegrationTestResource.class)
public class OutboundEndpointTest {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Inject
    OutboundEndpoint outboundEndpoint;

    @Test
    void simple() {
        SendOutboundMessage message = createMessage();
        SendOutboundMessageResponse response = outboundEndpoint.sendOutboundMessage(message, "false");
        assertEquals("success", response.getResponse());
    }

    private SendOutboundMessage createMessage() {
        SendOutboundMessage message = OBJECT_FACTORY.createSendOutboundMessage();
        message.setMessage(createPayload());
        return message;
    }

    private Element createPayload() {
        return new MessageBuilder("ReceiptConfirmationMessage", "3.5.0.0").build();
    }
}
