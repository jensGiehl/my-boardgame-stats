package de.agiehl.bgstats.domain;

import java.time.LocalDate;
import java.util.List;

public record PlayRecord(
        int id,
        LocalDate date,
        int quantity,
        int lengthMinutes,
        boolean excludedFromWinStats,
        String location,
        Game game,
        List<Participant> participants) {

    public PlayRecord {
        quantity = Math.max(quantity, 1);
        lengthMinutes = Math.max(lengthMinutes, 0);
        location = location == null || location.isBlank() ? "Ohne Ortsangabe" : location.trim();
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    public long totalMinutes() {
        return (long) quantity * lengthMinutes;
    }

    public boolean includes(String participantKey) {
        return participants.stream().anyMatch(participant -> participant.key().equals(participantKey));
    }

    public long winsFor(String participantKey) {
        if (excludedFromWinStats) {
            return 0;
        }
        return participants.stream()
                .filter(participant -> participant.key().equals(participantKey) && participant.winner())
                .findFirst()
                .map(participant -> (long) quantity)
                .orElse(0L);
    }
}
