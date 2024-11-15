package org.example.messaging.ack;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.MessageExtractor;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

@ApplicationScoped
public class LITechnicalAckBuilder {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final MessageExtractor messageExtractor;
    private final XmlUtilityService xmlUtilityService;
    private final String remoteLIName;
    private final int remoteLIInstanceNumber;

    @Inject
    public LITechnicalAckBuilder(
            MessageExtractor messageExtractor,
            XmlUtilityService xmlUtilityService,
            @ConfigProperty(name = "comint.remote-li-name", defaultValue = "LIName") String remoteLIName,
            @ConfigProperty(name = "comint.remote-li-instance-number", defaultValue = "19") int remoteLIInstanceNumber
    ) {
        this.messageExtractor = messageExtractor;
        this.xmlUtilityService = xmlUtilityService;
        this.remoteLIName = remoteLIName;
        this.remoteLIInstanceNumber = remoteLIInstanceNumber;
    }

    public Element build(String messageIdentifier, boolean success, Node message) {
        LITechnicalAck liTechnicalAck = OBJECT_FACTORY.createLITechnicalAck();
        liTechnicalAck.setAckIndentifier("ACKID" + messageIdentifier);
        liTechnicalAck.setResponseStatus(success ? "ACK" : "NACK");
        liTechnicalAck.setMessageReference(createMessageReference(message));
        liTechnicalAck.setSender(messageExtractor.extractSender(message));
        liTechnicalAck.setRecipient(messageExtractor.extractRecipient(message));
        liTechnicalAck.setRemoteLIName(remoteLIName);
        liTechnicalAck.setRemoteLIInstanceNumber(remoteLIInstanceNumber);
        liTechnicalAck.setMessageTransportMechanism("WEBSERVICE");

        try {
            Document document = xmlUtilityService.createDocument();
            Element wrapper = document.createElement("wrapper");
            document.appendChild(wrapper);
            JAXBContext.newInstance(LITechnicalAck.class).createMarshaller().marshal(liTechnicalAck, wrapper);
            return wrapper;
        } catch (ParserConfigurationException | JAXBException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    private MessageReference createMessageReference(Node message) {
        MessageReference messageReference = OBJECT_FACTORY.createMessageReference();
        messageReference.setMessageType(messageExtractor.extractMessageType(message));
        messageReference.setMessageTypeVersion(messageExtractor.extractMessageTypeVersion(message));
        messageReference.setMessageIdentifier(messageExtractor.extractMessageIdentifier(message));
        messageReference.setMessageDateTime(createMessageDateTime(messageExtractor.extractMessageDateTime(message)));
        return messageReference;
    }

    private XMLGregorianCalendar createMessageDateTime(String date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }
}
