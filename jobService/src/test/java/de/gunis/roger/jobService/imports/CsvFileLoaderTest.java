package de.gunis.roger.jobService.imports;

import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.workersAvailable.Worker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gunis.roger.jobService.imports.CsvFileLoader.*;

public class CsvFileLoaderTest {

    String holidayCsvLineExample = "01.01.2017,01.01.2017,New Year";

    // name, jobs (separated by space), vacation settings
    // only one job proposal (diving) possible
    String workerCsvLineExample = "Batman, drive rescue(diving), 0, 12.12.2017-13.12.2017 14.12.2017 - 15.12.2017";

    // name, startDayOfWeek (mo=1,...,sun=7),duration, begin, end, moveInformationToDayOfWeek (off=0, mo=1,...,sun=7), special
    String jobDescriptionCsvLineExample = "rescue, 1, 7, 01.01.2017, 01.01.2017, 0, 1, 1,''";

    String dateFormat = "dd.MM.yyyy";

    // special treatment where to set information of the worker who will to this job which is on all weekdays
    // I want to put it on sunday only - and not on the weekdays
    String jobDescriptionInfo2DayOfWeek = "making breakfast whole week, 1, 7, 01.01.2017, 01.01.2017, 7, 1, 1,''";


    @Before
    public void setUp() throws Exception {
        CsvFileLoader.dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
    }


    @Test
    public void testHoliday() throws Exception {
        List<Holiday> holiday = Stream.of(holidayCsvLineExample)
                .map(trimLine).map(mapToHoliday).collect(Collectors.toList());
        Assert.assertTrue(holiday.size() > 0);
    }

    @Test
    public void testWorker() throws Exception {
        Set<DayOfWeek> sunday = Arrays.asList(DayOfWeek.SUNDAY).stream().collect(Collectors.toSet());
        List<Worker> workers = Stream.of(workerCsvLineExample)
                .map(trimLine).map(mapToWorker).collect(Collectors.toList());
        String jobProposal = workers.get(0).askForProposal(new JobDescription("rescue", sunday,
                7, LocalDate.parse("11.03.2017", dateFormatter), LocalDate.parse("11.03.2017", dateFormatter),
                Boolean.FALSE,
                new HashSet<>(Arrays.asList("Bauernhof")),""));
        Assert.assertTrue(workers.size() > 0);
        Assert.assertEquals("diving", jobProposal);
        Assert.assertTrue(workers.get(0).isOnHoliday(LocalDate.parse("12.12.2017", dateFormatter)));
    }

    @Test
    public void testJobDescription() throws Exception {
        List<JobDescription> jobDescriptions = Stream.of(jobDescriptionCsvLineExample)
                .map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
        Assert.assertTrue(jobDescriptions.size() > 0);
    }

    @Test
    public void testJobDescriptionInfo2DayOfWeek() throws Exception {
        List<JobDescription> jobDescriptions = Stream.of(jobDescriptionInfo2DayOfWeek)
                .map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
        Assert.assertTrue("We move the information of the whole week to one day, wich is sunday",
                jobDescriptions.get(0).getManuallySetDay().equals(DayOfWeek.SUNDAY));
    }
}