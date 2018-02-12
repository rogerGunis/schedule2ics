package de.gunis.roger.jobService.calendar;

import net.fortuna.ical4j.model.Calendar;

public interface ICalendarAccess {
    Calendar getCalendar();
    String getName();
}
