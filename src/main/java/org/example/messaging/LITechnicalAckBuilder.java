package org.example.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.MessageReference;
import org.example.messaging.ack.ObjectFactory;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

@ApplicationScoped
public class LITechnicalAckBuilder {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final MessageExtractor messageExtractor;
    private final String remoteLIName;
    private final int remoteLIInstanceNumber;

    @Inject
    public LITechnicalAckBuilder(
            MessageExtractor messageExtractor,
            @ConfigProperty(name = "comint.remote-li-name", defaultValue = "LIName") String remoteLIName,
            @ConfigProperty(name = "comint.remote-li-instance-number", defaultValue = "19") int remoteLIInstanceNumber
    ) {
        this.messageExtractor = messageExtractor;
        this.remoteLIName = remoteLIName;
        this.remoteLIInstanceNumber = remoteLIInstanceNumber;
    }

    public LITechnicalAck build(Node message, String messageIdentifier) {
        LITechnicalAck liTechnicalAck = OBJECT_FACTORY.createLITechnicalAck();
        liTechnicalAck.setAckIndentifier("ACKID" + messageIdentifier);
        liTechnicalAck.setResponseStatus("ACK");
        liTechnicalAck.setMessageReference(createMessageReference(message));
        liTechnicalAck.setSender(messageExtractor.extractSender(message));
        liTechnicalAck.setRecipient(messageExtractor.extractRecipient(message));
        liTechnicalAck.setRemoteLIName(remoteLIName);
        liTechnicalAck.setRemoteLIInstanceNumber(remoteLIInstanceNumber);
        liTechnicalAck.setMessageTransportMechanism("WEBSERVICE");
        return liTechnicalAck;
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
            throw new RuntimeException(e);
        }
    }
}
