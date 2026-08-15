package de.agiehl.bgstats.statistics;

import java.time.LocalDate;
import java.util.List;

public record CustomStatistics(
        FilterCriteria filters,
        long totalPlays,
        long uniqueGames,
        long totalMinutes,
        long wins,
        double winRate,
        long activeDays,
        long averageMinutes,
        Double averageRating,
        String favoriteGame,
        LocalDate firstPlay,
        LocalDate lastPlay,
        List<GameStatistics> games,
        List<CategoryStatistics> categories,
        List<ActivityStatistics> activity) {
}
