package de.agiehl.bgstats.statistics;

import de.agiehl.bgstats.domain.Game;

import java.time.LocalDate;

public record GameStatistics(
        Game game,
        long plays,
        long totalMinutes,
        LocalDate firstPlay,
        LocalDate lastPlay,
        long wins,
        long uniquePlayers,
        String favoriteLocation) {
}
