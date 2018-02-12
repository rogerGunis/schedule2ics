package de.gunis.roger.jobService.jobsToDo;

import de.gunis.roger.jobService.calendar.Holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LaborMarket {
    private final List<JobDescription> jobs;

    public LaborMarket(List<JobDescription> jobs) {
        this.jobs = jobs;
    }

    public List<JobDescription> getJobDescriptions(LocalDate day, Optional<Holiday> mayBeHoliday) {

        List<JobDescription> jobQueue;
        if (mayBeHoliday.isPresent()) {
            jobQueue = jobs.stream().filter(job -> job.isWithinRange(day)).filter(job -> job.hasToBeDoneOnHoliday(day)).collect(Collectors.toList());
        } else {
            jobQueue = jobs.stream().filter(job -> job.isWithinRange(day)).collect(Collectors.toList());
        }

        return jobQueue;
    }
}
