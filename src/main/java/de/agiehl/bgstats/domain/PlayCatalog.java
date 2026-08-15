package de.agiehl.bgstats.domain;

import java.time.Instant;
import java.util.List;

public record PlayCatalog(String username, Instant loadedAt, List<PlayRecord> plays) {

    public PlayCatalog {
        plays = List.copyOf(plays);
    }
}
