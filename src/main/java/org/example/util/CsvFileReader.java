package org.example.util;

import com.opencsv.bean.CsvToBeanBuilder;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class CsvFileReader {

    public static <T> List<T> readFile(String filename, Class<T> type) {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                Log.warnf("Could not find resource %s", filename);
                return Collections.emptyList();
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return new CsvToBeanBuilder<T>(reader)
                        .withType(type)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse();
            }
        } catch (IOException e) {
            Log.warnf("Failed to parse %s: %s", filename, e.getMessage());
            Log.debug(e);
            return Collections.emptyList();
        }
    }
}
