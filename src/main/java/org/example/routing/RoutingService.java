package org.example.routing;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.util.CsvFileReader;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoutingService {

    private static final String ROUTING_FILE = "routing.csv";

    private final List<Route> routes = CsvFileReader.readFile(ROUTING_FILE, Route.class);

    public Optional<String> getDestination(RoutingCriteria routingCriteria) {
        return routes.stream()
                .filter(route -> matches(route.getMessageType(), routingCriteria.messageType()))
                .filter(route -> matches(route.getMessageTypeVersion(), routingCriteria.messageTypeVersion()))
                .filter(route -> matches(route.getRecipient(), routingCriteria.recipient()))
                .map(Route::getDestination)
                .findFirst();
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
