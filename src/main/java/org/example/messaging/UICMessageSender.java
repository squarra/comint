package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.ws.BindingProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.host.Host;
import org.example.messaging.ack.LITechnicalAck;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@ApplicationScoped
public class UICMessageSender {

    private static final LIReceiveMessageService MESSAGE_SERVICE = new LIReceiveMessageService();
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final XmlUtilityService xmlUtilityService;
    private final String messageLiHost;

    public UICMessageSender(
            XmlUtilityService xmlUtilityService,
            @ConfigProperty(name = "messageLiHost", defaultValue = "localhost") String messageLiHost
    ) {
        this.xmlUtilityService = xmlUtilityService;
        this.messageLiHost = messageLiHost;
    }

    public boolean sendMessage(Host host, String messageIdentifier, String message) {
        UICReceiveMessage client = createClient(host);
        if (client == null) return false;

        try {
            UICMessage uicMessage = createUICMessage(message);
            UICMessageResponse response = client.uicMessage(uicMessage, messageIdentifier, messageLiHost, false, false, false);
            Log.info("Sent message");

            Node returnNode = (Node) response.getReturn();
            JAXBContext context = JAXBContext.newInstance(LITechnicalAck.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            LITechnicalAck liTechnicalAck = (LITechnicalAck) unmarshaller.unmarshal(returnNode.getFirstChild());
            Log.infof("Received LITechnicalAck with responseStatus '%s'", liTechnicalAck.getResponseStatus());

            return true;
        } catch (Exception e) {
            // Either bad response or host not reachable
            Log.errorf("Failed to send message: %s", e.getMessage());
            return false;
        }
    }

    private UICReceiveMessage createClient(Host host) {
        UICReceiveMessage client = MESSAGE_SERVICE.getUICReceiveMessagePort();
        BindingProvider bindingProvider = (BindingProvider) client;

        String url = host.getUrl() + host.getMessagingEndpoint();
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

        return client;
    }

    private UICMessage createUICMessage(String message) throws ParserConfigurationException, IOException, SAXException {
        Document document = xmlUtilityService.parseXmlString(message);
        Element wrapperRoot = document.createElement("wrapper");
        Node originalRoot = document.getDocumentElement();
        document.removeChild(originalRoot);
        wrapperRoot.appendChild(originalRoot);
        document.appendChild(wrapperRoot);

        UICMessage uicMessage = OBJECT_FACTORY.createUICMessage();
        uicMessage.setMessage(wrapperRoot);
        return uicMessage;
    }
}
