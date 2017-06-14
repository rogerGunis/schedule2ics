package de.gunis.roger;

import de.gunis.roger.calendar.HolidayInformationCenter;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
                new Worker("asdf",
                        Stream.of(new Job("help")).collect(Collectors.toSet()),
                        null)
        ).collect(Collectors.toList());

        List<JobDescription> jobDescriptions = Stream.of(
                new JobDescription("help",
                        Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                        1,
                        newYear, newYear, null)
        ).collect(Collectors.toList());

        LocalDate myDay = newYear;
        LocalDate endDay = newYear.plusDays(15L);

        JobCenter.instance().combineJobAndWorkerAndSubscribe(workers, jobDescriptions, myDay, endDay);

        JobCenter.close();
        HolidayInformationCenter.close();

    }


}