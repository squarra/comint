package org.example.host;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.log.HostLogger;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class HostStateService {

    private final HostLogger hostLogger;
    private final Map<Host, HostState> hostStates;

    @Inject
    public HostStateService(HostLogger hostLogger) {
        this.hostLogger = hostLogger;
        hostStates = new HashMap<>();
    }

    public void initializeHostState(Host host) {
        hostStates.put(host, HostState.ACTIVE);
    }

    public void updateToActive(Host host) {
        updateHostState(host, HostState.ACTIVE);
    }

    public void updateToInactive(Host host) {
        updateHostState(host, HostState.INACTIVE);
    }

    public void updateToAbandoned(Host host) {
        updateHostState(host, HostState.ABANDONED);
    }

    public boolean isActive(Host host) {
        return hostStates.get(host) == HostState.ACTIVE;
    }

    public boolean isInactive(Host host) {
        return hostStates.get(host) == HostState.INACTIVE;
    }

    public boolean isAbandoned(Host host) {
        return hostStates.get(host) == HostState.ABANDONED;
    }

    private void updateHostState(Host host, HostState hostState) {
        if (hostStates.get(host) == null) {
            hostLogger.warn(host, "Host was never initialized");
            return;
        }
        HostState previous = hostStates.replace(host, hostState);
        if (previous != hostState) {
            hostLogger.info(host, "Updated state from %s to %s", previous, hostState);
        }
    }

    private enum HostState {
        ACTIVE,
        INACTIVE,
        ABANDONED
    }
}
