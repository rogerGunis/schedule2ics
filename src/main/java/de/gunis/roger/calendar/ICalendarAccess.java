package de.gunis.roger.calendar;

import net.fortuna.ical4j.model.Calendar;

public interface ICalendarAccess {
    Calendar getCalendar();
    String getName();
}
