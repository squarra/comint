package org.example.host;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.heartbeat.HeartbeatSender;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class HeartbeatScheduler {

    private final HostStateService hostStateService;
    private final HeartbeatSender heartbeatSender;
    private final Map<Host, Instant> lastHeartbeats = new HashMap<>();

    @Inject
    public HeartbeatScheduler(HostStateService hostStateService, HeartbeatSender heartbeatSender) {
        this.hostStateService = hostStateService;
        this.heartbeatSender = heartbeatSender;
    }

    public void scheduleHeartbeat(Host host) {
        lastHeartbeats.put(host, Instant.MIN);
    }

    @Scheduled(every = "1s")
    void checkAndSendHeartbeats() {
        lastHeartbeats.keySet().stream()
                .filter(this::shouldSendHeartbeat)
                .forEach(this::processHeartbeat);
    }

    private boolean shouldSendHeartbeat(Host host) {
        return lastHeartbeats.get(host)
                .plusSeconds(host.getHeartbeatInterval())
                .isBefore(Instant.now());
    }

    private void processHeartbeat(Host host) {
        lastHeartbeats.put(host, Instant.now());
        boolean success = heartbeatSender.sendHeartbeat(host);

        if (success) {
            hostStateService.heartbeatSuccess(host);
        } else {
            hostStateService.heartbeatFailure(host);
        }
    }
}
