package org.example.outbound;

import io.quarkus.logging.Log;
import jakarta.jws.WebService;
import org.example.MessageExtractor;
import org.example.host.Host;
import org.example.logging.MdcKeys;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.RoutingService;
import org.example.validation.MessageValidator;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.example.outbound.OutboundConnectorService")
public class OutboundEndpoint implements OutboundConnectorService {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String SUCCESS_MESSAGE = "success";
    private static final String ERROR_MESSAGE = "error";

    private final HostQueueProducer hostQueueProducer;
    private final MessageExtractor messageExtractor;
    private final MessageValidator messageValidator;
    private final RoutingService routingService;

    public OutboundEndpoint(
            HostQueueProducer hostQueueProducer,
            MessageExtractor messageExtractor,
            MessageValidator messageValidator,
            RoutingService routingService
    ) {
        this.hostQueueProducer = hostQueueProducer;
        this.messageExtractor = messageExtractor;
        this.messageValidator = messageValidator;
        this.routingService = routingService;
    }

    @Override
    public SendOutboundMessageResponse sendOutboundMessage(SendOutboundMessage parameters, String encoded) {
        try {
            Element message = (Element) parameters.getMessage();
            String messageIdentifier = messageExtractor.extractMessageIdentifier(message);
            MDC.put(MdcKeys.MESSAGE_ID, messageIdentifier);
            Log.debug("Received message");

            messageValidator.validateMessage(message);
            String responseMessage = processMessage(messageIdentifier, message);
            return createSendOutboundMessageResponse(responseMessage);
        } catch (Exception e) {
            Log.error(e);
            return createSendOutboundMessageResponse(ERROR_MESSAGE);
        }
    }

    private String processMessage(String messageIdentifier, Element message) {
        Host host = routingService.findHost(message);
        if (host == null) return ERROR_MESSAGE;

        boolean success = hostQueueProducer.sendUICMessage(host.getName(), messageIdentifier, message);
        return success ? SUCCESS_MESSAGE : ERROR_MESSAGE;
    }

    private SendOutboundMessageResponse createSendOutboundMessageResponse(String responseMessage) {
        SendOutboundMessageResponse response = OBJECT_FACTORY.createSendOutboundMessageResponse();
        response.setResponse(responseMessage);
        return response;
    }
}
