package de.gunis.roger.jobsToDo;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobDescriptionTest {

    @Test
    public void testInfo2WeekOfDay() {
        LocalDate monday = LocalDate.of(2017, 2, 27);
        JobDescription jobDescription = new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                7, monday, monday,
                DayOfWeek.SUNDAY);
        Worker worker = new Worker("asdf",
                Stream.of(new Job("JobA")).collect(Collectors.toSet()), null);
        jobDescription.registerWorkerOnDate(monday, worker, jobDescription);
        Calendar calendar = jobDescription.getCalendar();
        Assert.assertTrue(calendar != null);
    }

    @Test
    public void hasToBeDoneOnHoliday() {
        LocalDate monday = LocalDate.of(2017, 5, 22);
        LocalDate friday = LocalDate.of(2017, 5, 26);
        LocalDate saturday = LocalDate.of(2017, 5, 27);
        JobDescription jobDescription = new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                5, monday, friday,
                DayOfWeek.SUNDAY);
        Holiday testHoliday = new Holiday(monday, saturday, "testHoliday");
        Assert.assertFalse(jobDescription.hasToBeDoneOnHoliday(monday, testHoliday));
    }

}