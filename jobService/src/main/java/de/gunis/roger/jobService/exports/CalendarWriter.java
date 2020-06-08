package de.gunis.roger.jobService.exports;

import de.gunis.roger.jobService.calendar.ICalendarAccess;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.DtStart;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarWriter {
    private static final Logger logger = LoggerFactory.getLogger("CalendarWriter.class");

    public static boolean isWithinRange(Pair<LocalDate, LocalDate> reportRange, long value) {
        LocalDate testDate = Instant.ofEpochMilli(value * 1000).atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
        return (Math.max(reportRange.getKey().toEpochDay(), testDate.toEpochDay()) == Math.min(testDate.toEpochDay(), reportRange.getValue().toEpochDay()));
    }

    public static void writeCalendar(Calendar calendar, String outputFile, Pair<LocalDate, LocalDate> reportRange) {
        Calendar calendarWithRange = new Calendar();

        List<CalendarComponent> vevents = calendar.getComponents("VEVENT").stream().filter(event -> isWithinRange(reportRange, ((DtStart) event.getProperty(Property.DTSTART)).getDate().getTime() / 1000)).collect(Collectors.toList());
        calendarWithRange.getComponents().addAll(vevents);

        Path path = Paths.get(outputFile);
        FileOutputStream fileOutputStream = null;
        try {
            Files.createDirectories(path.getParent());
            fileOutputStream = new FileOutputStream(path.toString());
            CalendarOutputter calendarOutputter = new CalendarOutputter();
            calendarOutputter.setValidating(false);
            calendarOutputter.output(calendarWithRange, fileOutputStream);

        } catch (IOException e) {
            logger.warn("Exception: {}", e);
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                logger.warn("Exception: {}", e);
            }
        }
    }

    public static void documentJobsAndWorkers(List<ICalendarAccess> calendarAccesses, String outputFilePath, Pair<LocalDate, LocalDate> reportRange) {
        calendarAccesses.forEach(calendarAccess -> {
                    Calendar calendar = calendarAccess.getCalendar();
                    String name = normalizeString(calendarAccess.getName());
                    Path path = Paths.get(outputFilePath, name + ".ics");
                    writeCalendar(calendar, path.toString(), reportRange);
                }
        );
    }

    private static String normalizeString(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        return name.replaceAll("[^\\x00-\\x7F]", "").replaceAll("\\s+", "-");
    }
}
