package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.host.Host;
import org.example.logging.MDCKeys;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.HostNotFoundException;
import org.example.routing.RoutingService;
import org.example.util.XmlUtilityService;
import org.example.validation.MessageValidationException;
import org.example.validation.MessageValidator;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final HostQueueProducer hostQueueProducer;
    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final MessageValidator messageValidator;
    private final RoutingService routingService;
    private final XmlUtilityService xmlUtilityService;

    public UICReceiveMessageImpl(
            HostQueueProducer hostQueueProducer,
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            RoutingService routingService,
            XmlUtilityService xmlUtilityService
    ) {
        this.hostQueueProducer = hostQueueProducer;
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.messageValidator = messageValidator;
        this.routingService = routingService;
        this.xmlUtilityService = xmlUtilityService;
    }

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        MDC.put(MDCKeys.MESSAGE_ID, messageIdentifier);
        Log.debug("Received message");

        Element tafTapTsiMessage = validateOrThrow(parameters.getMessage());
        boolean success = processMessage(messageIdentifier, tafTapTsiMessage);

        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(tafTapTsiMessage, messageIdentifier);
        if (!success) {
            liTechnicalAck.setAckIndentifier("NACK");
        }

        return createUICMessageResponse(liTechnicalAck);
    }

    private Element validateOrThrow(Object message) {
        try {
            Log.info("Validating message");
            return messageValidator.validateMessage(message);
        } catch (MessageValidationException e) {
            Log.errorf("Message validation failed: %s", e.getMessage());
            throw e;
        }
    }

    private boolean processMessage(String messageIdentifier, Element message) {
        try {
            Host host = routingService.findHost(message);

            if (host.isPassthrough()) {
                return hostQueueProducer.sendUICMessage(host, messageIdentifier, message);
            } else {
                return hostQueueProducer.sendInboundMessage(host, messageIdentifier, message);
            }
        } catch (HostNotFoundException e) {
            return false;
        }
    }

    private UICMessageResponse createUICMessageResponse(LITechnicalAck liTechnicalAck) {
        try {
            Document document = xmlUtilityService.createDocument();
            Element wrapper = document.createElement("wrapper");
            document.appendChild(wrapper);

            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(liTechnicalAck, wrapper);

            UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
            uicMessageResponse.setReturn(document.getDocumentElement());
            return uicMessageResponse;
        } catch (JAXBException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
