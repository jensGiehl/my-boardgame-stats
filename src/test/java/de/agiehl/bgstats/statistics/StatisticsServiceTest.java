package de.agiehl.bgstats.statistics;

import de.agiehl.bgstats.config.BggProperties;
import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.domain.Participant;
import de.agiehl.bgstats.domain.PlayCatalog;
import de.agiehl.bgstats.domain.PlayRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsServiceTest {

    private StatisticsService service;
    private PlayCatalog catalog;

    @BeforeEach
    void setUp() {
        BggProperties properties = new BggProperties(
                "key",
                "Owner",
                "",
                "bg-stats-data.json",
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
        service = new StatisticsService(properties);

        Game catan = game(13, "Catan", 7.4, List.of("Strategy", "Negotiation"));
        Game azul = game(230802, "Azul", 7.8, List.of("Abstract Strategy"));
        Participant ownerWinner = new Participant("Owner", "Alex", true);
        Participant ownerLoser = new Participant("Owner", "Alex", false);
        Participant sam = new Participant("Sam", "Sam", false);
        catalog = new PlayCatalog("Owner", Instant.parse("2026-08-13T10:00:00Z"), List.of(
                new PlayRecord(1, LocalDate.of(2025, 1, 10), 5, 60, false, "Zuhause", catan, List.of(ownerWinner, sam)),
                new PlayRecord(2, LocalDate.of(2025, 3, 12), 4, 90, false, "Café", catan, List.of(ownerLoser, sam)),
                new PlayRecord(3, LocalDate.of(2026, 2, 2), 8, 30, false, "Zuhause", azul, List.of(ownerWinner))));
    }

    @Test
    void overviewUsesStrictThresholdAndSummarizesAllPlays() {
        OverviewStatistics overview = service.overview(catalog);

        assertThat(overview.games()).extracting(game -> game.game().name()).containsExactly("Catan");
        assertThat(overview.totalPlays()).isEqualTo(17);
        assertThat(overview.totalMinutes()).isEqualTo(900);
        assertThat(overview.totalWins()).isEqualTo(13);
    }

    @Test
    void yearProvidesUsefulAggregates() {
        YearStatistics year = service.year(catalog, 2025);

        assertThat(year.totalPlays()).isEqualTo(9);
        assertThat(year.uniqueGames()).isEqualTo(1);
        assertThat(year.totalMinutes()).isEqualTo(660);
        assertThat(year.wins()).isEqualTo(5);
        assertThat(year.activeDays()).isEqualTo(2);
        assertThat(year.locations()).isEqualTo(2);
        assertThat(year.favoriteGame()).isEqualTo("Catan");
        assertThat(year.busiestMonth()).isEqualTo("Januar");
    }

    @Test
    void participantSearchAndStatisticsUseRecordedPeople() {
        List<ParticipantOption> matches = service.participants(catalog, "sam");
        ParticipantStatistics statistics = service.participant(catalog, matches.getFirst().key());

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().plays()).isEqualTo(9);
        assertThat(statistics.totalMinutes()).isEqualTo(660);
        assertThat(statistics.favoriteCoPlayers()).extracting(CoPlayerStatistics::displayName).containsExactly("Alex");
    }

    @Test
    void locationRankingUsesTimeAndBuildsDetails() {
        List<LocationSummary> locations = service.locations(catalog);
        LocationStatistics home = service.location(catalog, "Zuhause");

        assertThat(locations).extracting(LocationSummary::name).containsExactly("Zuhause", "Café");
        assertThat(home.totalPlays()).isEqualTo(13);
        assertThat(home.uniqueGames()).isEqualTo(2);
        assertThat(home.totalMinutes()).isEqualTo(540);
    }

    @Test
    void customStatisticsCombineGameDetailsAndPlayFilters() {
        FilterCriteria filters = new FilterCriteria(2025, "Strategy", "u:sam", "Café", 13, 7.0);

        CustomStatistics statistics = service.custom(catalog, filters);

        assertThat(statistics.totalPlays()).isEqualTo(4);
        assertThat(statistics.totalMinutes()).isEqualTo(360);
        assertThat(statistics.games().getFirst().activeDays()).isEqualTo(1);
        assertThat(statistics.games().getFirst().maximumMinutesPerDay()).isEqualTo(360);
        assertThat(statistics.games().getFirst().averageMinutesPerDay()).isEqualTo(360);
        assertThat(statistics.games()).extracting(game -> game.game().name()).containsExactly("Catan");
        assertThat(statistics.categories()).extracting(CategoryStatistics::name)
                .containsExactly("Negotiation", "Strategy");
        assertThat(statistics.activity()).filteredOn(month -> month.label().equals("März"))
                .extracting(ActivityStatistics::plays).containsExactly(4L);
    }

    @Test
    void customStatisticsCalculateMaximumAndAverageTimePerDay() {
        CustomStatistics statistics = service.custom(catalog, new FilterCriteria(null, null, null, null, null, null));

        assertThat(statistics.games()).satisfiesExactly(
                game -> {
                    assertThat(game.game().name()).isEqualTo("Catan");
                    assertThat(game.activeDays()).isEqualTo(2);
                    assertThat(game.maximumMinutesPerDay()).isEqualTo(360);
                    assertThat(game.averageMinutesPerDay()).isEqualTo(330);
                },
                game -> {
                    assertThat(game.game().name()).isEqualTo("Azul");
                    assertThat(game.activeDays()).isEqualTo(1);
                    assertThat(game.maximumMinutesPerDay()).isEqualTo(240);
                    assertThat(game.averageMinutesPerDay()).isEqualTo(240);
                });
    }

    @Test
    void filterOptionsExposeDetailsAndExcludeCatalogOwnerFromCoPlayers() {
        FilterOptions options = service.filterOptions(catalog);

        assertThat(options.categories()).containsExactly("Abstract Strategy", "Negotiation", "Strategy");
        assertThat(options.participants()).extracting(ParticipantOption::displayName).containsExactly("Sam");
        assertThat(options.games()).extracting(Game::name).containsExactly("Azul", "Catan");
    }

    private Game game(int id, String name, double rating, List<String> categories) {
        return new Game(
                id,
                name,
                "https://example.com/" + id + ".jpg",
                2020,
                2,
                4,
                60,
                10,
                rating,
                2.5,
                100,
                categories,
                List.of("Hand Management"));
    }
}
