package de.gunis.roger.jobService.jobsToDo;

import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.workersAvailable.Worker;
import net.fortuna.ical4j.model.Calendar;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
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
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")), "");
        Worker worker = new Worker("asdf",
                Stream.of(new Job("JobA")).collect(Collectors.toSet()), null, 0);
        jobDescription.registerWorkerOnDate(monday, worker);
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
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")), "");

//        HolidayInformationCenter.instance().getHolidays().stream().anyMatch(holiday -> holiday.isWithinRange(friday));

        Assert.assertFalse(jobDescription.hasToBeDoneOnHoliday(monday));
        HolidayInformationCenter.close();
    }

    @Test
    public void hasToBeDoneOnBauernhof() {
        String noSkipOnThisHoliday = "Bauernhof";
        HolidayInformationCenter.open();
        LocalDate sunday = LocalDate.of(2017, 5, 21);
        LocalDate monday = LocalDate.of(2017, 5, 22);
        LocalDate friday = LocalDate.of(2017, 5, 26);
        LocalDate saturday = LocalDate.of(2017, 5, 27);
        Holiday testHoliday = new Holiday(monday, saturday, noSkipOnThisHoliday);
        Set<Holiday> holidays = new HashSet<>();
        holidays.add(testHoliday);
        HolidayInformationCenter.instance().setHolidays(holidays);


        JobDescription jobDescription = new JobDescription("Weekly",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                5, monday, friday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList(noSkipOnThisHoliday)), "");

//        HolidayInformationCenter.instance().getHolidays().stream().anyMatch(holiday -> holiday.isWithinRange(friday));

        Assert.assertTrue(jobDescription.hasToBeDoneOnHoliday(monday));
        HolidayInformationCenter.close();
    }

    @Test
    public void hasToBeDoneOnBauernhofDaily() {
        String noSkipOnThisHoliday = "Bauernhof";
        HolidayInformationCenter.open();
        LocalDate sunday = LocalDate.of(2017, 5, 21);
        LocalDate monday = LocalDate.of(2017, 5, 22);
        LocalDate friday = LocalDate.of(2017, 5, 26);
        LocalDate saturday = LocalDate.of(2017, 5, 27);
        Holiday testHoliday = new Holiday(monday, saturday, noSkipOnThisHoliday);
        Set<Holiday> holidays = new HashSet<>();
        holidays.add(testHoliday);
        HolidayInformationCenter.instance().setHolidays(holidays);


        JobDescription jobDescription = new JobDescription("Daily",
                Stream.of(DayOfWeek.MONDAY).collect(Collectors.toSet()),
                1, monday, friday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Collections.EMPTY_LIST), "");

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
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")), "");

        Assert.assertEquals(jobDescription.getDaysInWeekTotal().size(), 2);

    }


    @Test(expected = IllegalArgumentException.class)
    public void adjustDaysInWeekTotalTestException() {
        LocalDate jobActivationOnMonday = LocalDate.of(2017, 5, 22);
        LocalDate jobTerminationOnFriday = LocalDate.of(2017, 5, 26);

        new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY).collect(Collectors.toSet()),
                2, jobActivationOnMonday, jobTerminationOnFriday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")), "");
    }


    @Test(expected = IllegalArgumentException.class)
    public void jobRangeProducesTestException() {
        LocalDate jobActivationOnMonday = LocalDate.of(2017, 5, 22);
        LocalDate jobTerminationOnFriday = LocalDate.of(2017, 5, 26);

        new JobDescription("test",
                Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY).collect(Collectors.toSet()),
                2, jobTerminationOnFriday, jobActivationOnMonday,
                DayOfWeek.SUNDAY, Boolean.getBoolean("0"), new HashSet<>(Arrays.asList("Bauernhof")), "");

    }
}