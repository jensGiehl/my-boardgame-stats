package de.agiehl.bgstats.domain;

import java.util.Locale;

public record Participant(String username, String name, boolean winner) {

    public String key() {
        if (username != null && !username.isBlank()) {
            return "u:" + username.trim().toLowerCase(Locale.ROOT);
        }
        return "n:" + displayName().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return username == null || username.isBlank() ? "Unbekannt" : username.trim();
    }
}
