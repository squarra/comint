package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class UICMessageHandler {

    private static final Logger LOG = Logger.getLogger(UICMessageHandler.class);
    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    private static final String[] XPATH_MAPPINGS = {
            "//*[local-name()='MessageReference']/*[local-name()='MessageType']",
            "//*[local-name()='MessageReference']/*[local-name()='MessageTypeVersion']",
            "//*[local-name()='MessageReference']/*[local-name()='MessageIdentifier']",
            "//*[local-name()='MessageReference']/*[local-name()='MessageDateTime']",
            "//*[local-name()='MessageHeader']/*[local-name()='Sender']",
            "//*[local-name()='MessageHeader']/*[local-name()='Recipient']"
    };

    private static final String[] FIELD_NAMES = {
            "MessageType",
            "MessageTypeVersion",
            "MessageIdentifier",
            "MessageDateTime",
            "Sender",
            "Recipient"
    };

    public List<String> getMissingKeys(Map<String, String> ackData) {
        List<String> result = new ArrayList<>();
        for (String fieldName : FIELD_NAMES) {
            if (!ackData.containsKey(fieldName)) {
                result.add(fieldName);
            }
        }
        return result;
    }

    public Map<String, String> getAckData(Object message) {
        Map<String, String> result = new HashMap<>();
        if (!(message instanceof Element element)) {
            LOG.error("Message not of type Element");
            return result;
        }
        try {
            for (int i = 0; i < XPATH_MAPPINGS.length; i++) {
                String value = evaluateXPath(element, XPATH_MAPPINGS[i]);
                if (value != null && !value.isEmpty()) {
                    result.put(FIELD_NAMES[i], value);
                }
            }
        } catch (XPathExpressionException e) {
            LOG.error("Error evaluating XPath expression", e);
        }

        return result;
    }

    private String evaluateXPath(Element element, String expression) throws XPathExpressionException {
        try {
            return XPATH.evaluate(expression, element);
        } catch (XPathExpressionException e) {
            LOG.error("Failed to evaluate XPath: " + expression, e);
            return null;
        }
    }
}
