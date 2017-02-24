package de.gunis.roger.jobsToDo;

import de.gunis.roger.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public class JobDescription {
    private static final Logger logger = LoggerFactory.getLogger("JobDescription.class");

    String name = "";
    Set<DayOfWeek> dayOfWeeks;
    Integer duration = 0;
    Calendar calendar = new Calendar();

    public JobDescription(String name, Set<DayOfWeek> dayOfWeeks, Integer duration) {
        this.name = name;
        this.dayOfWeeks = dayOfWeeks;
        this.duration = duration;
    }

    public boolean hasToBeDoneOn(LocalDate day) {
        return dayOfWeeks.contains(day.getDayOfWeek().getValue());
    }

    public String getName() {
        return name;
    }

    public void registerEvent(LocalDate day, Worker foundWorker) {

        try {
            VEvent vEvent = new VEvent(new Date(day.toEpochDay()), foundWorker.getName());
            UidGenerator ug = new UidGenerator("1");
            vEvent.getProperties().add(ug.generateUid());

            calendar.getProperties().add(new ProdId("-//FischstäbchenMuc//iCal4j 1.0//EN"));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);

            calendar.getComponents().add(vEvent);
        } catch (SocketException e) {
            logger.warn("Exception: " + e);
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }
}
