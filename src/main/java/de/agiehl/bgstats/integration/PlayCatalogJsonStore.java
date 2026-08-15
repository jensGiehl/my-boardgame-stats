package de.agiehl.bgstats.integration;

import de.agiehl.bgstats.domain.PlayCatalog;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PlayCatalogJsonStore {

    private final ObjectMapper objectMapper;

    public PlayCatalogJsonStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PlayCatalog read(String fileName) {
        Path path = resolve(fileName);
        if (!Files.isRegularFile(path)) {
            throw new CatalogFileException("Die JSON-Datendatei wurde nicht gefunden: " + path);
        }
        try {
            return objectMapper.readValue(path, PlayCatalog.class);
        } catch (JacksonException exception) {
            throw new CatalogFileException("Die JSON-Datendatei konnte nicht gelesen werden: " + path, exception);
        }
    }

    public void write(String fileName, PlayCatalog catalog) {
        Path path = resolve(fileName);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path, catalog);
        } catch (IOException | JacksonException exception) {
            throw new CatalogFileException("Der JSON-Snapshot konnte nicht geschrieben werden: " + path, exception);
        }
    }

    private Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new CatalogFileException("Für die JSON-Datendatei wurde kein Pfad angegeben.");
        }
        return Path.of(fileName).toAbsolutePath().normalize();
    }
}
