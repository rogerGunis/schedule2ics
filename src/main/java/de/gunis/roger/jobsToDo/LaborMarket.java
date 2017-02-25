package de.gunis.roger.jobsToDo;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class LaborMarket {
    private final List<JobDescription> jobs;

    public LaborMarket(List<JobDescription> jobs) {
        this.jobs = jobs;
    }

    public List<JobDescription> getJobDescriptions(LocalDate day) {
        return jobs.parallelStream().filter(job -> job.hasToBeDoneOn(day)).collect(Collectors.toList());
    }
}
