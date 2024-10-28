package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.MessageReference;
import org.example.messaging.ack.ObjectFactory;
import org.jboss.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Map;

@ApplicationScoped
public class LITechnicalAckBuilder {

    private static final Logger LOG = Logger.getLogger(LITechnicalAckBuilder.class);
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    public LITechnicalAck build(Map<String, String> ackData) {
        LITechnicalAck liTechnicalAck = OBJECT_FACTORY.createLITechnicalAck();
        MessageReference messageReference = OBJECT_FACTORY.createMessageReference();
        messageReference.setMessageType(ackData.get("MessageType"));
        messageReference.setMessageTypeVersion(ackData.get("MessageTypeVersion"));
        messageReference.setMessageIdentifier(ackData.get("MessageIdentifier"));
        messageReference.setMessageDateTime(createMessageDateTime(ackData.get("MessageDateTime")));
        liTechnicalAck.setMessageReference(messageReference);
        liTechnicalAck.setSender(stringToInt(ackData.get("Sender")));
        liTechnicalAck.setRecipient(stringToInt(ackData.get("Recipient")));
        liTechnicalAck.setRemoteLIName("LIName");
        liTechnicalAck.setRemoteLIInstanceNumber(19);
        liTechnicalAck.setMessageTransportMechanism("WEBSERVICE");
        return liTechnicalAck;
    }

    private int stringToInt(String value) {
        // helper method for null check before Integer.parseInt()
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            LOG.error("Could not parse " + value + " to int");
            return -1;
        }
    }

    private XMLGregorianCalendar createMessageDateTime(String date) {
        if (date == null) {
            return null;
        }
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
}
