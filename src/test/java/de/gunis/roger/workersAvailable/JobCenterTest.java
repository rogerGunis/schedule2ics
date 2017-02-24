package de.gunis.roger.workersAvailable;

import de.gunis.roger.jobsToDo.Job;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobCenterTest {

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
    public void addWorker() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker worker = createWorker("John", cleaning);
        jobCenter.addWorker(worker);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);

        Assert.assertEquals(worker, workerForJob);
    }

    @Test
    public void addWorkerAndVacation() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker worker = new Worker("John",
                Stream.of(cleaning).collect(Collectors.toSet()),
                Stream.of(LocalDate.now()).collect(Collectors.toList()));
        jobCenter.addWorker(worker);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now(), cleaning);

        Assert.assertEquals(null, workerForJob);
    }

    @Test
    public void addTwoWorkers() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker john = createWorker("John", cleaning);
        Worker jim = createWorker("Jim", cleaning);
        jobCenter.addWorker(john);
        jobCenter.addWorker(jim);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);
        Worker workerForJobSameDay = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);

        Assert.assertEquals(john, workerForJob);
        Assert.assertEquals(jim, workerForJobSameDay);
    }

    @Test
    public void resetRound() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker john = createWorker("John", cleaning);
        Worker jim = createWorker("Jim", cleaning);
        jobCenter.addWorker(john);
        jobCenter.addWorker(jim);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);
        Worker workerForJobSameDay = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);
        Worker nextRound = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);

        Assert.assertEquals(john, workerForJob);
        Assert.assertEquals(jim, workerForJobSameDay);
        Assert.assertEquals(john, nextRound);
    }

    @Test
    public void resetRoundEvenOneWasOnHoliday() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker john = new Worker("John" + "",
                Stream.of(cleaning).collect(Collectors.toSet()),
                Stream.of(LocalDate.now()).collect(Collectors.toList()));
        Worker jim = createWorker("Jim", cleaning);
        Worker tom = createWorker("Tom", cleaning);
        jobCenter.addWorker(john);
        jobCenter.addWorker(jim);
        jobCenter.addWorker(tom);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now(), cleaning);
        Worker workerForJobNextDay = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);
        Worker nextOne = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), cleaning);

        Assert.assertEquals(jim, workerForJob);
        Assert.assertEquals(tom, workerForJobNextDay);
        Assert.assertEquals(john, nextOne);
    }

    Worker createWorker(String name, Job cleaning) {
        return new Worker(name,
                Stream.of(cleaning).collect(Collectors.toSet()),
                null);
    }
}