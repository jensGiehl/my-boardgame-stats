package de.agiehl.bgstats.integration;

import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayCatalogJsonStoreTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void writesAndReadsCompleteCatalog() {
        PlayCatalogJsonStore store = new PlayCatalogJsonStore(JsonMapper.builder().findAndAddModules().build());
        Path snapshot = temporaryDirectory.resolve("catalog.json");
        PlayCatalog catalog = catalog();

        store.write(snapshot.toString(), catalog);

        assertThat(store.read(snapshot.toString())).isEqualTo(catalog);
        assertThat(snapshot).content().contains("\"username\" : \"Owner\"");
    }

    @Test
    void readsSnapshotsCreatedBeforeGameDetailsWereAdded() throws IOException {
        PlayCatalogJsonStore store = new PlayCatalogJsonStore(JsonMapper.builder().findAndAddModules().build());
        Path snapshot = temporaryDirectory.resolve("old-catalog.json");
        Files.writeString(snapshot, """
                {
                  "username": "Owner",
                  "loadedAt": "2026-08-13T10:00:00Z",
                  "plays": [{
                    "id": 1,
                    "date": "2026-05-03",
                    "quantity": 1,
                    "lengthMinutes": 75,
                    "excludedFromWinStats": false,
                    "location": "Zuhause",
                    "game": {"id": 13, "name": "Catan", "coverUrl": null},
                    "participants": []
                  }]
                }
                """);

        Game game = store.read(snapshot.toString()).plays().getFirst().game();

        assertThat(game.averageRating()).isNull();
        assertThat(game.categories()).isEmpty();
        assertThat(game.mechanics()).isEmpty();
    }

    private PlayCatalog catalog() {
        Participant participant = new Participant("Owner", "Alex", true);
        Game game = new Game(
                13,
                "Catan",
                "https://example.com/catan.jpg",
                1995,
                3,
                4,
                90,
                10,
                7.1,
                2.3,
                600,
                List.of("Strategy"),
                List.of("Trading"));
        PlayRecord play = new PlayRecord(
                1,
                LocalDate.of(2026, 5, 3),
                2,
                75,
                false,
                "Zuhause",
                game,
                List.of(participant));
        return new PlayCatalog("Owner", Instant.parse("2026-08-13T10:00:00Z"), List.of(play));
    }
}
