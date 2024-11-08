package org.example.messaging;

import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.host.Host;
import org.example.host.HostService;
import org.example.log.MessageLogger;
import org.example.messaging.ack.LITechnicalAck;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.RoutingService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Optional;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final MessageLogger messageLogger;
    private final MessageValidator messageValidator;
    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final RoutingService routingService;
    private final HostService hostService;
    private final HostQueueProducer hostQueueProducer;

    @Inject
    public UICReceiveMessageImpl(
            MessageLogger messageLogger,
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            RoutingService routingService,
            HostService hostService,
            HostQueueProducer hostQueueProducer) {
        this.messageLogger = messageLogger;
        this.messageValidator = messageValidator;
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.routingService = routingService;
        this.hostService = hostService;
        this.hostQueueProducer = hostQueueProducer;
    }

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        messageLogger.info(messageIdentifier, "Received message");
        Element message = validateMessageOrThrow(parameters.getMessage(), messageIdentifier);
        messageLogger.info(messageIdentifier, "Successfully validated message");

        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(message, messageIdentifier);
        boolean success = processMessage(messageIdentifier, message);
        if (!success) {
            liTechnicalAck.setAckIndentifier("NACK");
        }
        return createUICMessageResponse(liTechnicalAck);
    }

    private Element validateMessageOrThrow(Object message, String messageIdentifier) {
        try {
            return messageValidator.validateMessage(message, messageIdentifier);
        } catch (SchemaValidationException e) {
            messageLogger.warn(messageIdentifier, "Schema validation failed: %s", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean processMessage(String messageIdentifier, Element message) {
        Optional<String> destination = routingService.getDestination(message);
        if (destination.isEmpty()) {
            messageLogger.warn(messageIdentifier, "Failed to find destination in routes");
            return false;
        }
        Optional<Host> host = hostService.getHost(destination.get());
        if (host.isEmpty()) {
            messageLogger.warn(messageIdentifier, "Failed to find '%s' in hosts", destination);
            return false;
        }
        return hostQueueProducer.send(host.get(), messageIdentifier, message);
    }

    private UICMessageResponse createUICMessageResponse(LITechnicalAck liTechnicalAck) {
        try {
            UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(liTechnicalAck, document);
            uicMessageResponse.setReturn(document.getDocumentElement());
            return uicMessageResponse;
        } catch (ParserConfigurationException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
