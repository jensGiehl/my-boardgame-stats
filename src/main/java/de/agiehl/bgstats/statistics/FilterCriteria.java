package de.agiehl.bgstats.statistics;

import de.agiehl.bgstats.domain.PlayRecord;

public record FilterCriteria(
        Integer year,
        String category,
        String participant,
        String location,
        Integer gameId,
        Double minimumRating) {

    public FilterCriteria {
        category = normalize(category);
        participant = normalize(participant);
        location = normalize(location);
        minimumRating = minimumRating == null ? null : Math.clamp(minimumRating, 0.0, 10.0);
    }

    public boolean matches(PlayRecord play) {
        return (year == null || play.date().getYear() == year)
                && (category == null || play.game().categories().stream()
                .anyMatch(value -> value.equalsIgnoreCase(category)))
                && (participant == null || play.includes(participant))
                && (location == null || play.location().equalsIgnoreCase(location))
                && (gameId == null || play.game().id() == gameId)
                && (minimumRating == null || play.game().averageRating() != null
                && play.game().averageRating() >= minimumRating);
    }

    public boolean active() {
        return year != null || category != null || participant != null || location != null
                || gameId != null || minimumRating != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
