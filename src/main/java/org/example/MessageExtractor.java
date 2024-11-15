package org.example;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.routing.RoutingCriteria;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Extracts message header fields from XML payloads.
 * All methods return null if the field cannot be extracted or is empty.
 * Null-check the return values if working with unvalidated XML documents.
 */
@ApplicationScoped
public class MessageExtractor {

    private static final String MESSAGE_HEADER_RELATIVE = "//*[local-name()='MessageHeader']";
    private static final String MESSAGE_REFERENCE = "/*[local-name()='MessageReference']";
    private static final String MESSAGE_TYPE = "/*[local-name()='MessageType']";
    private static final String MESSAGE_TYPE_VERSION = "/*[local-name()='MessageTypeVersion']";
    private static final String MESSAGE_IDENTIFIER = "/*[local-name()='MessageIdentifier']";
    private static final String MESSAGE_DATE_TIME = "/*[local-name()='MessageDateTime']";
    private static final String SENDER = "/*[local-name()='Sender']";
    private static final String RECIPIENT = "/*[local-name()='Recipient']";

    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    public String extractMessageType(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + MESSAGE_REFERENCE + MESSAGE_TYPE;
        return extractValue(payload, expression, "MessageType");
    }

    public String extractMessageTypeVersion(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + MESSAGE_REFERENCE + MESSAGE_TYPE_VERSION;
        return extractValue(payload, expression, "MessageTypeVersion");
    }

    public String extractMessageIdentifier(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + MESSAGE_REFERENCE + MESSAGE_IDENTIFIER;
        return extractValue(payload, expression, "MessageIdentifier");
    }

    public String extractMessageDateTime(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + MESSAGE_REFERENCE + MESSAGE_DATE_TIME;
        return extractValue(payload, expression, "MessageDateTime");
    }

    public String extractSender(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + SENDER;
        return extractValue(payload, expression, "Sender");
    }

    public String extractRecipient(Object payload) {
        String expression = MESSAGE_HEADER_RELATIVE + RECIPIENT;
        return extractValue(payload, expression, "Recipient");
    }

    public RoutingCriteria extractRoutingCriteria(Object payload) {
        return new RoutingCriteria(extractMessageType(payload), extractMessageTypeVersion(payload), extractRecipient(payload));
    }

    /**
     * Extracts a string value from an XML node using the provided XPath expression.
     *
     * @param payload    The object to extract from (must be instanceof Node)
     * @param expression The XPath expression to evaluate
     * @param fieldName  The name of the field being extracted (for logging)
     * @return The extracted string value, or null if extraction fails
     */
    private String extractValue(Object payload, String expression, String fieldName) {
        if (!(payload instanceof Node node)) {
            Log.warnf("Failed to extract %s. Payload type '%s' is not a valid XML Node", fieldName, payload != null ? payload.getClass().getSimpleName() : "null");
            return null;
        }

        try {
            String value = (String) XPATH.evaluate(expression + "/text()", node, XPathConstants.STRING);
            if (value.isBlank()) {
                Log.debugf("No %s found in XML payload", fieldName);
                return null;
            }
            return value.trim();
        } catch (XPathExpressionException e) {
            Log.warnf("Error extracting %s from XML payload: %", fieldName, e.getMessage());
            return null;
        }
    }
}
