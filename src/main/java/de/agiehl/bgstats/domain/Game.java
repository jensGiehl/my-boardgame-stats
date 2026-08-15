package de.agiehl.bgstats.domain;

import java.util.List;

public record Game(
        int id,
        String name,
        String coverUrl,
        Integer yearPublished,
        Integer minimumPlayers,
        Integer maximumPlayers,
        Integer playingTimeMinutes,
        Integer minimumAge,
        Double averageRating,
        Double complexity,
        Integer boardGameRank,
        List<String> categories,
        List<String> mechanics) {

    public Game {
        categories = categories == null ? List.of() : List.copyOf(categories);
        mechanics = mechanics == null ? List.of() : List.copyOf(mechanics);
    }

    public Game(int id, String name, String coverUrl) {
        this(id, name, coverUrl, null, null, null, null, null, null, null, null, List.of(), List.of());
    }
}
