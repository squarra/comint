package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.messaging.ack.LITechnicalAck;
import org.example.rabbitmq.AcceptQueueProducer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final MessageValidator messageValidator;
    private final AcceptQueueProducer acceptQueueProducer;

    @Inject
    public UICReceiveMessageImpl(
            LITechnicalAckBuilder liTechnicalAckBuilder,
            MessageValidator messageValidator,
            AcceptQueueProducer acceptQueueProducer
    ) {
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.messageValidator = messageValidator;
        this.acceptQueueProducer = acceptQueueProducer;
    }

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        Element message = validateMessageOrThrow(parameters.getMessage(), messageIdentifier);
        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(message, messageIdentifier);
        if (!acceptQueueProducer.send(message, messageIdentifier)) {
            liTechnicalAck.setResponseStatus("NACK");
        }
        return createUICMessageResponse(liTechnicalAck);
    }

    private Element validateMessageOrThrow(Object message, String messageIdentifier) {
        try {
            return messageValidator.validateMessage(message, messageIdentifier);
        } catch (SchemaValidationException e) {
            Log.warnf("[ %s ] Schema validation failed: %s", messageIdentifier, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
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
