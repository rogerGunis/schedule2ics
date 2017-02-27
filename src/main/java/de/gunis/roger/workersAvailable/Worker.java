package de.gunis.roger.workersAvailable;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

public class Worker {
    private static final Logger logger = LoggerFactory.getLogger("Worker.class");
    String name;
    private List<Holiday> vacations;
    private Set<Job> jobs = new HashSet<>();
    private Map<Job, Boolean> hasJobDone = new HashMap<>();

    public Worker(String name, Set<Job> jobs, List<Holiday> vacations) {
        this.name = name;
        this.jobs = jobs;
        this.vacations = validate(vacations);
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
