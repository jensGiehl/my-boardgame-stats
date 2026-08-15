package de.agiehl.bgstats.web;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Component("humanDuration")
public class HumanDurationFormatter {

    public String formatMinutes(long minutes) {
        long normalized = Math.max(minutes, 0);
        long days = normalized / (24 * 60);
        long hours = normalized % (24 * 60) / 60;
        long remainingMinutes = normalized % 60;
        List<String> parts = new ArrayList<>();
        addPart(parts, days, "Tag", "Tage");
        addPart(parts, hours, "Stunde", "Stunden");
        addPart(parts, remainingMinutes, "Minute", "Minuten");
        return join(parts.isEmpty() ? List.of("0 Minuten") : parts);
    }

    public String formatBetween(LocalDate first, LocalDate last) {
        if (first == null || last == null) {
            return "–";
        }
        Period period = Period.between(first, last);
        List<String> parts = new ArrayList<>();
        addPart(parts, period.getYears(), "Jahr", "Jahre");
        addPart(parts, period.getMonths(), "Monat", "Monate");
        addPart(parts, period.getDays(), "Tag", "Tage");
        return parts.isEmpty() ? "am selben Tag" : join(parts);
    }

    private void addPart(List<String> parts, long value, String singular, String plural) {
        if (value > 0) {
            parts.add(value + " " + (value == 1 ? singular : plural));
        }
    }

    private String join(List<String> parts) {
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1))
                + " und " + parts.getLast();
    }
}
