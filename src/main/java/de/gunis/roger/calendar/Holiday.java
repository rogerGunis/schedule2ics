package de.gunis.roger.calendar;

import java.time.LocalDate;

public class Holiday {
    private LocalDate startDate;
    private LocalDate endDate;
    private String name;

    public Holiday(LocalDate startDate, LocalDate endDate, String name) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
    }

    public boolean isWithinRange(LocalDate testDate) {
        return testDate.toEpochDay() >= startDate.toEpochDay() &&
                testDate.toEpochDay() <= endDate.toEpochDay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Holiday holiday = (Holiday) o;

        if (startDate != null ? !startDate.equals(holiday.startDate) : holiday.startDate != null) return false;
        if (endDate != null ? !endDate.equals(holiday.endDate) : holiday.endDate != null) return false;
        return name != null ? name.equals(holiday.name) : holiday.name == null;
    }

    @Override
    public int hashCode() {
        int result = startDate != null ? startDate.hashCode() : 0;
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
