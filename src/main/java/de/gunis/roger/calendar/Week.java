package de.gunis.roger.calendar;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;

public class Week {

    public static TemporalAdjuster nextWorkingDayAdjuster = TemporalAdjusters.ofDateAdjuster(localDate -> {
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY) {
            return localDate.plusDays(3);
        } else if (dayOfWeek == DayOfWeek.SATURDAY) {
            return localDate.plusDays(2);
        }
        return localDate.plusDays(1);
    });


    public static boolean isWorkday(LocalDateTime localDate) {
        int dow = localDate.getDayOfWeek().getValue();
        return ((dow >= Calendar.MONDAY) && (dow <= Calendar.FRIDAY));
    }

}
