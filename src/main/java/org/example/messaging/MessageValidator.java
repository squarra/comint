package org.example.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.MessageKey;
import org.example.Messages;
import org.example.XmlSchemaService;
import org.example.util.XmlUtils;
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

    private final MessageExtractor messageExtractor;
    private final Messages messages;
    private final XmlSchemaService xmlSchemaService;

    @Inject
    public MessageValidator(MessageExtractor messageExtractor, Messages messages, XmlSchemaService xmlSchemaService) {
        this.messageExtractor = messageExtractor;
        this.messages = messages;
        this.xmlSchemaService = xmlSchemaService;
    }

    public Element validateMessage(Object message, String messageIdentifier) throws SchemaValidationException {

        if (!(message instanceof Node node)) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_NOT_NODE));
        }

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_NOT_ELEMENT, node.getNodeType()));
        }

        Element element = (Element) node;
        if (!element.getTagName().equalsIgnoreCase("message")) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_WRONG_ROOT));
        }

        List<Element> elementChildNodes = XmlUtils.getElementChildNodes(element);
        if (elementChildNodes.size() != 1) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_MULTIPLE_CHILDREN, elementChildNodes.size()));
        }

        Element tafTapTsiMessage = elementChildNodes.getFirst();
        String tafTapTsiMessageIdentifier = messageExtractor.extractMessageIdentifier(tafTapTsiMessage);
        if (tafTapTsiMessageIdentifier == null) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_IDENTIFIER_MISSING));
        }

        if (!tafTapTsiMessageIdentifier.equals(messageIdentifier)) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_IDENTIFIER_MISMATCH, messageIdentifier, tafTapTsiMessageIdentifier));
        }

        String messageTypeVersion = messageExtractor.extractMessageTypeVersion(tafTapTsiMessage);
        if (messageTypeVersion == null) {
            throw new SchemaValidationException(messages.get(MessageKey.MESSAGE_TYPE_VERSION_MISSING));
        }
        Optional<Schema> schema = xmlSchemaService.getSchema(messageTypeVersion);
        if (schema.isEmpty()) {
            throw new SchemaValidationException(messages.get(MessageKey.SCHEMA_NOT_FOUND, messageTypeVersion));
        }

        try {
            schema.get().newValidator().validate(new DOMSource(tafTapTsiMessage));
        } catch (IOException | SAXException e) {
            throw new SchemaValidationException(e.getMessage());
        }

        return tafTapTsiMessage;
    }
}
