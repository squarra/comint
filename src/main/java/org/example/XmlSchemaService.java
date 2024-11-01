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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class XmlSchemaService {

    private static final String SCHEMA_DIRECTORY = "schemas/";

    private final Map<String, Schema> version2Schema = new HashMap<>();

    void onStart(@Observes StartupEvent event) {
        loadSchemas();
        Log.infof("Found schema for versions: %s", version2Schema.keySet());
    }

    private void loadSchemas() {
        try {
            SchemaFactory schemaFactory = createSecureSchemaFactory();
            getSchemaFiles().forEach(schemaFile -> loadSchema(schemaFile, schemaFactory));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schemas from directory: " + SCHEMA_DIRECTORY, e);
        }
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

    private Set<String> getSchemaFiles() throws IOException {
        URL directoryUrl = Thread.currentThread().getContextClassLoader().getResource(SCHEMA_DIRECTORY);
        if (directoryUrl == null) {
            throw new IOException("Schema directory not found: " + SCHEMA_DIRECTORY);
        }

        URI schemaUri = getSchemaUri(directoryUrl);
        return schemaUri.getScheme().equals("jar") ? getJarSchemaFiles(schemaUri) : getFileSystemSchemaFiles(schemaUri);
    }

    private URI getSchemaUri(URL directoryUrl) throws IOException {
        try {
            return directoryUrl.toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI for schema directory", e);
        }
    }

    private Set<String> getJarSchemaFiles(URI schemaUri) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(schemaUri, Collections.emptyMap())) {
            return findSchemaFiles(fs.getPath(SCHEMA_DIRECTORY));
        }
    }

    private Set<String> getFileSystemSchemaFiles(URI schemaUri) throws IOException {
        return findSchemaFiles(Paths.get(schemaUri));
    }

    private Set<String> findSchemaFiles(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory, 1)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".xsd"))
                    .collect(Collectors.toSet());
        }
    }

    private void loadSchema(String schemaFile, SchemaFactory schemaFactory) {
        try (InputStream schemaStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SCHEMA_DIRECTORY + schemaFile)) {
            if (schemaStream == null) {
                Log.warnf("Schema file not found: %s", schemaFile);
                return;
            }

            Optional<String> version = extractVersion(schemaFile);
            if (version.isPresent()) {
                version2Schema.put(version.get(), schemaFactory.newSchema(new StreamSource(schemaStream)));
            }
        } catch (IOException | SAXException e) {
            Log.errorf(e, "Error loading schema file: %s", schemaFile);
        }
    }

    private Optional<String> extractVersion(String filename) {
        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore == -1) return Optional.empty();

        String version = filename.substring(lastUnderscore + 1, filename.length() - 4);
        return Optional.of(normalize(version));
    }

    private String normalize(String version) {
        return version.endsWith(".0") ? normalize(version.substring(0, version.length() - 2)) : version;
    }

    public Optional<Schema> getSchema(String version) {
        return Optional.ofNullable(version2Schema.get(normalize(version)));
    }
}
