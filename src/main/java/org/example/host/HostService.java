package org.example.host;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.logging.MDCKeys;
import org.example.rabbitmq.HostQueueConsumer;
import org.example.rabbitmq.HostQueueProducer;
import org.example.util.CsvFileReader;
import org.jboss.logmanager.MDC;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HostService {

    private static final String HOSTS_FILE = "hosts.csv";

    private final HostStateService hostStateService;
    private final HostQueueProducer hostQueueProducer;
    private final HostQueueConsumer hostQueueConsumer;
    private final HeartbeatScheduler heartbeatScheduler;
    private final boolean consumerEnabled;
    private final List<Host> hosts;

    @Inject
    public HostService(
            HostStateService hostStateService,
            HostQueueProducer hostQueueProducer,
            HostQueueConsumer hostQueueConsumer,
            HeartbeatScheduler heartbeatScheduler,
            @ConfigProperty(name = "comint.host.consumer.enable", defaultValue = "true") boolean consumerEnabled
    ) {
        this.hostStateService = hostStateService;
        this.hostQueueProducer = hostQueueProducer;
        this.hostQueueConsumer = hostQueueConsumer;
        this.heartbeatScheduler = heartbeatScheduler;
        this.consumerEnabled = consumerEnabled;
        this.hosts = CsvFileReader.readFile(HOSTS_FILE, Host.class);
    }

    void onStart(@Observes StartupEvent ev) {
        MDC.clear();
        Log.info("***** Initializing hosts *****");
        initializeHosts();
        MDC.clear();
    }

    private void initializeHosts() {
        hosts.forEach(host -> {
            MDC.put(MDCKeys.HOST_NAME, host.getName());

            hostStateService.initializeHostState(host);
            hostQueueProducer.initializeHostQueue(host);
            if (consumerEnabled) hostQueueConsumer.startConsuming(host);
            if (host.getHeartbeatInterval() != 0) heartbeatScheduler.scheduleHeartbeat(host);
        });
    }

    public Optional<Host> getHost(String name) {
        return hosts.stream().filter(host -> host.getName().equals(name)).findFirst();
    }
}