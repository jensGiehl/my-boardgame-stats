package de.agiehl.bgstats.web;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HumanDurationFormatterTest {

    private final HumanDurationFormatter formatter = new HumanDurationFormatter();

    @Test
    void formatsMinutesWithGermanUnits() {
        assertThat(formatter.formatMinutes(1_744)).isEqualTo("1 Tag, 5 Stunden und 4 Minuten");
        assertThat(formatter.formatMinutes(60)).isEqualTo("1 Stunde");
        assertThat(formatter.formatMinutes(0)).isEqualTo("0 Minuten");
    }

    @Test
    void formatsCalendarSpan() {
        assertThat(formatter.formatBetween(
                LocalDate.of(2020, 1, 2),
                LocalDate.of(2023, 3, 6)))
                .isEqualTo("3 Jahre, 2 Monate und 4 Tage");
        assertThat(formatter.formatBetween(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1)))
                .isEqualTo("am selben Tag");
    }
}
