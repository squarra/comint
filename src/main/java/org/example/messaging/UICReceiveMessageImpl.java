package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.example.XmlSchemaService;
import org.example.host.Host;
import org.example.host.HostService;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.rabbitmq.HostQueueProducer;
import org.example.routing.RoutingCriteria;
import org.example.routing.RoutingService;
import org.example.util.XmlUtils;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@WebService(endpointInterface = "org.example.messaging.UICReceiveMessage")
public class UICReceiveMessageImpl implements UICReceiveMessage {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final RoutingService routingService;
    private final HostService hostService;
    private final HostQueueProducer hostQueueProducer;
    private final MessageExtractor messageExtractor;
    private final XmlSchemaService xmlSchemaService;

    @Inject
    public UICReceiveMessageImpl(
            LITechnicalAckBuilder liTechnicalAckBuilder,
            RoutingService routingService,
            HostService hostService,
            HostQueueProducer hostQueueProducer,
            MessageExtractor messageExtractor, XmlSchemaService xmlSchemaService) {
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.routingService = routingService;
        this.hostService = hostService;
        this.hostQueueProducer = hostQueueProducer;
        this.messageExtractor = messageExtractor;
        this.xmlSchemaService = xmlSchemaService;
    }

    @Override
    public UICMessageResponse uicMessage(UICMessage parameters, String messageIdentifier, String messageLiHost, boolean compressed, boolean encrypted, boolean signed) {
        MDC.put("MessageId", messageIdentifier);
        Log.info("Received message");

        Element tafTapTsiMessage = extractTafTapTsiMessage((Node) parameters.getMessage());
        RoutingCriteria routingCriteria = messageExtractor.extractRoutingCriteria(tafTapTsiMessage);

        validateAgainstSchema(tafTapTsiMessage, routingCriteria.messageTypeVersion());

        LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.build(tafTapTsiMessage, messageIdentifier);
        Optional<Host> host = findHost(routingCriteria);
        if (host.isEmpty()) {
            liTechnicalAck.setAckIndentifier("NACK");
            return createUICMessageResponse(liTechnicalAck);
        }

        boolean success = hostQueueProducer.send(host.get(), messageIdentifier, tafTapTsiMessage);
        if (!success) {
            liTechnicalAck.setAckIndentifier("NACK");
        }
        return createUICMessageResponse(liTechnicalAck);
    }

    private Element extractTafTapTsiMessage(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new RuntimeException("message node not an element node");
        }

        Element element = (Element) node;
        if (!element.getTagName().equalsIgnoreCase("message")) {
            throw new RuntimeException("message not an element node");
        }

        List<Element> elementChildNodes = XmlUtils.getElementChildNodes(element);
        if (elementChildNodes.size() != 1) {
            throw new RuntimeException("need exactly one child");
        }

        return elementChildNodes.getFirst();
    }

    private void validateAgainstSchema(Element message, String version) {
        Optional<Schema> schema = xmlSchemaService.getSchema(version);
        if (schema.isEmpty()) {
            throw new RuntimeException("No schema found for version " + version);
        }

        try {
            schema.get().newValidator().validate(new DOMSource(message));
        } catch (IOException | SAXException e) {
            throw new RuntimeException("Schema validation failed: " + e.getMessage());
        }
    }

    private Optional<Host> findHost(RoutingCriteria routingCriteria) {
        Optional<String> destination = routingService.getDestination(routingCriteria);
        if (destination.isEmpty()) {
            Log.warn("Failed to find destination in routes");
            return Optional.empty();
        }
        return hostService.getHost(destination.get());
    }

    private UICMessageResponse createUICMessageResponse(LITechnicalAck liTechnicalAck) {
        try {
            UICMessageResponse uicMessageResponse = OBJECT_FACTORY.createUICMessageResponse();
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.newDocument();

            Document tempDocument = documentBuilder.newDocument();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(liTechnicalAck, tempDocument);

            Element liTechnicalAckElement = document.createElement("LI_TechnicalAck");
            Node importedNode = document.importNode(tempDocument.getDocumentElement(), true);
            liTechnicalAckElement.appendChild(importedNode);

            uicMessageResponse.setReturn(liTechnicalAckElement);
            return uicMessageResponse;
        } catch (ParserConfigurationException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
