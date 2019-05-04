package de.gunis.roger.jobService.workersAvailable;

import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.jobsToDo.Job;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobCenterTest {

    private JobCenter jobCenter = null;
    private JobDescription jobCleaning;

    @After
    public void tearDown() throws Exception {
        jobCenter.close();
        HolidayInformationCenter.close();
    }

    @Before
    public void setUp() throws Exception {
        jobCenter = JobCenter.open();
        HolidayInformationCenter.open();

        LocalDate monday = LocalDate.of(2017, 2, 27);
        jobCleaning = new JobDescription("cleaning",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                7, monday, monday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")));
    }

    @Test
    public void addWorker() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker worker = createWorker("John", cleaning);
        jobCenter.addWorker(worker);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);

        Assert.assertEquals(worker, workerForJob);
    }

    @Test
    public void addWorkerAndVacation() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker worker = new Worker("John",
                Stream.of(cleaning).collect(Collectors.toSet()),
                Stream.of(new Holiday(LocalDate.now(), LocalDate.now(), "blub")).collect(Collectors.toList()));
        jobCenter.addWorker(worker);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now(), jobCleaning);

        Assert.assertEquals(null, workerForJob);
    }

    @Test
    public void addTwoWorkers() throws Exception {
        Job cleaning = new Job("cleaning");
        Worker john = createWorker("John", cleaning);
        Worker jim = createWorker("Jim", cleaning);
        jobCenter.addWorker(john);
        jobCenter.addWorker(jim);
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);
        Worker workerForJobSameDay = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);

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
        Worker workerForJob = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);
        Worker workerForJobSameDay = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);
        Worker nextRound = jobCenter.getWorkerForJob(LocalDate.now().plusDays(1L), jobCleaning);

        Assert.assertEquals(john, workerForJob);
        Assert.assertEquals(jim, workerForJobSameDay);
        Assert.assertEquals(john, nextRound);
    }

    @Test
    public void resetRoundEvenOneWasOnHoliday() throws Exception {
        Job cleaning = new Job("cleaning");
        LocalDate newYear = LocalDate.of(2017, 1, 1);
        Worker john = new Worker("John" + "",
                Stream.of(cleaning).collect(Collectors.toSet()),
                Stream.of(new Holiday(newYear, newYear, "John is on Holiday@NewYear")).collect(Collectors.toList()));
        Worker jim = createWorker("Jim", cleaning);
        Worker tom = createWorker("Tom", cleaning);
        jobCenter.addWorker(john);
        jobCenter.addWorker(jim);
        jobCenter.addWorker(tom);
        Worker workerForNewYear = jobCenter.getWorkerForJob(newYear, jobCleaning);
        Worker workerForJobNewYearAndOneDay = jobCenter.getWorkerForJob(newYear.plusDays(1L), jobCleaning);
        Worker workerForJobNewSameDay = jobCenter.getWorkerForJob(newYear.plusDays(1L), jobCleaning);

        Assert.assertEquals(jim, workerForNewYear);
        Assert.assertEquals(tom, workerForJobNewYearAndOneDay);
        Assert.assertEquals(john, workerForJobNewSameDay);
    }

    Worker createWorker(String name, Job cleaning) {
        return new Worker(name,
                Stream.of(cleaning).collect(Collectors.toSet()),
                null);
    }
}