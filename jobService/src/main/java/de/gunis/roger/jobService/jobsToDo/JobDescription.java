package de.gunis.roger.jobService.jobsToDo;

import de.gunis.roger.jobService.ClearingHouse;
import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.calendar.ICalendarAccess;
import de.gunis.roger.jobService.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class JobDescription implements ICalendarAccess {
    private static final Logger logger = LoggerFactory.getLogger("JobDescription.class");
    private final String name;
    private final Set<DayOfWeek> daysInWeek;
    private final Set<DayOfWeek> daysInWeekTotal;
    private final DayOfWeek manuallySetDay;
    private final Integer duration;
    private final Calendar calendar;
    private final LocalDate begin;
    private final LocalDate end;
    private final Dur jobDuration;
    private final Boolean reminder;

    public JobDescription(String name, Set<DayOfWeek> daysInWeek, Integer duration, LocalDate begin, LocalDate end, DayOfWeek manuallySetDay, Boolean reminder) {
        this.name = name;
        this.daysInWeek = new HashSet<>(daysInWeek);
        this.daysInWeekTotal = daysInWeek;
        this.duration = duration;
        this.jobDuration = new Dur(duration, 0, 0, 0);
        this.begin = begin;
        this.end = end;
        this.reminder = reminder;

        if (begin.toEpochDay() > end.toEpochDay()) {
            throw new IllegalArgumentException(this.toString() + ", begin day is before end date");
        }

        addDaysInWeekTotalOnJobWithBiggerDuration();

        this.manuallySetDay = manuallySetDay;

        calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//" + name + "//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

    }

    public JobDescription(String name, Set<DayOfWeek> daysInWeek, Integer duration, LocalDate begin, LocalDate end, Boolean reminder) {
        this(name, daysInWeek, duration, begin, end, null, reminder);
    }

    private static LocalDate getManuallyPlacedCalendarDay(LocalDate day, DayOfWeek manuallySetDayCheckSatOrSun) {
        LocalDate manuallySetDay = day.with(manuallySetDayCheckSatOrSun);
        return manuallySetDayCheckSatOrSun == DayOfWeek.SATURDAY ? manuallySetDay : manuallySetDay.minusDays(7);
    }

    public Set<DayOfWeek> getDaysInWeekTotal() {
        return daysInWeekTotal;
    }

    private void addDaysInWeekTotalOnJobWithBiggerDuration() {
        if (this.duration > 1 && daysInWeekTotal.size() == 1) {
            DayOfWeek startDay = daysInWeekTotal.iterator().next();
            Stream.iterate(startDay.getValue(), dayOfWeek -> dayOfWeek + 1)
                    .limit(this.duration)
                    .filter(i -> i > 0 && i < 7)
                    .forEach(i -> daysInWeekTotal.add(DayOfWeek.of(i)));
        } else if (this.duration > 1 && daysInWeekTotal.size() != 1) {
            throw new IllegalArgumentException(this.toString() + ",Currently it is only supported to have amount of 'startDayOfWeek' equals 1");
        }
    }

    public LocalDate getBegin() {
        return begin;
    }

    public LocalDate getEnd() {
        return end;
    }

    boolean hasToBeDoneOnHoliday(LocalDate testDate) {
        Set<Holiday> holidays = HolidayInformationCenter.instance().getHolidays();

        boolean jobIsTotalInHolidays = Stream.iterate(testDate, date -> date.plusDays(1))
                .limit(duration)
                .filter(day -> daysInWeekTotal.contains(day.getDayOfWeek()))
                .allMatch(day -> holidays.stream().anyMatch(holiday -> holiday.isWithinRange(day))
                );

        return !jobIsTotalInHolidays;
    }

    boolean isWithinRange(LocalDate testDate) {
        return daysInWeek.contains(testDate.getDayOfWeek()) &&
                testDate.toEpochDay() >= begin.toEpochDay() &&
                testDate.toEpochDay() <= end.toEpochDay();
    }

    public String getName() {
        return name;
    }

    public VEvent registerWorkerOnDate(LocalDate day, Worker foundWorker) {
        VEvent vEvent;
        LocalDate placedCalendarDay;
        Dur duration1Day = new Dur(1, 0, 0, 0);
        if (manuallySetDay != null) {
            placedCalendarDay = getManuallyPlacedCalendarDay(day, manuallySetDay);
        } else {
            placedCalendarDay = day;
        }
        vEvent = getNewEvent(placedCalendarDay, (manuallySetDay != null ? duration1Day : jobDuration), foundWorker);

        logger.trace("registerWorkerOnDate start");
        Categories categories = new Categories(name + "," + foundWorker.getName());
        vEvent.getProperties().add(categories);

        askWorkerForJobProposalAndSubscribe(foundWorker, vEvent);

        calendar.getComponents().add(vEvent);

        foundWorker.registerJobOnDate(day, jobDuration, this);
        logger.trace("registerWorkerOnDate done");

        return vEvent;
    }

    private void askWorkerForJobProposalAndSubscribe(Worker foundWorker, VEvent vEvent) {
        String jobProposal = foundWorker.askForProposal(this);

        if (jobProposal != null) {
            Description calDescription = new Description();
            calDescription.setValue(jobProposal);
            vEvent.getProperties().add(calDescription);
        }
    }

    private VEvent getNewEvent(LocalDate day, Dur duration, Worker foundWorker) {
        logger.trace("getNewEvent");
        long jobDate = day.toEpochDay() * 86400 * 1000;
        String foundWorkerName = foundWorker.getName();
        VEvent vEvent = new VEvent(new Date(jobDate), duration, foundWorkerName);
        vEvent.getProperties().add(new Uid(String.valueOf(day.format(ClearingHouse.dateTimeFormatter)) + "_" + foundWorkerName));

        askWorkerForJobProposalAndSubscribe(foundWorker, vEvent);
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

    public Boolean hasReminder() {
        return reminder;
    }

    @Override
    public String toString() {
        return "JobDescription{" +
                "name='" + name + '\'' +
                ", daysInWeek=" + daysInWeek +
                ", daysInWeekTotal=" + daysInWeekTotal +
                ", manuallySetDay=" + manuallySetDay +
                ", duration=" + duration +
                ", calendar=" + calendar +
                ", begin=" + begin +
                ", end=" + end +
                ", jobDuration=" + jobDuration +
                ", reminder=" + reminder +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobDescription that = (JobDescription) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (daysInWeek != null ? !daysInWeek.equals(that.daysInWeek) : that.daysInWeek != null) return false;
        if (daysInWeekTotal != null ? !daysInWeekTotal.equals(that.daysInWeekTotal) : that.daysInWeekTotal != null)
            return false;
        if (manuallySetDay != that.manuallySetDay) return false;
        if (duration != null ? !duration.equals(that.duration) : that.duration != null) return false;
        if (calendar != null ? !calendar.equals(that.calendar) : that.calendar != null) return false;
        if (begin != null ? !begin.equals(that.begin) : that.begin != null) return false;
        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        if (jobDuration != null ? !jobDuration.equals(that.jobDuration) : that.jobDuration != null) return false;
        return reminder != null ? reminder.equals(that.reminder) : that.reminder == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (daysInWeek != null ? daysInWeek.hashCode() : 0);
        result = 31 * result + (daysInWeekTotal != null ? daysInWeekTotal.hashCode() : 0);
        result = 31 * result + (manuallySetDay != null ? manuallySetDay.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (calendar != null ? calendar.hashCode() : 0);
        result = 31 * result + (begin != null ? begin.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (jobDuration != null ? jobDuration.hashCode() : 0);
        result = 31 * result + (reminder != null ? reminder.hashCode() : 0);
        return result;
    }
}

