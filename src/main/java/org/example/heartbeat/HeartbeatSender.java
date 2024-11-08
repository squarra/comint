package org.example.heartbeat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.ws.BindingProvider;
import org.example.host.Host;
import org.example.log.HostLogger;
import org.w3c.dom.Node;

import java.util.List;

@ApplicationScoped
public class HeartbeatSender {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final LIHBMessageService MESSAGE_SERVICE = new LIHBMessageService();
    private static final String HEARTBEAT_MESSAGE = "Are you alive?";
    private static final String HEARTBEAT_ENDPOINT = "/mockheartbeat";

    private final HostLogger hostLogger;

    @Inject
    public HeartbeatSender(HostLogger hostLogger) {
        this.hostLogger = hostLogger;
    }

    public boolean sendHeartbeat(Host host) {
        UICHBMessage client = createClient(host);
        if (client == null) return false;

        try {
            UICHBMessage_Type message = createHeartbeatMessage();
            List<Object> response = client.uichbMessage(message.getMessage(), message.getProperties());
            hostLogger.debug(host, "Successfully sent heartbeat");

            if (response == null) {
                return false;
            }

            for (Object object : response) {
                if (object instanceof Node node) {
                    hostLogger.debug(host, "Received heartbeat response: %s", node.getTextContent());
                }
            }
            return true;
        } catch (Exception e) {
            hostLogger.error(host, "Failed to send heartbeat: %s", e.getMessage());
            return false;
        }
    }

    private UICHBMessage createClient(Host host) {
        try {
            UICHBMessage client = MESSAGE_SERVICE.getUICHBMessagePort();
            BindingProvider bindingProvider = (BindingProvider) client;

            String url = host.getUrl() + HEARTBEAT_ENDPOINT;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

            hostLogger.debug(host, "Successfully created SOAP client");
            return client;
        } catch (Exception e) {
            hostLogger.error(host, e, "Failed to create SOAP client");
            return null;
        }
    }

    private UICHBMessage_Type createHeartbeatMessage() {
        UICHBMessage_Type message = OBJECT_FACTORY.createUICHBMessage_Type();
        message.setMessage(HEARTBEAT_MESSAGE);
        message.setProperties("timestamp=" + System.currentTimeMillis());
        return message;
    }
}
