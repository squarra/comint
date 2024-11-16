package org.example.inbound;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.example.MessageSendException;
import org.example.host.Host;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class InboundMessageSender {

    private static final InboundConnectorService_Service INBOUND_SERVICE = new InboundConnectorService_Service();
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final XmlUtilityService xmlUtilityService;
    private final Map<Host, InboundConnectorService> clientCache = new HashMap<>();

    public InboundMessageSender(XmlUtilityService xmlUtilityService) {
        this.xmlUtilityService = xmlUtilityService;
    }

    public void sendMessage(Host host, String message) throws MessageSendException {
        Log.debug("Sending message to host");
        InboundConnectorService client = getOrCreateClient(host);

        try {
            SendInboundMessage inboundMessage = createInboundMessage(message);
            SendInboundMessageResponse response = client.sendInboundMessage(inboundMessage, "false");
            if (!response.getResponse().equals("success")) {
                throw new MessageSendException(MessageSendException.FailureType.MESSAGE_REJECTED);
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new MessageSendException(MessageSendException.FailureType.REQUEST_CREATION_ERROR);
        } catch (WebServiceException e) {
            throw new MessageSendException(MessageSendException.FailureType.HOST_UNREACHABLE);
        }
    }

    private InboundConnectorService getOrCreateClient(Host host) {
        return clientCache.computeIfAbsent(host, k -> createClient(host));
    }

    private InboundConnectorService createClient(Host host) {
        InboundConnectorService client = INBOUND_SERVICE.getInboundConnectorServicePort();
        BindingProvider bindingProvider = (BindingProvider) client;

        String url = host.getUrl() + host.getMessagingEndpoint();
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

        return client;
    }

    private SendInboundMessage createInboundMessage(String message) throws ParserConfigurationException, IOException, SAXException {
        Document document = xmlUtilityService.parseXmlString(message);
        SendInboundMessage inboundMessage = OBJECT_FACTORY.createSendInboundMessage();
        inboundMessage.setMessage(document.getDocumentElement());
        return inboundMessage;
    }
}
