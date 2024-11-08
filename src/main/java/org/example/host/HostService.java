package org.example.host;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.example.rabbitmq.HostQueueProducer;
import org.example.util.CsvFileReader;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HostService {

    private static final String HOSTS_FILE = "hosts.csv";

    private final HostStateService hostStateService;
    private final HostQueueProducer hostQueueProducer;
    private final HeartbeatScheduler heartbeatScheduler;
    private final List<Host> hosts;

    @Inject
    public HostService(
            HostStateService hostStateService,
            HostQueueProducer hostQueueProducer,
            HeartbeatScheduler heartbeatScheduler) {
        this.hostStateService = hostStateService;
        this.hostQueueProducer = hostQueueProducer;
        this.heartbeatScheduler = heartbeatScheduler;
        this.hosts = CsvFileReader.readFile(HOSTS_FILE, Host.class);
    }

    void onStart(@Observes StartupEvent ev) {
        initializeHostStates();
        initializeHostQueues();
        scheduleHeartbeats();
    }

    private void initializeHostStates() {
        hosts.forEach(hostStateService::initializeHostState);
    }

    private void initializeHostQueues() {
        hosts.forEach(hostQueueProducer::initializeHostQueue);
    }

    private void scheduleHeartbeats() {
        hosts.stream().filter(this::hasHeartbeat).forEach(heartbeatScheduler::scheduleHeartbeat);
    }

    private boolean hasHeartbeat(Host host) {
        return host.getHeartbeatInterval() != 0;
    }

    public Optional<Host> getHost(String name) {
        return hosts.stream().filter(host -> host.getName().equals(name)).findFirst();
    }
}
