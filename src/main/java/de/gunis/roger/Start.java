package de.gunis.roger;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.calendar.ICalendarAccess;
import de.gunis.roger.exports.CalendarWriter;
import de.gunis.roger.imports.CsvFileLoader;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Start {
    private static final Logger logger = LoggerFactory.getLogger("Start.class");

    @SuppressWarnings("unused")
    @Parameter(required = true, names = {"--holidays", "-hs"}, description = "File for Holidays <begin, end,event>")
    private String inputFilePathHolidays;

    @SuppressWarnings("unused")
    @Parameter(required = true, names = {"--jobDescriptions", "-js"}, description = "File for Jobs <name,dayOfWeek (mo=1,...,sun=7),duration,begin,end,infoInDayOfWeek (off=0, mo=1,...,sun=7)>")
    private String inputFilePathJobDescriptions;

    @SuppressWarnings("unused")
    @Parameter(required = true, names = {"--workers", "-ws"}, description = "File for Workers <name,jobA jobB..,Holiday (begin-end)>")
    private String inputFilePathWorkers;

    @SuppressWarnings("unused")
    @Parameter(required = true, names = {"--outputFilePath", "-out"}, description = "Path for ics file created per JobDescription")
    private String outputFilePath;

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"--dateFormat", "-dateFormat"}, description = "Format of the Dates (begin|end|holiday) in all files [yyyy-MM-dd|dd.MM.yyyy|...]")
    private String dateFormat = "dd.MM.yyyy";

    @SuppressWarnings("unused")
    @Parameter(names = {"-log", "-verbose"}, description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
    private String verbose;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = {"--help", "-h"}, description = "This help", help = true)
    private boolean help = false;

    Start() {
    }

    public static void main(String[] args) {

        Start main = new Start();
        JCommander jCommander = new JCommander(main, args);
        main.run(jCommander);

    }


    private static void setLoggingLevel(String level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(level, Level.DEBUG));

        // we suppress calendar TRACE level, because not needed for me
        ch.qos.logback.classic.Logger cal = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("net.fortuna.ical4j.data.FoldingWriter");
        cal.setLevel(Level.DEBUG);
    }


    void run(JCommander jCommander) {


        if (help) {
            jCommander.usage();
            System.out.print("-------------\nAll elements marked with * are required\n-------------\n");
            System.exit(2);
        }

        logger.info("starting with: {}, {}, {}", inputFilePathHolidays, inputFilePathWorkers, inputFilePathJobDescriptions);
        setLoggingLevel(verbose);

        JobCenter.open();
        CsvFileLoader csvFileLoader = new CsvFileLoader(dateFormat);

        Set<Holiday> holidays = csvFileLoader.importHolidaysFromFile(inputFilePathHolidays);
        List<Worker> workers = csvFileLoader.importWorkerFromFile(inputFilePathWorkers);
        List<JobDescription> jobDescriptions = csvFileLoader.importJobDescriptionFromFile(inputFilePathJobDescriptions);

        int startOffset = jobDescriptions.stream().mapToInt(job -> (int) job.getBegin().toEpochDay()).min().getAsInt();
        int endOffset = jobDescriptions.stream().mapToInt(job -> (int) job.getEnd().toEpochDay()).max().getAsInt();

        LocalDate myDay = LocalDate.ofEpochDay(startOffset);
        LocalDate endDay = LocalDate.ofEpochDay(endOffset);

        logger.info("Searching, between {} -> {} (days: {})", myDay, endDay, endDay.toEpochDay() - myDay.toEpochDay());

        JobCenter.instance().combineJobAndWorkerAndSubscribe(holidays, workers, jobDescriptions, myDay, endDay);

        CalendarWriter.documentJobsAndWork(jobDescriptions.stream().map(job -> (ICalendarAccess) job).collect(Collectors.toList()), outputFilePath);
        CalendarWriter.documentJobsAndWork(workers.stream().map(worker -> (ICalendarAccess) worker).collect(Collectors.toList()), outputFilePath);

        try {
            CalendarWriter.writeCalendar(JobCenter.instance().getAllCalendarEntries(),
                    Paths.get(outputFilePath, "allEvents.ics").toString());
        } catch (Exception e) {
            logger.error("Buggy Calender export, please try again {}", e);
        }

        logger.info("Finished");
        JobCenter.close();
    }

}
