package org.example.inbound;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.example.MessageSendException;
import org.example.host.Host;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class InboundMessageSender {

    private static final InboundConnectorService_Service INBOUND_SERVICE = new InboundConnectorService_Service();
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String SUCCESS_MESSAGE = "success";

    private final Map<Host, InboundConnectorService> clientCache = new HashMap<>();

    public void sendMessage(Host host, Document message) throws MessageSendException {
        Log.debug("Sending message to host");
        InboundConnectorService client = getOrCreateClient(host);

        try {
            SendInboundMessage inboundMessage = createInboundMessage(message);
            SendInboundMessageResponse response = client.sendInboundMessage(inboundMessage, "false");

            String content = (response.getResponse() instanceof Element element)
                    ? element.getTextContent()
                    : (String) response.getResponse();

            if (!content.equalsIgnoreCase(SUCCESS_MESSAGE)) {
                Log.errorf("Message was not %s but: %s", SUCCESS_MESSAGE, content);
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

    private SendInboundMessage createInboundMessage(Document message) throws ParserConfigurationException, IOException, SAXException {
        SendInboundMessage inboundMessage = OBJECT_FACTORY.createSendInboundMessage();
        inboundMessage.setMessage(message.getDocumentElement());
        return inboundMessage;
    }
}
