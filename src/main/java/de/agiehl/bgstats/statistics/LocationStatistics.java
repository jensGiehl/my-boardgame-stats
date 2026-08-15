package de.agiehl.bgstats.statistics;

import java.time.LocalDate;
import java.util.List;

public record LocationStatistics(
        String name,
        long totalPlays,
        long uniqueGames,
        long totalMinutes,
        long participants,
        long activeDays,
        String favoriteGame,
        LocalDate firstPlay,
        LocalDate lastPlay,
        List<GameStatistics> games) {
}
