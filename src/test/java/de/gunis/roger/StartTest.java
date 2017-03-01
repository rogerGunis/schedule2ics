package de.gunis.roger;

import com.beust.jcommander.JCommander;
import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StartTest {

    @Test
    public void startMain() {
        Start main = new Start();
        String[] argv = {"-hs", "/home/vagrant/scheduler2ics/internal/Holidays.csv ",
                "-js", "/home/vagrant/scheduler2ics/internal/JobDescription.csv",
                "-ws", "/home/vagrant/scheduler2ics/internal/Workers.csv", "-log", "DEBUG", "-out", "/var/tmp/scheduler"};
        JCommander jCommander = new JCommander(main, argv);
        main.run(jCommander);

    }

    @Test
    public void combineJobAndWorker() throws Exception {
        JobCenter.open();

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

        JobCenter.instance().combineJobAndWorkerAndSubscribe(holidays, workers, jobDescriptions, myDay, endDay);

//        documentJobsAndWork(jobDescriptions.stream().map(job -> (ICalendarAccess) job).collect(Collectors.toList()), "/tmp");
        JobCenter.close();

    }


}