package org.example.messaging;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;
import org.example.IntegrationTestResource;
import org.example.MessageBuilder;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(IntegrationTestResource.class)
public class ExternalEndpointTest {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final ExternalEndpoint externalEndpoint;
    private final LITechnicalAckBuilder liTechnicalAckBuilder;

    @Inject
    public ExternalEndpointTest(
            ExternalEndpoint externalEndpoint,
            LITechnicalAckBuilder liTechnicalAckBuilder
    ) {
        this.externalEndpoint = externalEndpoint;
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
    }

    @Test
    void simple() throws JAXBException {
        String messageIdentifier = UUID.randomUUID().toString();
        UICMessage message = createMessage(messageIdentifier);
        UICMessageResponse response = externalEndpoint.uicMessage(message, messageIdentifier, "test", false, false, false);

        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.unmarshal((Node) response.getReturn());
        assertEquals("ACK", liTechnicalAck.getResponseStatus());
    }

    private UICMessage createMessage(String messageIdentifier) {
        UICMessage message = OBJECT_FACTORY.createUICMessage();
        message.setMessage(createPayload(messageIdentifier));
        return message;
    }

    private Element createPayload(String messageIdentifier) {
        return new MessageBuilder("ReceiptConfirmationMessage", "3.5.0.0")
                .messageIdentifier(messageIdentifier)
                .build();
    }
}
