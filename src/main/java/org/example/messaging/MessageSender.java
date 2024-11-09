package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.ws.BindingProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.host.Host;
import org.example.messaging.ack.LITechnicalAck;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@ApplicationScoped
public class MessageSender {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final LIReceiveMessageService MESSAGE_SERVICE = new LIReceiveMessageService();

    private final String messageLiHost;

    @Inject
    public MessageSender(@ConfigProperty(name = "messageLiHost", defaultValue = "localhost") String messageLiHost) {
        this.messageLiHost = messageLiHost;
    }

    public boolean sendMessage(Host host, String messageIdentifier, Element message) {
        UICReceiveMessage client = createClient(host);
        if (client == null) return false;

        try {
            UICMessage uicMessage = createMessage(message);
            UICMessageResponse response = client.uicMessage(uicMessage, messageIdentifier, messageLiHost, false, false, false);
            Log.info("Sent message");

            Node returnNode = (Node) response.getReturn();
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            LITechnicalAck liTechnicalAck = (LITechnicalAck) unmarshaller.unmarshal(returnNode.getFirstChild());
            Log.infof("Received LITechnicalAck with responseStatus '%s'", liTechnicalAck.getResponseStatus());

            return true;
        } catch (Exception e) {
            Log.errorf("Failed to send message: %s", e.getMessage());
            return false;
        }
    }

    private UICReceiveMessage createClient(Host host) {
        try {
            UICReceiveMessage client = MESSAGE_SERVICE.getUICReceiveMessagePort();
            BindingProvider bindingProvider = (BindingProvider) client;

            String url = host.getUrl() + host.getMessagingEndpoint();
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

            return client;
        } catch (Exception e) {
            Log.errorf("Failed to create SOAP client: %s", e.getMessage());
            return null;
        }
    }

    private UICMessage createMessage(Element message) {
        UICMessage uicMessage = OBJECT_FACTORY.createUICMessage();
        uicMessage.setMessage(message);
        return uicMessage;
    }
}
