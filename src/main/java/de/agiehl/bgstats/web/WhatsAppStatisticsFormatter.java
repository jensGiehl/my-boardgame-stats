package de.agiehl.bgstats.web;

import de.agiehl.bgstats.statistics.CustomStatistics;
import de.agiehl.bgstats.statistics.GameStatistics;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

@Component("whatsAppStatistics")
public class WhatsAppStatisticsFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    private final HumanDurationFormatter durationFormatter;

    public WhatsAppStatisticsFormatter(HumanDurationFormatter durationFormatter) {
        this.durationFormatter = durationFormatter;
    }

    public String format(CustomStatistics statistics) {
        return statistics.games().stream()
                .map(this::formatGame)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatGame(GameStatistics statistics) {
        return """
                *%s*

                🎲 *Partien:* %d
                📅 *Verschiedene Spieltage:* %d
                ⏱️ *Spielzeit gesamt:* %s
                🔥 *Max. Spielzeit an einem Tag:* %s
                📊 *Ø Spielzeit pro Tag:* %s
                🗓️ *Erste Partie:* %s
                🏁 *Letzte Partie:* %s
                ⌛ *Zeit dazwischen:* %s
                """.formatted(
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
