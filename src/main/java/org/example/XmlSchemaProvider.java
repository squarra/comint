package org.example;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class XmlSchemaProvider {

    private final Map<String, Schema> version2Schema = new HashMap<>();
    private static final String SCHEMA_DIRECTORY = "src/main/resources/schemas";

    @PostConstruct
    void init() {
        Log.info("***** Loading XSD files *****");
        loadSchemas();
        Log.infof("Found schema for versions: %s", version2Schema.keySet());
    }

    private void loadSchemas() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            try (Stream<Path> paths = Files.walk(Paths.get(SCHEMA_DIRECTORY))) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".xsd"))
                        .forEach(path -> loadSchema(path, schemaFactory));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void loadSchema(Path path, SchemaFactory schemaFactory) {
        try {
            String filename = path.getFileName().toString();
            String version = extractVersion(filename);
            if (version != null) {
                Schema schema = schemaFactory.newSchema(path.toFile());
                version2Schema.put(version, schema);
            }
        } catch (SAXException e) {
            throw new RuntimeException("Failed to load schema: " + path, e);
        }
    }

    private String extractVersion(String filename) {
        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore == -1) return null;

        String version = filename.substring(lastUnderscore + 1, filename.length() - 4);
        return normalize(version);
    }

    private String normalize(String version) {
        if (version.endsWith(".0")) {
            return normalize(version.substring(0, version.length() - 2));
        }
        return version;
    }

    public Optional<Schema> getSchema(String version) {
        return Optional.ofNullable(version2Schema.get(normalize(version)));
    }
}
