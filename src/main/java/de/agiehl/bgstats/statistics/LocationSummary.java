package de.agiehl.bgstats.statistics;

import java.time.LocalDate;

public record LocationSummary(
        String name,
        long plays,
        long totalMinutes,
        long uniqueGames,
        long participants,
        LocalDate firstPlay,
        LocalDate lastPlay) {
}
