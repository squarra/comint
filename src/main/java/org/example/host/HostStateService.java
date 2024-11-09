package org.example.host;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class HostStateService {

    private final Map<Host, HostState> hostStates = new HashMap<>();

    public void initializeHostState(Host host) {
        hostStates.put(host, HostState.ACTIVE);
    }

    public void heartbeatSuccess(Host host) {
        if (isInactive(host) || isAbandoned(host)) {
            updateToActive(host);
        }
    }

    public void heartbeatFailure(Host host) {
        if (isActive(host)) {
            updateToInactive(host);
        }
    }

    public void messageDeliveryFailure(Host host) {
        if (isInactive(host)) {
            updateToAbandoned(host);
        }
    }

    private void updateToActive(Host host) {
        updateHostState(host, HostState.ACTIVE);
    }

    private void updateToInactive(Host host) {
        updateHostState(host, HostState.INACTIVE);
    }

    private void updateToAbandoned(Host host) {
        updateHostState(host, HostState.ABANDONED);
    }

    private boolean isActive(Host host) {
        return hostStates.get(host) == HostState.ACTIVE;
    }

    private boolean isInactive(Host host) {
        return hostStates.get(host) == HostState.INACTIVE;
    }

    private boolean isAbandoned(Host host) {
        return hostStates.get(host) == HostState.ABANDONED;
    }

    private void updateHostState(Host host, HostState hostState) {
        if (hostStates.get(host) == null) {
            Log.warn("Host was not initialized");
            return;
        }
        HostState previous = hostStates.replace(host, hostState);
        if (previous != hostState) {
            Log.infof("Updated host state from %s to %s", previous, hostState);
        }
    }

    private enum HostState {
        ACTIVE,
        INACTIVE,
        ABANDONED
    }
}
