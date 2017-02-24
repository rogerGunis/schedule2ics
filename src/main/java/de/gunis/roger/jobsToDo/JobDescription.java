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

    private final String name;
    private final Set<DayOfWeek> dayOfWeeks;
    private final Integer duration;
    private final Calendar calendar;

    public JobDescription(String name, Set<DayOfWeek> dayOfWeeks, Integer duration) {
        this.name = name;
        this.dayOfWeeks = dayOfWeeks;
        this.duration = duration;

        calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//" + name + "//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
    }

    public Integer getDuration() {
        return duration;
    }

    boolean hasToBeDoneOn(LocalDate day) {
        return dayOfWeeks.contains(day.getDayOfWeek());
    }

    public String getName() {
        return name;
    }

    public void registerWorkerOnDate(LocalDate day, Worker foundWorker) {

        try {
            VEvent vEvent = new VEvent(new Date(day.toEpochDay() * 86400 * 1000), foundWorker.getName());
            UidGenerator ug = new UidGenerator("1");
            vEvent.getProperties().add(ug.generateUid());

            calendar.getComponents().add(vEvent);
        } catch (SocketException e) {
            logger.warn("Exception: " + e);
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }
}
