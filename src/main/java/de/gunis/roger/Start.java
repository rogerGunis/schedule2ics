package de.gunis.roger;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.imports.CsvFileLoader;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.jobsToDo.LaborMarket;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import groovy.util.logging.Slf4j;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
public class Start {
    private static final Logger logger = LoggerFactory.getLogger("Start.class");

    @Parameter(required = true, names = {"--holidays", "-hs"}, description = "File for Holidays <YYYYMMDD,event>")
    private String inputFilePathHolidays;

    @Parameter(required = true, names = {"--workers", "-ws"}, description = "File for Workers <Name,JobA JobB..,Holidays>")
    private String inputFilePathWorkers;

    @Parameter(required = true, names = {"--jobDescriptions", "-js"}, description = "File for Jobs <name,dayOfWeek (mo=1,...,sun=7),duration,begin,end>")
    private String inputFilePathJobDescriptions;

    @Parameter(names = {"-log", "-verbose"}, description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
    private String verbose;

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help = false;

    private Start() {
    }

    public static void main(String[] args) {

        Start main = new Start();
        JCommander jCommander = new JCommander(main, args);
        main.run(jCommander);

    }

    static void writeIcsFile(List<JobDescription> jobDescriptions) {
        jobDescriptions.stream().forEach(jobDescription -> {
                    Calendar calendar = jobDescription.getCalendar();
                    String name = jobDescription.getName();
                    try (
                            FileOutputStream fout = new FileOutputStream(Paths.get("/tmp/" + name + ".ics").toFile())
                    ) {

                        CalendarOutputter outputter = new CalendarOutputter();
                        outputter.setValidating(false);
                        outputter.output(calendar, fout);

                    } catch (IOException e) {
                        logger.warn("Exception: " + e);
                    }
                }
        );
    }

    static void combineJobAndWorkerAndRegisterOnDescription(Set<Holiday> holidays,
                                                            List<Worker> workers,
                                                            List<JobDescription> jobDescriptions,
                                                            LocalDate myDay,
                                                            LocalDate endDay) {
        LaborMarket laborMarket = new LaborMarket(jobDescriptions);
        JobCenter jobCenter = JobCenter.instance();
        jobCenter.addWorkers(workers);

        while (!myDay.isEqual(endDay)) {
            logger.debug("Day: {}", myDay.toString());

            List<JobDescription> jobQueue = laborMarket.getJobDescriptions(myDay);
            if (jobQueue.isEmpty()) {
                logger.debug("No work for day: {}", myDay.toString());
                myDay = myDay.plusDays(1L);
                continue;
            }

            if (holidays.contains(myDay)) {
                logger.debug("Found holiday: {}", myDay.toString());
                myDay = myDay.plusDays(1L);
                continue;
            }

            // foreach necessary pool get person to do this
            LocalDate finalMyDay = myDay;
            jobQueue.parallelStream().forEach(jobDescription -> {
                String jobDescriptionName = jobDescription.getName();
                Worker foundWorker = jobCenter.getWorkerForJob(finalMyDay, new Job(jobDescriptionName));
                logger.trace("Found {} for {} @ {}", foundWorker, jobDescriptionName, finalMyDay);
                jobDescription.registerWorkerOnDate(finalMyDay, foundWorker);
                logger.trace("registration done: {} {} @ {}", finalMyDay, foundWorker, jobDescriptionName);
            });

            myDay = myDay.plusDays(1L);
        }
    }

    private static void setLoggingLevel(String level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(level, Level.DEBUG));

        // we suppress calendar TRACE level, because not needed for me
        ch.qos.logback.classic.Logger cal = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("net.fortuna.ical4j.data.FoldingWriter");
        cal.setLevel(Level.DEBUG);
    }

    private void run(JCommander jCommander) {

        if (help) {
            jCommander.usage();
            System.exit(2);
        }

        logger.info("starting with: {}, {}, {}", inputFilePathHolidays, inputFilePathWorkers, inputFilePathJobDescriptions);
        setLoggingLevel(verbose);

        JobCenter jobCenter = JobCenter.start();

        Set<Holiday> holidays = CsvFileLoader.importHolidaysFromFile(inputFilePathHolidays);
        List<Worker> workers = CsvFileLoader.importWorkerFromFile(inputFilePathWorkers);
        List<JobDescription> jobDescriptions = CsvFileLoader.importJobDescriptionFromFile(inputFilePathJobDescriptions);

        int startOffset = jobDescriptions.stream().mapToInt(job -> (int) job.getBegin().toEpochDay()).min().getAsInt();
        int endOffset = jobDescriptions.stream().mapToInt(job -> (int) job.getEnd().toEpochDay()).max().getAsInt();

        LocalDate myDay = LocalDate.ofEpochDay(startOffset);
        LocalDate endDay = LocalDate.ofEpochDay(endOffset);

        logger.info("Searching, between {} -> {} (days: {})", myDay, endDay, endDay.toEpochDay() - myDay.toEpochDay());
        combineJobAndWorkerAndRegisterOnDescription(holidays, workers, jobDescriptions, myDay, endDay);

        writeIcsFile(jobDescriptions);


        logger.info("Finished");
        jobCenter.stop();
    }

}
