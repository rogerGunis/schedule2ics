package de.gunis.roger.jobService.exports;

import de.gunis.roger.jobService.calendar.ICalendarAccess;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;

public class CalendarWriter {
    private static final Logger logger = LoggerFactory.getLogger("CalendarWriter.class");

    public static void writeCalendar(Calendar calendar, String outputFile) {
        Path path = Paths.get(outputFile);
        FileOutputStream fileOutputStream = null;
        try {
            Files.createDirectories(path.getParent());
            fileOutputStream = new FileOutputStream(path.toString());
            CalendarOutputter calendarOutputter = new CalendarOutputter();
            calendarOutputter.setValidating(false);
            calendarOutputter.output(calendar, fileOutputStream);

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

    public static void documentJobsAndWorkers(List<ICalendarAccess> calendarAccesses, String outputFilePath) {
        calendarAccesses.forEach(calendarAccess -> {
                    Calendar calendar = calendarAccess.getCalendar();
                    String name = normalizeString(calendarAccess.getName());
                    Path path = Paths.get(outputFilePath, name + ".ics");
                    writeCalendar(calendar, path.toString());
                }
        );
    }

    private static String normalizeString(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFD);
        return name.replaceAll("[^\\x00-\\x7F]", "").replaceAll("\\s+", "-");
    }
}