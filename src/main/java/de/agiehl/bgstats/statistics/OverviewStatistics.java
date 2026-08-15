package de.agiehl.bgstats.statistics;

import java.time.LocalDate;
import java.util.List;

public record OverviewStatistics(
        List<GameStatistics> games,
        long totalPlays,
        long totalMinutes,
        long totalWins,
        LocalDate firstPlay,
        LocalDate lastPlay,
        int threshold) {
}
