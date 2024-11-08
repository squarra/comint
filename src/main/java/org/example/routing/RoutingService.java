package org.example.routing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.messaging.MessageExtractor;
import org.example.util.CsvFileReader;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoutingService {

    private static final String ROUTING_FILE = "routing.csv";

    private final MessageExtractor messageExtractor;
    private final List<Route> routes;

    @Inject
    public RoutingService(MessageExtractor messageExtractor) {
        this.messageExtractor = messageExtractor;
        routes = CsvFileReader.readFile(ROUTING_FILE, Route.class);
    }

    public Optional<String> getDestination(Element message) {
        String messageType = messageExtractor.extractMessageType(message);
        String messageTypeVersion = messageExtractor.extractMessageTypeVersion(message);
        String recipient = messageExtractor.extractRecipient(message);
        return routes.stream()
                .filter(route -> route.getMessageType().equals(messageType))
                .filter(route -> route.getMessageTypeVersion().equals(messageTypeVersion))
                .filter(route -> route.getRecipient().equals(recipient))
                .map(Route::getDestination)
                .findFirst();
    }
}
