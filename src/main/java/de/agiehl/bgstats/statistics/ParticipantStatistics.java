package de.agiehl.bgstats.statistics;

import java.time.LocalDate;
import java.util.List;

public record ParticipantStatistics(
        ParticipantOption participant,
        long totalPlays,
        long uniqueGames,
        long totalMinutes,
        long wins,
        double winRate,
        long locations,
        String favoriteGame,
        LocalDate firstPlay,
        LocalDate lastPlay,
        List<CoPlayerStatistics> favoriteCoPlayers,
        List<GameStatistics> games) {
}
