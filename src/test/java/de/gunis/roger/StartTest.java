package de.gunis.roger;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.calendar.ICalendarAccess;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gunis.roger.exports.CalendarWriter.documentJobsAndWork;

public class StartTest {
    JobCenter jobCenter = null;

    @After
    public void tearDown() throws Exception {
        jobCenter.stop();
    }

    @Before
    public void setUp() throws Exception {
        jobCenter = JobCenter.start();
    }

    @Test
    public void combineJobAndWorker() throws Exception {
        LocalDate newYear = LocalDate.of(2017, 01, 01);
        Set<Holiday> holidays = Stream.of(new Holiday(newYear.minusDays(1L), newYear.minusDays(1L), "myHoliday"))
                .collect(Collectors.toSet());
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

        JobCenter.instance().combineJobAndWorkerAndRegisterOnDescription(holidays, workers, jobDescriptions, myDay, endDay);

        documentJobsAndWork(jobDescriptions.stream().map(job -> (ICalendarAccess) job).collect(Collectors.toList()), "/tmp");
    }


}