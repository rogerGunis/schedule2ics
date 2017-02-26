package de.gunis.roger.jobsToDo;

import de.gunis.roger.calendar.Holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LaborMarket {
    private final List<JobDescription> jobs;
    private final Set<Holiday> holidays;

    public LaborMarket(List<JobDescription> jobs, Set<Holiday> holidays) {
        this.jobs = jobs;
        this.holidays = holidays;
    }

    public List<JobDescription> getJobDescriptions(LocalDate day) {
        Optional<Holiday> holidayRange = holidays.parallelStream().filter(holiday -> holiday.isWithinRange(day)).findFirst();

        List<JobDescription> jobQueue;
        if (holidayRange.isPresent()) {
            jobQueue = jobs.parallelStream().filter(job -> job.hasToBeDoneOn(day, holidayRange.get())).collect(Collectors.toList());
        } else {

            jobQueue = jobs.parallelStream().filter(job -> job.hasToBeDoneOn(day)).collect(Collectors.toList());
        }

        return jobQueue;
    }
}
