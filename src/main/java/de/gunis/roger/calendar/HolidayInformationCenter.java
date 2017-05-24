package de.gunis.roger.calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HolidayInformationCenter {
    private static final Logger logger = LoggerFactory.getLogger("HolidayInformationCenter.class");
    private static HolidayInformationCenter instance;
    private Set<Holiday> holidays = new HashSet<>();


    public HolidayInformationCenter() {
    }

    public static synchronized HolidayInformationCenter open() {
        if (instance != null) {
            throw new RuntimeException(HolidayInformationCenter.class.getSimpleName() + " is already open, terminating");
        }

        instance = new HolidayInformationCenter();
        logger.info("Instance started");

        return instance;
    }

    public static synchronized void close() {
        instance = null;
        logger.info("HolidayInformationCenter closed");
    }

    public static HolidayInformationCenter instance() {
        if (instance == null) {
            throw new RuntimeException("HolidayInformationCenter is closed");
        }
        return instance;
    }

    public Set<Holiday> getHolidays() {
        return holidays;
    }

    public void setHolidays(Set<Holiday> holidays) {
        this.holidays = holidays;
    }
}
