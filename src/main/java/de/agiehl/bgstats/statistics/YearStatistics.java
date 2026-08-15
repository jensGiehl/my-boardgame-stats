package de.agiehl.bgstats.statistics;

import java.time.LocalDate;
import java.util.List;

public record YearStatistics(
        int year,
        long totalPlays,
        long uniqueGames,
        long totalMinutes,
        long wins,
        long activeDays,
        long locations,
        long averageMinutes,
        String favoriteGame,
        String busiestMonth,
        LocalDate firstPlay,
        LocalDate lastPlay,
        List<ActivityStatistics> monthlyActivity,
        List<GameStatistics> games) {
}
