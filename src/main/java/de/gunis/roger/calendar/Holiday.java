package de.gunis.roger.calendar;

import net.fortuna.ical4j.model.Dur;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.stream.IntStream;

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


    public boolean isHolidayInCompleteWorkingTime(LocalDate testDate, Integer duration) {
        return IntStream.of(0,duration).allMatch(days -> isWithinRange(testDate.plusDays(days)));
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

    @Override
    public String toString() {
        return "Holiday{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", name='" + name + '\'' +
                '}';
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Dur getDuration() {
        return new Dur(Math.toIntExact(endDate.toEpochDay() - startDate.toEpochDay()), 0, 0, 0);
    }

    public String getName() {
        return name;
    }
}
