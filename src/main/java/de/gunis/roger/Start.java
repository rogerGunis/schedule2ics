package de.gunis.roger;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import de.gunis.roger.imports.CsvFileLoader;
import de.gunis.roger.jobsToDo.LaborMarket;
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
        JobCenter jobCenter = JobCenter.start();

        Set<Holiday> holidays = CsvFileLoader.importHolidaysFromFile("asdfsadf");

        List<Worker> workers = CsvFileLoader.importWorkerFromFile("blubber");
        List<JobDescription> jobDescriptions = CsvFileLoader.importJobDescriptionFromFile("blubber");

        // fetch the day of the week and check for workday
        LocalDate myDay = LocalDate.of(2017, Month.MARCH, 27);
        LocalDate endDay = LocalDate.of(2017, Month.MARCH, 29);

        combineJobAndWorker(jobCenter, holidays, workers, jobDescriptions, myDay, endDay);

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


        logger.info("Finished");
        jobCenter.stop();
    }

    static void combineJobAndWorker(JobCenter jobCenter,
                                    Set<Holiday> holidays,
                                    List<Worker> workers,
                                    List<JobDescription> jobDescriptions,
                                    LocalDate myDay,
                                    LocalDate endDay) {
        LaborMarket laborMarket = new LaborMarket(jobDescriptions);
        jobCenter.addWorkers(workers);

        while (!myDay.isEqual(endDay)) {

            List<JobDescription> jobQueue = laborMarket.getJobDescriptions(myDay);
            if (jobQueue.isEmpty()) {
                logger.info("No work for day: %s", myDay);
                continue;
            }

            if (holidays.stream().filter(holiday -> holiday.match(myDay)).findFirst().isPresent()) {
                logger.debug("Found holiday: %s ", myDay);
                myDay.plusDays(1L);
                continue;
            }

            // foreach necessary pool get person to do this
            for (JobDescription jobDescription : jobQueue) {
                Worker foundWorker = jobCenter.getWorkerForJob(myDay, new Job(jobDescription.getName()));
                jobDescription.registerEvent(myDay, foundWorker);
            }

            myDay.plusDays(1L);
        }
    }


}
