package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class MessageBuilder {

    private final Document document;
    private final String version;
    private final String namespace;
    private Element rootElement;
    private Element currentElement;
    private Element messageType;
    private Element messageTypeVersion;
    private Element messageIdentifier;
    private Element messageDateTime;
    private Element sender;
    private Element recipient;


    public MessageBuilder(String root, String version) {
        this.version = version;
        this.namespace = "http://www.era.europa.eu/schemes/TAFTSI/" + version.substring(0, 3);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        rootElement = document.createElementNS(namespace, root);
        document.appendChild(rootElement);
        currentElement = rootElement;
        addDefaultMessageHeader();
    }

    public MessageBuilder element(String name) {
        Element element = createElement(name);
        currentElement.appendChild(element);
        currentElement = element;
        return this;
    }

    public MessageBuilder text(String name, String value) {
        currentElement.appendChild(createTextElement(name, value));
        return this;
    }

    public MessageBuilder attribute(String name, String value) {
        currentElement.setAttributeNS(namespace, name, value);
        return this;
    }

    public MessageBuilder messageType(String value) {
        return setElementText(messageType, value);
    }

    public MessageBuilder messageTypeVersion(String value) {
        return setElementText(messageTypeVersion, value);
    }

    public MessageBuilder messageIdentifier(String value) {
        return setElementText(messageIdentifier, value);
    }

    public MessageBuilder messageDateTime(String value) {
        return setElementText(messageDateTime, value);
    }

    public MessageBuilder sender(String value) {
        return setElementText(sender, value);
    }

    public MessageBuilder recipient(String value) {
        return setElementText(recipient, value);
    }

    public MessageBuilder up() {
        Node parent = currentElement.getParentNode();
        if (parent instanceof Element) {
            currentElement = (Element) parent;
        }
        return this;
    }

    public MessageBuilder navigateToElement(String name) {
        NodeList nodeList = document.getElementsByTagNameNS(namespace, name);
        if (nodeList.getLength() > 0) {
            currentElement = (Element) nodeList.item(0);
        }
        return this;
    }

    public MessageBuilder remove() {
        if (currentElement != rootElement) {
            Node parent = currentElement.getParentNode();
            parent.removeChild(currentElement);
            currentElement = (Element) parent;
        }
        return this;
    }

    public MessageBuilder rename(String newName) {
        Element newElement = document.createElementNS(namespace, newName);
        while (currentElement.hasChildNodes()) {
            newElement.appendChild(currentElement.getFirstChild());
        }
        Node parent = currentElement.getParentNode();
        parent.replaceChild(newElement, currentElement);
        currentElement = newElement;
        return this;
    }

    public MessageBuilder wrap(String name) {
        Element element = document.createElement(name);
        document.removeChild(rootElement);
        document.appendChild(element);
        element.appendChild(rootElement);
        rootElement = element;
        return this;
    }

    private MessageBuilder setElementText(Element element, String value) {
        element.getFirstChild().setNodeValue(value);
        return this;
    }

    private Element createElement(String name) {
        return document.createElementNS(namespace, name);
    }

    private Element createTextElement(String name, String value) {
        Element element = document.createElementNS(namespace, name);
        element.appendChild(document.createTextNode(value));
        return element;
    }

    private Element createTextElement(String name, String value, String attributeName, String attributeValue) {
        Element element = createTextElement(name, value);
        element.setAttributeNS(namespace, attributeName, attributeValue);
        return element;
    }

    private void addDefaultMessageHeader() {
        Node messageHeader = rootElement.appendChild(document.createElementNS(namespace, "MessageHeader"));
        Node messageReference = messageHeader.appendChild(document.createElementNS(namespace, "MessageReference"));
        messageType = (Element) messageReference.appendChild(createTextElement("MessageType", "2007"));
        messageTypeVersion = (Element) messageReference.appendChild(createTextElement("MessageTypeVersion", version));
        messageIdentifier = (Element) messageReference.appendChild(createTextElement("MessageIdentifier", "00000000-0000-0000-0000-000000000000"));
        messageDateTime = (Element) messageReference.appendChild(createTextElement("MessageDateTime", "2000-01-01T00:00:00.00"));
        sender = (Element) messageHeader.appendChild(createTextElement("Sender", "0000"));
        recipient = (Element) messageHeader.appendChild(createTextElement("Recipient", "0000"));
    }

    @Override
    public String toString() {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public Element build() {
        return document.getDocumentElement();
    }

    public Document getDocument() {
        return document;
    }
}