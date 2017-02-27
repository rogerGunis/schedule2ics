package de.gunis.roger.imports;

import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.Worker;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gunis.roger.imports.CsvFileLoader.*;

public class CsvFileLoaderTest {

    @Test
    public void testWorker() throws Exception {
        CsvFileLoader.dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<Worker> workers = Stream.of("Batman, drive rescue, 12.12.2011")
                .map(trimLine).map(mapToWorker).collect(Collectors.toList());
        Assert.assertTrue(workers.size() > 0);
    }

    @Test
    public void testJobDescription() throws Exception {
        CsvFileLoader.dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<JobDescription> jobDescriptions = Stream.of("rescue, 1, 7, 01.01.2017, 01.01.2017")
                .map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
        Assert.assertTrue(jobDescriptions.size() > 0);
    }

    @Test
    public void testJobDescriptionInfo2DayOfWeek() throws Exception {
        CsvFileLoader.dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<JobDescription> jobDescriptions = Stream.of("rescue, 1, 7, 01.01.2017, 01.01.2017,7")
                .map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
        Assert.assertTrue(jobDescriptions.get(0).getInfoInDayOfWeek().equals(DayOfWeek.SUNDAY));
    }
}