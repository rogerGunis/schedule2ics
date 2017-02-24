package de.gunis.roger.calendar;

import java.time.LocalDate;

public class Holiday {
    LocalDate holiday;
    String name;

    public Holiday(LocalDate holiday, String name) {
        this.holiday = holiday;
        this.name = name;
    }

    public boolean match(LocalDate day) {
        return day.equals(holiday);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Holiday holiday1 = (Holiday) o;

        return holiday.equals(holiday1.holiday);
    }

    @Override
    public int hashCode() {
        return holiday.hashCode();
    }
}
