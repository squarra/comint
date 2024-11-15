package org.example.outbound;

import jakarta.jws.WebService;
import org.example.MessageExtractor;
import org.example.host.Host;
import org.example.logging.MDCKeys;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.HostNotFoundException;
import org.example.routing.RoutingService;
import org.example.validation.MessageValidator;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.example.outbound.OutboundConnectorService")
public class OutboundConnectorServiceImpl implements OutboundConnectorService {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String SUCCESS_MESSAGE = "success";
    private static final String ERROR_MESSAGE = "error";

    private final HostQueueProducer hostQueueProducer;
    private final MessageExtractor messageExtractor;
    private final MessageValidator messageValidator;
    private final RoutingService routingService;

    public OutboundConnectorServiceImpl(
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
        Object message = parameters.getMessage();
        String messageIdentifier = messageExtractor.extractMessageIdentifier(message);
        MDC.put(MDCKeys.MESSAGE_ID, messageIdentifier);

        Element tafTapTsiMessage = messageValidator.validateMessage(message);
        boolean success = processMessage(messageIdentifier, tafTapTsiMessage);

        return createSendOutboundMessageResponse(success);
    }

    private boolean processMessage(String messageIdentifier, Element message) {
        try {
            Host host = routingService.findHost(message);
            return hostQueueProducer.sendUICMessage(host, messageIdentifier, message);
        } catch (HostNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private SendOutboundMessageResponse createSendOutboundMessageResponse(boolean success) {
        SendOutboundMessageResponse response = OBJECT_FACTORY.createSendOutboundMessageResponse();
        response.setResponse(success ? SUCCESS_MESSAGE : ERROR_MESSAGE);
        return response;
    }
}
