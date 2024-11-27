package org.example;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.example.util.XmlUtilityService;
import org.xml.sax.SAXException;

import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Startup
@ApplicationScoped
public class XmlSchemaService {

    private static final String SCHEMA_DIRECTORY = "schemas/";
    private static final List<String> SCHEMA_FILES = List.of(
            "taf_cat_complete_sector_3.1.0.0.xsd",
            "taf_cat_complete_sector_3.5.0.0.xsd"
    );

    private final XmlUtilityService xmlUtilityService;
    private final Map<String, Schema> version2Schema = new HashMap<>();

    public XmlSchemaService(XmlUtilityService xmlUtilityService) {
        this.xmlUtilityService = xmlUtilityService;
    }

    @PostConstruct
    void init() {
        loadSchemas();
    }

    private void loadSchemas() {
        Log.info("***** Loading schemas *****");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SCHEMA_FILES.forEach(schemaFile -> loadSchema(schemaFile, classLoader));
    }

    private void loadSchema(String schemaFile, ClassLoader classLoader) {
        Log.debugf("Loading schema file %s", schemaFile);
        try (InputStream schemaStream = classLoader.getResourceAsStream(SCHEMA_DIRECTORY + schemaFile)) {
            String version = normalize(extractVersion(schemaFile));
            Schema schema = xmlUtilityService.createSchema(schemaStream);
            version2Schema.put(version, schema);
        } catch (IOException | SAXException e) {
            Log.errorf(e, "Error loading schema file %s", schemaFile);
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
