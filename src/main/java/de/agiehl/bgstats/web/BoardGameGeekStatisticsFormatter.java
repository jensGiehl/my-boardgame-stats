package de.agiehl.bgstats.web;

import de.agiehl.bgstats.statistics.CustomStatistics;
import de.agiehl.bgstats.statistics.GameStatistics;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

@Component("boardGameGeekStatistics")
public class BoardGameGeekStatisticsFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    private final HumanDurationFormatter durationFormatter;

    public BoardGameGeekStatisticsFormatter(HumanDurationFormatter durationFormatter) {
        this.durationFormatter = durationFormatter;
    }

    public String format(CustomStatistics statistics) {
        return statistics.games().stream()
                .map(this::formatGame)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatGame(GameStatistics statistics) {
        return """
                [b][thing=%d]%s[/thing][/b]

                [b]Partien:[/b] %d
                [b]Verschiedene Spieltage:[/b] %d
                [b]Spielzeit gesamt:[/b] %s
                [b]Max. Spielzeit an einem Tag:[/b] %s
                [b]Ø Spielzeit pro Tag:[/b] %s
                [b]Erste Partie:[/b] %s
                [b]Letzte Partie:[/b] %s
                [b]Zeit dazwischen:[/b] %s
                """.formatted(
                statistics.game().id(),
                statistics.game().name(),
                statistics.plays(),
                statistics.activeDays(),
                durationFormatter.formatMinutes(statistics.totalMinutes()),
                durationFormatter.formatMinutes(statistics.maximumMinutesPerDay()),
                durationFormatter.formatMinutes(statistics.averageMinutesPerDay()),
                DATE_FORMATTER.format(statistics.firstPlay()),
                DATE_FORMATTER.format(statistics.lastPlay()),
                durationFormatter.formatBetween(statistics.firstPlay(), statistics.lastPlay())).stripTrailing();
    }
}
