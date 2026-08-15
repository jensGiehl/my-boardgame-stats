package de.agiehl.bgstats.service;

import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.integration.PlayCatalogGateway;
import de.agiehl.bgstats.integration.PlayCatalogJsonStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class PlayCatalogService {

    private final PlayCatalogGateway gateway;
    private final PlayCatalogJsonStore jsonStore;
    private final BggProperties properties;
    private final Clock clock;
    private volatile CachedCatalog cache;

    @Autowired
    public PlayCatalogService(
            PlayCatalogGateway gateway,
            PlayCatalogJsonStore jsonStore,
            BggProperties properties) {
        this(gateway, jsonStore, properties, Clock.systemUTC());
    }

    PlayCatalogService(
            PlayCatalogGateway gateway,
            PlayCatalogJsonStore jsonStore,
            BggProperties properties,
            Clock clock) {
        this.gateway = gateway;
        this.jsonStore = jsonStore;
        this.properties = properties;
        this.clock = clock;
    }

    public PlayCatalog getCatalog() {
        validateConfiguration();
        CachedCatalog current = cache;
        Instant expiration = Instant.now(clock).minus(properties.cacheTtl());
        if (current == null || current.cachedAt().isBefore(expiration)
                || !current.source().equals(source())) {
            return reload();
        }
        return current.catalog();
    }

    public synchronized PlayCatalog reload() {
        validateConfiguration();
        PlayCatalog loadedCatalog = usesInputFile()
                ? jsonStore.read(properties.inputFile())
                : loadFromBgg();
        PlayCatalog catalog = retainPlaysWithCatalogOwner(loadedCatalog);
        cache = new CachedCatalog(catalog, Instant.now(clock), source());
        return catalog;
    }

    private PlayCatalog retainPlaysWithCatalogOwner(PlayCatalog catalog) {
        if (catalog.username() == null || catalog.username().isBlank()) {
            return catalog;
        }
        String ownerKey = "u:" + catalog.username().trim().toLowerCase(Locale.ROOT);
        var plays = catalog.plays().stream()
                .filter(play -> play.includes(ownerKey))
                .toList();
        return plays.size() == catalog.plays().size()
                ? catalog
                : new PlayCatalog(catalog.username(), catalog.loadedAt(), plays);
    }

    private void validateConfiguration() {
        if (usesInputFile()) {
            return;
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new MissingConfigurationException("Der BGG-API-Key fehlt. Bitte BGG_API_KEY setzen.");
        }
        if (properties.username() == null || properties.username().isBlank()) {
            throw new MissingConfigurationException("Der BGG-Benutzername fehlt. Bitte BGG_USERNAME setzen.");
        }
    }

    private PlayCatalog loadFromBgg() {
        PlayCatalog catalog = gateway.load(properties.username().trim());
        jsonStore.write(properties.snapshotFile(), catalog);
        return catalog;
    }

    private boolean usesInputFile() {
        return properties.inputFile() != null && !properties.inputFile().isBlank();
    }

    private String source() {
        if (usesInputFile()) {
            return "file:" + Path.of(properties.inputFile()).toAbsolutePath().normalize();
        }
        return "bgg:" + properties.username().trim().toLowerCase(Locale.ROOT);
    }

    private record CachedCatalog(PlayCatalog catalog, Instant cachedAt, String source) {
    }
}
