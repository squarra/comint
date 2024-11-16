package org.example.messaging;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.MessageSendException;
import org.example.host.Host;
import org.example.messaging.ack.LITechnicalAck;
import org.example.messaging.ack.LITechnicalAckBuilder;
import org.example.util.XmlUtilityService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class UICMessageSender {

    private static final LIReceiveMessageService MESSAGE_SERVICE = new LIReceiveMessageService();
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private final LITechnicalAckBuilder liTechnicalAckBuilder;
    private final XmlUtilityService xmlUtilityService;
    private final String messageLiHost;
    private final Map<Host, UICReceiveMessage> clientCache = new HashMap<>();

    public UICMessageSender(
            LITechnicalAckBuilder liTechnicalAckBuilder,
            XmlUtilityService xmlUtilityService,
            @ConfigProperty(name = "messageLiHost", defaultValue = "localhost") String messageLiHost
    ) {
        this.liTechnicalAckBuilder = liTechnicalAckBuilder;
        this.xmlUtilityService = xmlUtilityService;
        this.messageLiHost = messageLiHost;
    }

    public void sendMessage(Host host, String messageIdentifier, String message) throws MessageSendException {
        Log.debug("Sending message to host");
        UICReceiveMessage client = getOrCreateClient(host);

        try {
            UICMessage uicMessage = createUICMessage(message);
            UICMessageResponse response = client.uicMessage(uicMessage, messageIdentifier, messageLiHost, false, false, false);

            LITechnicalAck liTechnicalAck = liTechnicalAckBuilder.unmarshal((Node) response.getReturn());
            if (!(liTechnicalAck.getResponseStatus().equals("ACK"))) {
                throw new MessageSendException(MessageSendException.FailureType.MESSAGE_REJECTED);
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new MessageSendException(MessageSendException.FailureType.REQUEST_CREATION_ERROR, e);
        } catch (WebServiceException e) {
            throw new MessageSendException(MessageSendException.FailureType.HOST_UNREACHABLE);
        } catch (JAXBException e) {
            throw new MessageSendException(MessageSendException.FailureType.RESPONSE_PROCESSING_ERROR, e);
        }
    }

    private UICReceiveMessage getOrCreateClient(Host host) {
        return clientCache.computeIfAbsent(host, k -> createClient(host));
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
