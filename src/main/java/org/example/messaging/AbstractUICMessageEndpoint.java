package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.jws.WebService;
import org.example.host.Host;
import org.example.logging.MdcKeys;
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
        MDC.put(MdcKeys.MESSAGE_ID, messageIdentifier);
        Log.debug("Received message");

        try {
            Element message = (Element) parameters.getMessage();
            messageValidator.validateMessage(message);
            Element liTechnicalAck = processMessage(messageIdentifier, message);
            return createUICMessageResponse(liTechnicalAck);
        } catch (ClassCastException e) {
            Log.error("Message not an element node");
            throw new RuntimeException(e);
        } catch (MessageValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private Element processMessage(String messageIdentifier, Element message) {
        Host host = routingService.findHost(message);
        if (host == null) return liTechnicalAckBuilder.createNack(messageIdentifier, message);

        boolean success = sendMessage(host.getName(), messageIdentifier, message);
        if (success) {
            return liTechnicalAckBuilder.createAck(messageIdentifier, message);
        } else {
            return liTechnicalAckBuilder.createNack(messageIdentifier, message);
        }
    }

    protected abstract boolean sendMessage(String queue, String messageIdentifier, Element message);

    private UICMessageResponse createUICMessageResponse(Element liTechnicalAck) {
        UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
        uicMessageResponse.setReturn(liTechnicalAck);
        return uicMessageResponse;
    }
}
