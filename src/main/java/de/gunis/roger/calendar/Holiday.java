package de.gunis.roger.calendar;

import java.time.LocalDate;

public class Holiday {
    LocalDate startDate;
    LocalDate endDate;
    String name;

    public Holiday(LocalDate startDate, LocalDate endDate, String name) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
    }

    public boolean isWithRange(LocalDate testDate) {
        return testDate.toEpochDay() >= startDate.toEpochDay() &&
                testDate.toEpochDay() <= endDate.toEpochDay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Holiday holiday1 = (Holiday) o;

        return startDate.equals(holiday1.startDate);
    }

    @Override
    public int hashCode() {
        return startDate.hashCode();
    }
}
