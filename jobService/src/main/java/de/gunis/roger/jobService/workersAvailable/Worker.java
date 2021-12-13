package de.gunis.roger.jobService.workersAvailable;

import de.gunis.roger.jobService.ClearingHouse;
import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.calendar.ICalendarAccess;
import de.gunis.roger.jobService.jobsToDo.Job;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

public class Worker implements ICalendarAccess {
    private static final Logger logger = LoggerFactory.getLogger("Worker.class");
    private final net.fortuna.ical4j.model.Calendar calendar;
    String name;
    private List<Holiday> vacations;
    private Set<Job> jobs = new HashSet<>();
    private Map<Job, Boolean> hasJobDone = new HashMap<>();

    public int hasToggledState() {
        return toggleTurnus;
    }

    private int toggleTurnus;
    private UidGenerator ug = null;

    public Worker(String name, Set<Job> jobs, List<Holiday> vacations, int toggleTurnus) {
        this.name = name;
        this.jobs = jobs;
        this.vacations = validate(vacations);
        this.toggleTurnus = toggleTurnus;

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

    public void registerJobOnDate(LocalDate day, Dur duration, JobDescription jobDescription) {
        long jobDate = day.toEpochDay() * 86400 * 1000;
        String jobDescriptionName = jobDescription.getName();
        VEvent vEvent = new VEvent(new net.fortuna.ical4j.model.Date(jobDate), duration, jobDescriptionName);

        vEvent.getProperties().add(new Uid(String.valueOf(day.format(ClearingHouse.dateTimeFormatter)) + "_" + jobDescriptionName));

        Optional<Job> maybeJobProposalFromWorker = jobs.stream().filter(job -> job.getName().equals(jobDescription.getName())).findFirst();

        if (maybeJobProposalFromWorker.isPresent()) {
            Description calDescription = new Description();
            calDescription.setValue(maybeJobProposalFromWorker.get().getJobProposal());
            vEvent.getProperties().add(calDescription);
        }

        if (jobDescription.hasReminder()) {
            Set<Holiday> holidays = HolidayInformationCenter.instance().getHolidays();

            // find previous workday
            VAlarm cooking = new VAlarm(new Dur(0, -9, 0, 0));
            cooking.getProperties().add(Action.DISPLAY);
            cooking.getProperties().add(new Description("Kochen für " + jobDescription.getName()));
            vEvent.getAlarms().add(cooking);

            Optional<LocalDate> dayOfShopping = Stream.iterate(day.minusDays(2), date -> date.minusDays(1))
                    .limit(6)
                    .filter(
                            iDay -> holidays.stream().allMatch(holiday -> holiday.isShoppingPossible(iDay))
                    ).findFirst();

            LocalDate shoppingDay = dayOfShopping.orElse(day);
            int diffOfDays = shoppingDay.getDayOfYear() - day.getDayOfYear();

            VAlarm goShopping = new VAlarm(new Dur(diffOfDays, -15, 0, 0));
            goShopping.getProperties().add(Action.DISPLAY);
            goShopping.getProperties().add(new Description("Einkaufen für " + jobDescription.getName()));
            vEvent.getAlarms().add(goShopping);
        }


        vEvent.getProperties().add(new Comment("----------------------------------"));
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

    public boolean isOnHoliday(LocalDate day) {
        return vacations.stream().anyMatch(vacation -> vacation.isWithinRange(day));
    }

    public Worker clone(){
        return new Worker(name,jobs,vacations, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Worker worker = (Worker) o;

        if (name != null ? !name.equals(worker.name) : worker.name != null) return false;
        if (vacations != null ? !vacations.equals(worker.vacations) : worker.vacations != null) return false;
        return jobs != null ? jobs.equals(worker.jobs) : worker.jobs == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (vacations != null ? vacations.hashCode() : 0);
        result = 31 * result + (jobs != null ? jobs.hashCode() : 0);
        return result;
    }

    void resetJobDone(Job job) {
        hasJobDone.put(job, Boolean.FALSE);
    }

    public String getName() {
        return name;
    }

    public String askForProposal(JobDescription jobName) {
        Optional<Job> maybeJobInfoFromWorker = jobs.stream().filter(job -> job.getName().equals(jobName.getName())).findFirst();

        return maybeJobInfoFromWorker.map(Job::getJobProposal).orElse(null);
    }
}
