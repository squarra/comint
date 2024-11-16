package org.example.messaging;

import jakarta.jws.WebService;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.RoutingService;
import org.example.validation.MessageValidator;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICMessageEndpoint extends AbstractUICMessageEndpoint implements UICReceiveMessage {

    private final HostQueueProducer hostQueueProducer;

    public UICMessageEndpoint(
            HostQueueProducer hostQueueProducer,
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            RoutingService routingService
    ) {
        super(liTechnicalAckBuilder, messageValidator, routingService);
        this.hostQueueProducer = hostQueueProducer;
    }

    @Override
    protected boolean sendMessage(String queue, String messageIdentifier, Element message) {
        return hostQueueProducer.sendInboundMessage(queue, messageIdentifier, message);
    }
}
