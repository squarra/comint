package org.example;

import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.messaging.UICMessage;
import org.example.messaging.UICMessageResponse;
import org.example.messaging.UICReceiveMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        return createUICMessageResponse(messageIdentifier);
    }

    private UICMessageResponse createUICMessageResponse(String messageId) {
        LITechnicalAck liTechnicalAck = createTechnicalAck(messageId);
        try {
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Node node = document.createElement("root");

            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(liTechnicalAck, node);

            UICMessageResponse result = new UICMessageResponse();
            result.setReturn(node);
            return result;
        } catch (JAXBException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private LITechnicalAck createTechnicalAck(String messageId) {
        LITechnicalAck result = new LITechnicalAck();
        result.setResponseStatus("ACK");
        result.setAckIndentifier("ACKID" + messageId);
        result.setMessageReference(createMessageReference());
        result.setSender(0);
        result.setRecipient(0);
        result.setRemoteLIName("LIName");
        result.setRemoteLIInstanceNumber(19);
        result.setMessageTransportMechanism("WEBSERVICE");
        return result;
    }

    private MessageReference createMessageReference() {
        return new MessageReference();
    }
}
