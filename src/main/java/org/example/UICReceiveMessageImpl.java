package org.example;

import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.messaging.ObjectFactory;
import org.example.messaging.UICMessage;
import org.example.messaging.UICMessageResponse;
import org.example.messaging.UICReceiveMessage;
import org.example.messaging.ack.LITechnicalAck;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    @Inject
    MessageValidator messageValidator;
    @Inject
    LITechnicalAckBuilder liTechnicalAckBuilder;

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        Object message = parameters.getMessage();

        if (compressed) {
            throw new RuntimeException("No support for compression yet");
        }
        if (encrypted) {
            throw new RuntimeException("No support for encryption yet");
        }
        if (signed) {
            throw new RuntimeException("No support for signing yet");
        }

        try {
            Node node = messageValidator.validateMessage(message, messageIdentifier);
            LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(node, messageIdentifier);
            return createUICMessageResponse(liTechnicalAck);
        } catch (SchemaValidationException e) {
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
