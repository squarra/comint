package org.example.host;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.logging.MdcKeys;
import org.example.rabbitmq.HostQueueConsumer;
import org.example.util.CsvFileReader;
import org.jboss.logmanager.MDC;

import java.util.List;
import java.util.Optional;

@Startup
@ApplicationScoped
public class HostService {

    private static final String HOSTS_FILE = "hosts.csv";

    private final HostStateService hostStateService;
    private final HostQueueConsumer hostQueueConsumer;
    private final HeartbeatScheduler heartbeatScheduler;
    private final List<Host> hosts = CsvFileReader.readFile(HOSTS_FILE, Host.class);

    @Inject
    public HostService(
            HostStateService hostStateService,
            HostQueueConsumer hostQueueConsumer,
            HeartbeatScheduler heartbeatScheduler
    ) {
        this.hostStateService = hostStateService;
        this.hostQueueConsumer = hostQueueConsumer;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @PostConstruct
    void init() {
        initializeHosts();
    }

    private void initializeHosts() {
        Log.info("***** Initializing hosts *****");
        hosts.forEach(this::initializeHost);
    }

    private void initializeHost(Host host) {
        MDC.put(MdcKeys.HOST_NAME, host.getName());
        Log.info(host);
        hostStateService.initializeHostState(host);
        hostQueueConsumer.startConsuming(host);
        if (host.getHeartbeatInterval() != 0) heartbeatScheduler.scheduleHeartbeat(host);
    }

    public Optional<Host> getHost(String name) {
        return hosts.stream().filter(host -> host.getName().equals(name)).findFirst();
    }
}
