package de.gunis.roger.workersAvailable;

import de.gunis.roger.calendar.ICalendarAccess;
import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.time.LocalDate;
import java.util.*;

public class Worker implements ICalendarAccess {
    private static final Logger logger = LoggerFactory.getLogger("Worker.class");
    private final net.fortuna.ical4j.model.Calendar calendar;
    String name;
    private List<Holiday> vacations;
    private Set<Job> jobs = new HashSet<>();
    private Map<Job, Boolean> hasJobDone = new HashMap<>();
    private UidGenerator ug = null;

    public Worker(String name, Set<Job> jobs, List<Holiday> vacations) {
        this.name = name;
        this.jobs = jobs;
        this.vacations = validate(vacations);

        calendar = new net.fortuna.ical4j.model.Calendar();
        calendar.getProperties().add(new ProdId("-//" + name + "//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        try {
            logger.trace("Generating UID (timeConsuming ...)");
            ug = new UidGenerator("1");
            logger.trace("Generating UID (DONE)");
        } catch (SocketException e) {
            logger.warn("Exception: " + e.getMessage());
        }
    }

    public net.fortuna.ical4j.model.Calendar getCalendar() {
        return calendar;
    }

    private List<Holiday> validate(List<Holiday> vacation) {
        return vacation == null ? Collections.emptyList() : vacation;
    }

    void doJob(Job job) {
        if (!jobs.contains(job)) {
            try {
                throw new IllegalJobException("This job cannot be done by me");
            } catch (IllegalJobException e) {
                logger.error("Exception: " + e);
            }
        }
        hasJobDone.put(job, Boolean.TRUE);
    }

    public void registerJobOnDate(LocalDate day, Dur duration, String jobName) {
        VEvent vEvent = new VEvent(new net.fortuna.ical4j.model.Date(day.toEpochDay() * 86400 * 1000), duration, jobName);
        vEvent.getProperties().add(ug.generateUid());
        this.calendar.getComponents().add(vEvent);
    }

    Boolean hasJobDone(Job job) {
        return hasJobDone.getOrDefault(job, Boolean.FALSE);
    }

    @Override
    public String toString() {
        return "Worker{" +
                "jobs=" + jobs +
                ", vacations=" + vacations +
                ", name='" + name + '\'' +
                '}';
    }

    Set<Job> getJobs() {
        return jobs;
    }

    boolean isOnHoliday(LocalDate day) {
        return vacations.stream().anyMatch(vacation -> vacation.isWithinRange(day));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Worker worker = (Worker) o;

        return name != null ? name.equals(worker.name) : worker.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    void resetJobDone(Job job) {
        hasJobDone.put(job, Boolean.FALSE);
    }

    public String getName() {
        return name;
    }
}
