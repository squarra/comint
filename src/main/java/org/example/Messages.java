package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Locale;
import java.util.ResourceBundle;

@ApplicationScoped
public class Messages {

    private static final String BUNDLE_NAME = "messages";
    private final ResourceBundle bundle;


    @Inject
    public Messages(@ConfigProperty(name = "app.locale", defaultValue = "en") String locale) {
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.forLanguageTag(locale));
    }

    // For testing
    Messages(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public String get(MessageKey key, Object... args) {
        String pattern = bundle.getString(key.getKey());
        return String.format(pattern, args);
    }
}
