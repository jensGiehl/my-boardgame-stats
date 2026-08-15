package de.agiehl.bgstats.statistics;

import de.agiehl.bgstats.domain.Game;

import java.util.List;

public record FilterOptions(
        List<Integer> years,
        List<String> categories,
        List<ParticipantOption> participants,
        List<String> locations,
        List<Game> games) {
}
