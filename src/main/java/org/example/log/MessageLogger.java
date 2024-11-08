package org.example.log;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageLogger {

    private static final Logger LOG = Logger.getLogger("protocol.event");

    public void debug(String messageIdentifier, String format, Object... args) {
        LOG.debugf(format(messageIdentifier, format), args);
    }

    public void info(String messageIdentifier, String format, Object... args) {
        LOG.infof(format(messageIdentifier, format), args);
    }

    public void warn(String messageIdentifier, String format, Object... args) {
        LOG.warnf(format(messageIdentifier, format), args);
    }

    public void warn(String messageIdentifier, Throwable throwable, String format, Object... args) {
        LOG.warnf(throwable, format(messageIdentifier, format), args);
    }

    public void error(String messageIdentifier, String format, Object... args) {
        LOG.errorf(format(messageIdentifier, format), args);
    }

    public void error(String messageIdentifier, Throwable throwable, String format, Object... args) {
        LOG.errorf(throwable, format(messageIdentifier, format), args);
    }

    private String format(String messageIdentifier, String message) {
        return String.format("[%s] %s", messageIdentifier, message);
    }
}
