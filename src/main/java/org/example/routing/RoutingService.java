package org.example.routing;

import com.opencsv.bean.CsvToBeanBuilder;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.rabbitmq.AcceptQueueMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoutingService {

    private final String hostsFile;
    private final String routingFile;
    private final List<Host> hosts;
    private final List<Route> routes;

    public RoutingService(@ConfigProperty(name = "hosts-file", defaultValue = "hosts.csv") String hostsFile,
                          @ConfigProperty(name = "app.config.routing-file", defaultValue = "routing.csv") String routingFile) {
        this.hostsFile = hostsFile;
        this.routingFile = routingFile;
        hosts = new ArrayList<>();
        routes = new ArrayList<>();
    }

    void onStart(@Observes StartupEvent ev) {
        hosts.addAll(loadCsv(hostsFile, Host.class));
        Log.infof("Loaded %d hosts successfully", hosts.size());

        routes.addAll(loadCsv(routingFile, Route.class));
        Log.infof("Loaded %d routes successfully", routes.size());
    }

    private <T> List<T> loadCsv(String filename, Class<T> type) {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                Log.warnf("Could not find resource %s", filename);
                return Collections.emptyList();
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return new CsvToBeanBuilder<T>(reader)
                        .withType(type)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse();
            }
        } catch (IOException e) {
            Log.warnf("Failed to parse %s: %s", filename, e.getMessage());
            Log.debug(e);
            return Collections.emptyList();
        }
    }

    public boolean isKnownHost(String name) {
        return hosts.stream().anyMatch(host -> host.getName().equals(name));
    }

    public Optional<Route> getRoute(AcceptQueueMessage message) {
        return routes.stream()
                .filter(route -> route.getMessageType().equals(message.messageType()))
                .filter(route -> route.getMessageTypeVersion().equals(message.messageTypeVersion()))
                .filter(route -> route.getRecipient().equals(message.recipient()))
                .findFirst();
    }

    public Optional<Host> getHost(String name) {
        return hosts.stream().filter(host -> host.getName().equals(name)).findFirst();
    }
}
