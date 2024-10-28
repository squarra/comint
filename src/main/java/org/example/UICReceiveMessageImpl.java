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
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    @Inject
    XmlSchemaProvider xmlSchemaProvider;
    @Inject
    LITechnicalAckBuilder liTechnicalAckBuilder;
    @Inject
    UICMessageHandler uicMessageHandler;

    private static final Logger LOG = Logger.getLogger(UICReceiveMessageImpl.class);
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        Object message = parameters.getMessage();
        Map<String, String> ackData = uicMessageHandler.getAckData(message);

        List<String> missingKeys = uicMessageHandler.getMissingKeys(ackData);
        if (!missingKeys.isEmpty()) {
            LOG.error("missing keys: " + missingKeys);
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        String messageId = ackData.get("MessageIdentifier");
        if (!messageIdentifier.equals(messageId)) {
            LOG.error("message ids dont match");
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        String messageTypeVersion = ackData.get("MessageTypeVersion");
        if (messageTypeVersion == null) {
            LOG.error("no messageTypeVersion");
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        Optional<Schema> schema = xmlSchemaProvider.getSchema(messageTypeVersion);
        if (schema.isEmpty()) {
            LOG.error("no schema found matching the version");
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        Optional<Node> tafTapTsiMessage = getTafTapTsiMessage(message);
        if (tafTapTsiMessage.isEmpty()) {
            LOG.error("taftaptsi message not found");
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        if (!isValidTafTapTsiMessage(tafTapTsiMessage.get(), schema.get())) {
            LOG.error("taftaptsi message not valid");
            return createUICMessageResponse("NACK", ackData, messageIdentifier);
        }

        return createUICMessageResponse("ACK", ackData, messageIdentifier);
    }

    private UICMessageResponse createUICMessageResponse(String responseStatus, Map<String, String> ackData, String messageIdentifier) {
        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(ackData);
        liTechnicalAck.setResponseStatus(responseStatus);
        liTechnicalAck.setAckIndentifier("ACKID" + messageIdentifier);

        UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
        try {
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(liTechnicalAck, document);
            uicMessageResponse.setReturn(document.getDocumentElement());
            return uicMessageResponse;
        } catch (ParserConfigurationException | JAXBException e) {
            LOG.error("error marshalling the ack {}", e);
        }
        return uicMessageResponse;
    }

    private Optional<Node> getTafTapTsiMessage(Object message) {
        if (!(message instanceof Node node)) {
            LOG.error("Schema is not an XML Node");
            return Optional.empty();
        }
        if (!node.getNodeName().equals("message")) {
            LOG.error("Node name is not 'message'");
            return Optional.empty();
        }
        List<Element> elementList = XmlUtils.getElementChildNodes(node);
        if (elementList.size() != 1) {
            LOG.error("not exactly one element in <message>");
            return Optional.empty();
        }
        return Optional.ofNullable(elementList.getFirst());
    }

    private boolean isValidTafTapTsiMessage(Node node, Schema schema) {
        Validator validator = schema.newValidator();
        try {
            validator.validate(new DOMSource(node));
            return true;
        } catch (SAXException | IOException e) {
            LOG.error(e);
            return false;
        }
    }
}
