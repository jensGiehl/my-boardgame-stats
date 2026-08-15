package de.agiehl.bgstats.service;

import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import de.agiehl.bgstats.integration.PlayCatalogGateway;
import de.agiehl.bgstats.integration.PlayCatalogJsonStore;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlayCatalogServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-14T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void loadsConfiguredJsonWithoutApiCallsOrApiConfiguration() {
        PlayCatalogGateway gateway = mock(PlayCatalogGateway.class);
        PlayCatalogJsonStore jsonStore = mock(PlayCatalogJsonStore.class);
        PlayCatalog catalog = catalog();
        when(jsonStore.read("development-data.json")).thenReturn(catalog);
        PlayCatalogService service = new PlayCatalogService(
                gateway,
                jsonStore,
                properties("", "", "development-data.json", "unused.json"),
                CLOCK);

        assertThat(service.getCatalog()).isSameAs(catalog);

        verify(jsonStore).read("development-data.json");
        verify(jsonStore, never()).write(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(gateway);
    }

    @Test
    void writesSnapshotAfterLoadingAllApiData() {
        PlayCatalogGateway gateway = mock(PlayCatalogGateway.class);
        PlayCatalogJsonStore jsonStore = mock(PlayCatalogJsonStore.class);
        PlayCatalog catalog = catalog();
        when(gateway.load("Owner")).thenReturn(catalog);
        PlayCatalogService service = new PlayCatalogService(
                gateway,
                jsonStore,
                properties("api-key", "Owner", "", "bg-stats-data.json"),
                CLOCK);

        assertThat(service.getCatalog()).isSameAs(catalog);
        assertThat(service.getCatalog()).isSameAs(catalog);

        verify(gateway).load("Owner");
        verify(jsonStore).write("bg-stats-data.json", catalog);
    }

    @Test
    void removesPlaysWithoutCatalogOwnerFromJsonInput() {
        PlayCatalogGateway gateway = mock(PlayCatalogGateway.class);
        PlayCatalogJsonStore jsonStore = mock(PlayCatalogJsonStore.class);
        PlayCatalog source = new PlayCatalog(
                "Owner",
                Instant.parse("2025-01-01T00:00:00Z"),
                List.of(play(1, "Owner"), play(2, "SomeoneElse")));
        when(jsonStore.read("development-data.json")).thenReturn(source);
        PlayCatalogService service = new PlayCatalogService(
                gateway,
                jsonStore,
                properties("", "", "development-data.json", "unused.json"),
                CLOCK);

        assertThat(service.getCatalog().plays())
                .extracting(PlayRecord::id)
                .containsExactly(1);
    }

    private BggProperties properties(
            String apiKey,
            String username,
            String inputFile,
            String snapshotFile) {
        return new BggProperties(
                apiKey,
                username,
                inputFile,
                snapshotFile,
                8,
                Duration.ofMinutes(15),
                20,
                new BggProperties.Client(
                        URI.create("https://example.com"),
                        "apikey",
                        "test",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        0,
                        Duration.ZERO));
    }

    private PlayCatalog catalog() {
        return new PlayCatalog("Owner", Instant.parse("2025-01-01T00:00:00Z"), List.of());
    }

    private PlayRecord play(int id, String username) {
        Game game = new Game(
                13,
                "Catan",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of());
        return new PlayRecord(
                id,
                LocalDate.of(2025, 1, 1),
                1,
                60,
                false,
                "Zuhause",
                game,
                List.of(new Participant(username, username, false)));
    }
}
