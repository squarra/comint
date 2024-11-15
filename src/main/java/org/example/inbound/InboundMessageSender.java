package org.example.inbound;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.example.host.Host;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@ApplicationScoped
public class InboundMessageSender {

    private static final InboundConnectorService_Service INBOUND_SERVICE = new InboundConnectorService_Service();
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final XmlUtilityService xmlUtilityService;

    public InboundMessageSender(XmlUtilityService xmlUtilityService) {
        this.xmlUtilityService = xmlUtilityService;
    }

    public boolean sendMessage(Host host, String message) {
        InboundConnectorService client = createClient(host);

        try {
            SendInboundMessage inboundMessage = createInboundMessage(message);
            SendInboundMessageResponse response = client.sendInboundMessage(inboundMessage, "false");

            Log.infof("Received response '%s'", response.getResponse());

            return true;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            Log.error("Error creating InboundMessage", e);
            return false;
        } catch (WebServiceException e) {
            Log.errorf("Failed to send message: %s", e.getMessage());
            return false;
        }
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
