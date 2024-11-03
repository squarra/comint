package org.example;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class XmlSchemaService {

    private static final String SCHEMA_DIRECTORY = "schemas/";
    private static final List<String> SCHEMA_FILES = List.of(
            "taf_cat_complete_sector_3.1.0.0.xsd",
            "taf_cat_complete_sector_3.5.0.0.xsd"
    );

    private final Map<String, Schema> version2Schema = new HashMap<>();

    void onStart(@Observes StartupEvent event) {
        loadSchemas();
        Log.infof("Found schema for versions: %s", version2Schema.keySet());
    }

    private void loadSchemas() {
        SchemaFactory factory = createSecureSchemaFactory();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SCHEMA_FILES.forEach(schemaFile -> loadSchema(schemaFile, factory, classLoader));
    }

    private SchemaFactory createSecureSchemaFactory() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            Log.warn("Could not set XML security properties", e);
        }
        return factory;
    }

    private void loadSchema(String schemaFile, SchemaFactory schemaFactory, ClassLoader classLoader) {
        try (InputStream schemaStream = classLoader.getResourceAsStream(SCHEMA_DIRECTORY + schemaFile)) {
            String version = normalize(extractVersion(schemaFile));
            Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
            version2Schema.put(version, schema);
        } catch (IOException | SAXException e) {
            Log.errorf(e, "Error loading schema file: %s", schemaFile);
        }
    }

    private String extractVersion(String filename) {
        int lastUnderscore = filename.lastIndexOf('_');
        return filename.substring(lastUnderscore + 1, filename.length() - 4);
    }

    private String normalize(String version) {
        return version.endsWith(".0") ? normalize(version.substring(0, version.length() - 2)) : version;
    }

    public Optional<Schema> getSchema(String version) {
        return Optional.ofNullable(version2Schema.get(normalize(version)));
    }
}
