package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.jws.WebService;
import org.example.host.Host;
import org.example.logging.MDCKeys;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.routing.RoutingService;
import org.example.validation.MessageValidationException;
import org.example.validation.MessageValidator;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public abstract class AbstractUICMessageEndpoint implements UICReceiveMessage {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final MessageValidator messageValidator;
    private final RoutingService routingService;

    public AbstractUICMessageEndpoint(
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            RoutingService routingService
    ) {
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.messageValidator = messageValidator;
        this.routingService = routingService;
    }

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        MDC.put(MDCKeys.MESSAGE_ID, messageIdentifier);
        Log.debug("Received message");
        Element tafTapTsiMessage = validateOrThrow(parameters.getMessage());
        boolean success = processMessage(messageIdentifier, tafTapTsiMessage);
        Element liTechnicalAck = liTechnicalAckBuilder.build(messageIdentifier, success, tafTapTsiMessage);
        return createUICMessageResponse(liTechnicalAck);
    }

    private Element validateOrThrow(Object message) {
        try {
            return messageValidator.validateMessage(message);
        } catch (MessageValidationException e) {
            Log.errorf("Failed to validate message: %s", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean processMessage(String messageIdentifier, Element message) {
        Host host = routingService.findHost(message);
        if (host == null) return false;
        return sendMessage(host.getName(), messageIdentifier, message);
    }

    protected abstract boolean sendMessage(String queue, String messageIdentifier, Element message);

    private UICMessageResponse createUICMessageResponse(Element liTechnicalAck) {
        UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
        uicMessageResponse.setReturn(liTechnicalAck);
        return uicMessageResponse;
    }
}
