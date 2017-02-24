package de.gunis.roger.workersAvailable;

import de.gunis.roger.jobsToDo.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

public class Worker {
    private static final Logger logger = LoggerFactory.getLogger("Worker.class");
    String name;
    private List<LocalDate> vacation;
    private Set<Job> jobs = new HashSet<>();
    private Map<Job, Boolean> hasJobDone = new HashMap<>();

    public Worker(String name, Set<Job> jobs, List<LocalDate> vacation) {
        this.name = name;
        this.jobs = jobs;
        this.vacation = validate(vacation);
    }

    private List<LocalDate> validate(List<LocalDate> vacation) {
        return vacation == null ? Collections.emptyList() : vacation;
    }

    public void doJob(Job job) {
        if (!jobs.contains(job)) {
            try {
                throw new IllegalJobException("This job cannot be done by me");
            } catch (IllegalJobException e) {
                logger.error("Exception: " + e);
            }
        }
        hasJobDone.put(job, Boolean.TRUE);
    }

    public Boolean hasJobDone(Job job) {
        return hasJobDone.getOrDefault(job, Boolean.FALSE);
    }

    @Override
    public String toString() {
        return "Worker{" +
                "jobs=" + jobs +
                ", vacation=" + vacation +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Worker worker = (Worker) o;

        if (jobs != null ? !jobs.equals(worker.jobs) : worker.jobs != null) return false;
        if (vacation != null ? !vacation.equals(worker.vacation) : worker.vacation != null) return false;
        return name != null ? name.equals(worker.name) : worker.name == null;
    }

    @Override
    public int hashCode() {
        int result = jobs != null ? jobs.hashCode() : 0;
        result = 31 * result + (vacation != null ? vacation.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public Set<Job> getJobs() {
        return jobs;
    }

    public boolean isAvailable(LocalDate day) {
        return !vacation.contains(day);
    }

    public void resetJobDone(Job job) {
        hasJobDone.put(job, Boolean.FALSE);
    }

    public String getName() {
        return name;
    }
}
