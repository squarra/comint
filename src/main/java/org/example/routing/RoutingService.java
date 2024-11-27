package org.example.routing;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.MessageExtractor;
import org.example.host.Host;
import org.example.host.HostService;
import org.example.logging.MdcKeys;
import org.example.util.CsvFileReader;
import org.jboss.logmanager.MDC;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoutingService {

    private static final String ROUTING_FILE = "routing.csv";

    private final HostService hostService;
    private final MessageExtractor messageExtractor;
    private final List<Route> routes = CsvFileReader.readFile(ROUTING_FILE, Route.class);

    public RoutingService(
            HostService hostService,
            MessageExtractor messageExtractor
    ) {
        this.hostService = hostService;
        this.messageExtractor = messageExtractor;
    }

    public Host findHost(Element message) {
        RoutingCriteria routingCriteria = messageExtractor.extractRoutingCriteria(message);
        Log.debug(routingCriteria);

        String destination = getDestination(routingCriteria);
        if (destination == null) {
            Log.errorf("Failed to find destination for %s", routingCriteria.toString());
            return null;
        }

        Optional<Host> host = hostService.getHost(destination);
        if (host.isEmpty()) {
            Log.errorf("Failed to find host for destination '%s'", destination);
            return null;
        }

        MDC.put(MdcKeys.HOST_NAME, host.get().getName());
        return host.get();
    }

    private String getDestination(RoutingCriteria routingCriteria) {
        return routes.stream()
                .filter(route -> matches(route.getMessageType(), routingCriteria.messageType()))
                .filter(route -> matches(route.getMessageTypeVersion(), routingCriteria.messageTypeVersion()))
                .filter(route -> matches(route.getRecipient(), routingCriteria.recipient()))
                .map(Route::getDestination)
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String pattern, String value) {
        if (pattern == null || value == null) {
            return pattern == null && value == null;
        }
        if (pattern.equals("*")) {
            return true;
        }
        return pattern.equals(value);
    }
}
