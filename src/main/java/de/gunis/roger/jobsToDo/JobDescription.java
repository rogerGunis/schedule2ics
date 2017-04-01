package de.gunis.roger.jobsToDo;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.calendar.ICalendarAccess;
import de.gunis.roger.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public class JobDescription implements ICalendarAccess {
    private static final Logger logger = LoggerFactory.getLogger("JobDescription.class");

    private final String name;
    private final Set<DayOfWeek> daysInWeek;
    private final DayOfWeek manuallySetDay;
    private final Integer duration;
    private final Calendar calendar;
    private final LocalDate begin;
    private final LocalDate end;
    private final Dur jobDuration;
    private UidGenerator ug = null;

    public JobDescription(String name, Set<DayOfWeek> daysInWeek, Integer duration, LocalDate begin, LocalDate end, DayOfWeek manuallySetDay) {
        this.name = name;
        this.daysInWeek = daysInWeek;
        this.duration = duration;
        this.jobDuration = new Dur(duration, 0, 0, 0);
        this.begin = begin;
        this.end = end;

        this.manuallySetDay = manuallySetDay;

        calendar = new Calendar();
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

    public JobDescription(String name, Set<DayOfWeek> daysInWeek, Integer duration, LocalDate begin, LocalDate end) {
        this(name, daysInWeek, duration, begin, end, null);
    }

    private static LocalDate getManuallyPlacedCalendarDay(LocalDate day, DayOfWeek manuallySetDayCheckSatOrSun) {
        LocalDate manuallySetDay = day.with(manuallySetDayCheckSatOrSun);
        return manuallySetDayCheckSatOrSun == DayOfWeek.SATURDAY ? manuallySetDay : manuallySetDay.minusDays(7);
    }

    public LocalDate getBegin() {
        return begin;
    }

    public LocalDate getEnd() {
        return end;
    }

    boolean hasToBeDoneOnNormalDay(LocalDate testDate) {

        return isWithinRange(testDate);
    }

    boolean hasToBeDoneOnHoliday(LocalDate testDate, Holiday holiday) {
        LocalDate endDate = testDate.plusDays(duration);

        boolean durationIsWithinHolidays = holiday.isWithinRange(testDate) && holiday.isWithinRange(endDate);
        boolean doIfWorkIsOnlyForThisDay = duration != 1;
        return daysInWeek.contains(testDate.getDayOfWeek()) && doIfWorkIsOnlyForThisDay && !durationIsWithinHolidays;
    }

    private boolean isWithinRange(LocalDate testDate) {
        return daysInWeek.contains(testDate.getDayOfWeek()) &&
                testDate.toEpochDay() >= begin.toEpochDay() &&
                testDate.toEpochDay() <= end.toEpochDay();
    }

    public String getName() {
        return name;
    }

    public VEvent registerWorkerOnDate(LocalDate day, Worker foundWorker, JobDescription jobDescription) {
        VEvent vEvent;
        LocalDate placedCalendarDay;
        Dur duration1Day = new Dur(1, 0, 0, 0);
        if (manuallySetDay != null) {
            placedCalendarDay = getManuallyPlacedCalendarDay(day, manuallySetDay);
        } else {
            placedCalendarDay = day;
        }
        vEvent = getNewEvent(placedCalendarDay, (manuallySetDay != null ? duration1Day : jobDuration), foundWorker, jobDescription);

        logger.trace("registerWorkerOnDate start");
        Categories categories = new Categories(name + "," + foundWorker.getName());
        vEvent.getProperties().add(categories);

        askWorkerForJobProposalAndSubscribe(foundWorker, jobDescription, vEvent);

        calendar.getComponents().add(vEvent);

        foundWorker.registerJobOnDate(day, jobDuration, name);
        logger.trace("registerWorkerOnDate done");

        return vEvent;
    }

    private void askWorkerForJobProposalAndSubscribe(Worker foundWorker, JobDescription jobDescription, VEvent vEvent) {
        String jobProposal = foundWorker.askForProposal(jobDescription);

        if (jobProposal != null) {
            Description calDescription = new Description();
            calDescription.setValue(jobProposal);
            vEvent.getProperties().add(calDescription);

        }
    }

    private VEvent getNewEvent(LocalDate day, Dur duration, Worker foundWorker, JobDescription jobDescription) {
        logger.trace("getNewEvent");
        VEvent vEvent = new VEvent(new Date(day.toEpochDay() * 86400 * 1000), duration, foundWorker.getName());
        vEvent.getProperties().add(ug.generateUid());
        askWorkerForJobProposalAndSubscribe(foundWorker, jobDescription, vEvent);
        logger.trace("getNewEvent done");
        return vEvent;
    }

    @Override
    public Calendar getCalendar() {
        return calendar;
    }

    public DayOfWeek getManuallySetDay() {
        return manuallySetDay;
    }
}
