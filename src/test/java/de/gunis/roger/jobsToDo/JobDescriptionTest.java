package de.gunis.roger.jobsToDo;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.calendar.HolidayInformationCenter;
import de.gunis.roger.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobDescriptionTest {

    @Test
    public void testInfo2WeekOfDay() {
        LocalDate monday = LocalDate.of(2017, 2, 27);
        JobDescription jobDescription = new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                7, monday, monday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"));
        Worker worker = new Worker("asdf",
                Stream.of(new Job("JobA")).collect(Collectors.toSet()), null);
        jobDescription.registerWorkerOnDate(monday, worker, jobDescription);
        Calendar calendar = jobDescription.getCalendar();
        Assert.assertTrue(calendar != null);
    }

    @Test
    public void hasToBeDoneOnHoliday() {
        HolidayInformationCenter.open();
        LocalDate monday = LocalDate.of(2017, 5, 22);
        LocalDate friday = LocalDate.of(2017, 5, 26);
        LocalDate saturday = LocalDate.of(2017, 5, 27);
        Holiday testHoliday = new Holiday(monday, saturday, "testHoliday");
        Set<Holiday> holidays = new HashSet<>();
        holidays.add(testHoliday);
        HolidayInformationCenter.instance().setHolidays(holidays);


        JobDescription jobDescription = new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                5, monday, friday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"));

//        HolidayInformationCenter.instance().getHolidays().stream().anyMatch(holiday -> holiday.isWithinRange(friday));

        Assert.assertFalse(jobDescription.hasToBeDoneOnHoliday(monday));
        HolidayInformationCenter.close();
    }

    @Test
    public void adjustDaysInWeekTotalTest() {
        LocalDate jobActivationOnMonday = LocalDate.of(2017, 5, 22);
        LocalDate jobTerminationOnFriday = LocalDate.of(2017, 5, 26);

        JobDescription jobDescription = new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                2, jobActivationOnMonday, jobTerminationOnFriday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"));

        Assert.assertEquals(jobDescription.getDaysInWeekTotal().size(), 2);

    }


    @Test(expected = IllegalArgumentException.class)
    public void adjustDaysInWeekTotalTestException() {
        LocalDate jobActivationOnMonday = LocalDate.of(2017, 5, 22);
        LocalDate jobTerminationOnFriday = LocalDate.of(2017, 5, 26);

        new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY).collect(Collectors.toSet()),
                2, jobActivationOnMonday, jobTerminationOnFriday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void jobRangeProducesTestException() {
        LocalDate jobActivationOnMonday = LocalDate.of(2017, 5, 22);
        LocalDate jobTerminationOnFriday = LocalDate.of(2017, 5, 26);

        new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY).collect(Collectors.toSet()),
                2, jobTerminationOnFriday, jobActivationOnMonday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"));

    }
}