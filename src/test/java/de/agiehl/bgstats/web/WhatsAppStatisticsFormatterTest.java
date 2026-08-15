package de.agiehl.bgstats.web;

import de.agiehl.bgstats.domain.Game;
import de.agiehl.bgstats.statistics.CustomStatistics;
import de.agiehl.bgstats.statistics.FilterCriteria;
import de.agiehl.bgstats.statistics.GameStatistics;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppStatisticsFormatterTest {

    private final WhatsAppStatisticsFormatter formatter = new WhatsAppStatisticsFormatter(new HumanDurationFormatter());

    @Test
    void formatsStatisticsForWhatsApp() {
        CustomStatistics statistics = new CustomStatistics(
                new FilterCriteria(null, null, null, null, null, null),
                9,
                2,
                660,
                5,
                55.6,
                2,
                73,
                7.4,
                "Catan",
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 3, 12),
                List.of(
                        gameStatistics("Catan", 9, 660, 2, 360, 330,
                                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 3, 12)),
                        gameStatistics("Azul", 3, 240, 1, 240, 240,
                                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 2))),
                List.of(),
                List.of());

        assertThat(formatter.format(statistics)).isEqualTo("""
                *Catan*

                🎲 *Partien:* 9
                📅 *Verschiedene Spieltage:* 2
                ⏱️ *Spielzeit gesamt:* 11 Stunden
                🔥 *Max. Spielzeit an einem Tag:* 6 Stunden
                📊 *Ø Spielzeit pro Tag:* 5 Stunden und 30 Minuten
                🗓️ *Erste Partie:* 10.01.2025
                🏁 *Letzte Partie:* 12.03.2025
                ⌛ *Zeit dazwischen:* 2 Monate und 2 Tage

                *Azul*

                🎲 *Partien:* 3
                📅 *Verschiedene Spieltage:* 1
                ⏱️ *Spielzeit gesamt:* 4 Stunden
                🔥 *Max. Spielzeit an einem Tag:* 4 Stunden
                📊 *Ø Spielzeit pro Tag:* 4 Stunden
                🗓️ *Erste Partie:* 02.02.2026
                🏁 *Letzte Partie:* 02.02.2026
                ⌛ *Zeit dazwischen:* am selben Tag""");
    }

    private GameStatistics gameStatistics(
            String name,
            long plays,
            long totalMinutes,
            long activeDays,
            long maximumMinutesPerDay,
            long averageMinutesPerDay,
            LocalDate firstPlay,
            LocalDate lastPlay) {
        Game game = new Game(
                name.hashCode(),
                name,
                "",
                2020,
                1,
                4,
                60,
                8,
                7.5,
                2.5,
                100,
                List.of(),
                List.of());
        return new GameStatistics(
                game,
                plays,
                totalMinutes,
                activeDays,
                maximumMinutesPerDay,
                averageMinutesPerDay,
                firstPlay,
                lastPlay,
                0,
                1,
                "Zuhause");
    }
}
