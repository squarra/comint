package org.example.heartbeat;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.ws.BindingProvider;
import org.example.host.Host;
import org.jboss.logmanager.MDC;

import java.util.List;

@ApplicationScoped
public class HeartbeatSender {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final LIHBMessageService MESSAGE_SERVICE = new LIHBMessageService();
    private static final String HEARTBEAT_MESSAGE = "Are you alive?";
    private static final String HEARTBEAT_RESPONSE = "HEART_BEAT_WS_RECEIVED";

    public boolean sendHeartbeat(Host host) {
        MDC.put("Host", host.getName());
        UICHBMessage client = createClient(host);
        if (client == null) return false;

        try {
            UICHBMessage_Type message = createHeartbeatMessage();
            List<Object> response = client.uichbMessage(message.getMessage(), message.getProperties());
            Log.debug("Sent heartbeat");

            for (Object object : response) {
                Log.debugf("Received heartbeat response: %s", object);
                if (object.equals(HEARTBEAT_RESPONSE)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.errorf("Failed to send heartbeat: %s", e.getMessage());
        }
        return false;
    }

    private UICHBMessage createClient(Host host) {
        try {
            UICHBMessage client = MESSAGE_SERVICE.getUICHBMessagePort();
            BindingProvider bindingProvider = (BindingProvider) client;

            String url = host.getUrl() + host.getHeartbeatEndpoint();
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

            return client;
        } catch (Exception e) {
            Log.errorf("Failed to create SOAP client: ", e.getMessage());
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
