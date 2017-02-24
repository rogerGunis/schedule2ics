package de.gunis.roger.imports;

import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.Worker;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.gunis.roger.imports.CsvFileLoader.*;

public class CsvFileLoaderTest {

    @Test
    public void testWorker() throws Exception {
        List<Worker> workers = Stream.of("Batman, drive rescue, 2011-12-12")
                .map(trimLine).map(mapToWorker).collect(Collectors.toList());
        Assert.assertTrue(workers.size() > 0);
    }

    @Test
    public void testJobDescription() throws Exception {
        List<JobDescription> jobDescriptions = Stream.of("rescue, 1, 7")
                .map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
        Assert.assertTrue(jobDescriptions.size() > 0);
    }

}