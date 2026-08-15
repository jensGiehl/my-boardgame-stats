package de.agiehl.bgstats.web;

import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.statistics.CustomStatistics;
import de.agiehl.bgstats.statistics.FilterCriteria;
import de.agiehl.bgstats.statistics.GameStatistics;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoardGameGeekStatisticsFormatterTest {

    private final BoardGameGeekStatisticsFormatter formatter =
            new BoardGameGeekStatisticsFormatter(new HumanDurationFormatter());

    @Test
    void formatsStatisticsWithBoardGameGeekMarkupAndWithoutEmojis() {
        Game game = new Game(
                13,
                "Catan",
                "",
                1995,
                3,
                4,
                90,
                10,
                7.1,
                2.3,
                600,
                List.of(),
                List.of());
        GameStatistics gameStatistics = new GameStatistics(
                game,
                9,
                660,
                2,
                360,
                330,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 3, 12),
                5,
                2,
                "Zuhause");
        CustomStatistics statistics = new CustomStatistics(
                new FilterCriteria(null, null, null, null, null, null),
                9,
                1,
                660,
                5,
                55.6,
                2,
                73,
                7.1,
                "Catan",
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 3, 12),
                List.of(gameStatistics),
                List.of(),
                List.of());

        assertThat(formatter.format(statistics)).isEqualTo("""
                [b][thing=13]Catan[/thing][/b]

                [b]Partien:[/b] 9
                [b]Verschiedene Spieltage:[/b] 2
                [b]Spielzeit gesamt:[/b] 11 Stunden
                [b]Max. Spielzeit an einem Tag:[/b] 6 Stunden
                [b]Ø Spielzeit pro Tag:[/b] 5 Stunden und 30 Minuten
                [b]Erste Partie:[/b] 10.01.2025
                [b]Letzte Partie:[/b] 12.03.2025
                [b]Zeit dazwischen:[/b] 2 Monate und 2 Tage""");
    }
}
