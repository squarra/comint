package org.example.inbound;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.example.MessageSendException;
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

    private final Map<String, InboundConnectorService> clientCache = new HashMap<>();

    public void sendMessage(String endpoint, Document message) throws MessageSendException {
        Log.debugf("Sending message to %s", endpoint);
        InboundConnectorService client = getOrCreateClient(endpoint);

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

    private InboundConnectorService getOrCreateClient(String endpoint) {
        return clientCache.computeIfAbsent(endpoint, k -> createClient(endpoint));
    }

    private InboundConnectorService createClient(String endpoint) {
        InboundConnectorService client = INBOUND_SERVICE.getInboundConnectorServicePort();
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        return client;
    }

    private SendInboundMessage createInboundMessage(Document message) throws ParserConfigurationException, IOException, SAXException {
        SendInboundMessage inboundMessage = OBJECT_FACTORY.createSendInboundMessage();
        inboundMessage.setMessage(message.getDocumentElement());
        return inboundMessage;
    }
}
