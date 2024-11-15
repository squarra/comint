package org.example.messaging;

import jakarta.jws.WebService;
import org.example.host.Host;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.RoutingService;
import org.example.validation.MessageValidator;
import org.w3c.dom.Element;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class PassthroughEndpoint extends AbstractUICMessageEndpoint implements UICReceiveMessage {

    private final HostQueueProducer hostQueueProducer;

    public PassthroughEndpoint(
            HostQueueProducer hostQueueProducer,
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            RoutingService routingService
    ) {
        super(liTechnicalAckBuilder, messageValidator, routingService);
        this.hostQueueProducer = hostQueueProducer;
    }

    @Override
    protected boolean sendMessage(Host host, String messageIdentifier, Element message) {
        return hostQueueProducer.sendUICMessage(host, messageIdentifier, message);
    }
}
