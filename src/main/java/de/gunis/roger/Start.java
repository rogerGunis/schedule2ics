package de.gunis.roger;

import ch.qos.logback.classic.Level;
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
import java.time.Month;
import java.util.List;
import java.util.Set;

@Slf4j
public class Start {
    private static final Logger logger = LoggerFactory.getLogger("Start.class");


    private Start() {
    }

    public static void main(String[] args) {
        logger.info("starting");
        setLoggingLevel(ch.qos.logback.classic.Level.TRACE);

        JobCenter jobCenter = JobCenter.start();

        Set<Holiday> holidays = CsvFileLoader.importHolidaysFromFile("/home/vagrant/scheduler2ics/internal/Holidays.csv");

        List<Worker> workers = CsvFileLoader.importWorkerFromFile("/home/vagrant/scheduler2ics/internal/Workers.csv");
        List<JobDescription> jobDescriptions = CsvFileLoader.importJobDescriptionFromFile("/home/vagrant/scheduler2ics/internal/JobDescription.csv");

        // fetch the day of the week and check for workday
        LocalDate day = LocalDate.of(2017, Month.JANUARY, 11);
        LocalDate myDay = day;
        LocalDate endDay = day.plusDays(30);

        combineJobAndWorkerAndRegisterOnDescription(holidays, workers, jobDescriptions, myDay, endDay);

        writeIcsFile(jobDescriptions);


        logger.info("Finished");
        jobCenter.stop();
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
                Worker foundWorker = jobCenter.getWorkerForJob(finalMyDay, new Job(jobDescription.getName()));
                logger.trace("Found {} for {} @ {}", foundWorker, jobDescription.getName(), finalMyDay);
                jobDescription.registerWorkerOnDate(finalMyDay, foundWorker);
                logger.trace("registering done");
            });

            myDay = myDay.plusDays(1L);
        }
    }

    private static void setLoggingLevel(ch.qos.logback.classic.Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);

        // we suppress calendar TRACE level, because not needed for me
        ch.qos.logback.classic.Logger cal = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("net.fortuna.ical4j.data.FoldingWriter");
        cal.setLevel(Level.DEBUG);
    }

}
