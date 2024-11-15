package org.example.inbound;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.host.Host;

@ApplicationScoped
public class InboundMessageSender {

    public boolean sendMessage(Host host, String message) {
        return true;
    }
}
