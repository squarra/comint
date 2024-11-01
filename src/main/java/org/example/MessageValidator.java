package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MessageValidator {

    @Inject
    MessageHeaderExtractor messageHeaderExtractor;
    @Inject
    XmlSchemaProvider xmlSchemaProvider;

    public Element validateMessage(Object message, String messageIdentifier) throws SchemaValidationException {

        if (!(message instanceof Node node)) {
            throw new SchemaValidationException("The message is not an XML node");
        }

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SchemaValidationException("The message is not an element node but of type " + node.getNodeType());
        }

        Element element = (Element) node;
        if (!element.getTagName().equalsIgnoreCase("message")) {
            throw new SchemaValidationException("The message is not an element node with name \"message\"");
        }

        List<Element> elementChildNodes = XmlUtils.getElementChildNodes(element);
        if (elementChildNodes.size() != 1) {
            throw new SchemaValidationException(
                    "The number of XML element child nodes of the message was " + elementChildNodes.size() + " (1 expected)");
        }

        Element tafTapTsiMessage = elementChildNodes.getFirst();
        String tafTapTsiMessageIdentifier = messageHeaderExtractor.extractMessageIdentifier(tafTapTsiMessage);
        if (tafTapTsiMessageIdentifier == null) {
            throw new SchemaValidationException("MessageIdentifier in TafTapTsi MessageHeader is missing");
        }

        if (!tafTapTsiMessageIdentifier.equals(messageIdentifier)) {
            throw new SchemaValidationException("MessageIdentifier in Soap Header (" + messageIdentifier
                    + ") and TafTapTsi MessageHeader (" + tafTapTsiMessageIdentifier + ") do not match");
        }

        String messageTypeVersion = messageHeaderExtractor.extractMessageTypeVersion(tafTapTsiMessage);
        if (messageTypeVersion == null) {
            throw new SchemaValidationException("MessageTypeVersion in TafTapTsi MessageHeader is missing");
        }
        Optional<Schema> schema = xmlSchemaProvider.getSchema(messageTypeVersion);
        if (schema.isEmpty()) {
            throw new SchemaValidationException("No schema found for version " + messageTypeVersion);
        }

        try {
            schema.get().newValidator().validate(new DOMSource(tafTapTsiMessage));
        } catch (IOException | SAXException e) {
            throw new SchemaValidationException(e.getMessage());
        }

        return tafTapTsiMessage;
    }
}