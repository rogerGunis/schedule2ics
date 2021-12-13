package de.gunis.roger.jobService;

import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.jobsToDo.Job;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.workersAvailable.JobCenter;
import de.gunis.roger.jobService.workersAvailable.Worker;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmployeeSearchTest {

    @Test
    public void combineJobAndWorker() throws Exception {
        JobCenter.open();
        HolidayInformationCenter.open();

        LocalDate newYear = LocalDate.of(2017, 01, 01);
        List<Worker> workers = Stream.of(
                new Worker("asdf1",
                        Stream.of(new Job("help")).collect(Collectors.toSet()),
                        null, 0),
                new Worker("asdf2",
                        Stream.of(new Job("help")).collect(Collectors.toSet()),
                        null, 0)
        ).collect(Collectors.toList());

        List<JobDescription> jobDescriptions = Stream.of(
                new JobDescription("help",
                        Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                        1,
                        newYear, newYear, null, new HashSet<>(Arrays.asList("Bauernhof")),"asdf2")
        ).collect(Collectors.toList());

        LocalDate myDay = newYear;
        LocalDate endDay = newYear.plusDays(15L);

        JobCenter.instance().combineJobAndWorkerAndSubscribe(workers, jobDescriptions, myDay, endDay);

        JobCenter.close();
        HolidayInformationCenter.close();

    }


}