package org.example.log;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.host.Host;
import org.jboss.logging.Logger;

@ApplicationScoped
public class HostLogger {

    private static final Logger LOG = Logger.getLogger("protocol.host");

    public void debug(Host host, String format, Object... args) {
        LOG.debugf(format(host, format), args);
    }

    public void info(Host host, String format, Object... args) {
        LOG.infof(format(host, format), args);
    }

    public void warn(Host host, String format, Object... args) {
        LOG.warnf(format(host, format), args);
    }

    public void warn(Host host, Throwable throwable, String format, Object... args) {
        LOG.warnf(throwable, format(host, format), args);
    }

    public void error(Host host, String format, Object... args) {
        LOG.errorf(format(host, format), args);
    }

    public void error(Host host, Throwable throwable, String format, Object... args) {
        LOG.errorf(throwable, format(host, format), args);
    }

    private String format(Host host, String message) {
        return String.format("[%s] %s", host.getName(), message);
    }
}
