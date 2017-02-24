package de.gunis.roger.jobsToDo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LaborMarket {
    private static List<Job> NOTHING_TODO = Collections.emptyList();
    private final List<JobDescription> jobs;

    public LaborMarket(List<JobDescription> jobs) {
        this.jobs = jobs;
    }

    public List<JobDescription> getJobDescriptions(LocalDate day) {
        return jobs.stream().filter(job -> job.hasToBeDoneOn(day)).collect(Collectors.toList());
    }
}
