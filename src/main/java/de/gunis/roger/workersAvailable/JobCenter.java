package de.gunis.roger.workersAvailable;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.jobsToDo.LaborMarket;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobCenter {

    private static final Logger logger = LoggerFactory.getLogger("JobCenter.class");
    private static volatile JobCenter instance;

    private Map<Worker, Set<Job>> workerToJobs = new HashMap<>();
    private Map<Job, List<Worker>> jobToWorker = new HashMap<>();
    private Calendar allCalendarEntries;

    private JobCenter(Calendar allCalendarEntries) {
        this.allCalendarEntries = allCalendarEntries;
    }

    public static synchronized JobCenter start() {
        if (instance != null) {
            throw new RuntimeException("start called, but already started");
        }
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//allEvents//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        instance = new JobCenter(calendar);
        logger.info("Instance started");

        return instance;
    }

    public static synchronized void stop() {
        instance = null;
        logger.info("Instance stopped");
    }

    public static JobCenter instance() {
        if (instance == null) {
            throw new RuntimeException("JobCenter is closed");
        }
        return instance;
    }

    public void addWorker(Worker worker) {
        Set<Job> jobsOfWorker = worker.getJobs();
        Set<Job> jobs = workerToJobs.getOrDefault(worker, jobsOfWorker);
        jobs.addAll(jobsOfWorker);
        workerToJobs.put(worker, jobs);

        jobs.forEach(job -> {
                    List<Worker> workers = jobToWorker.getOrDefault(job, Stream.of(worker).collect(Collectors.toList()));
                    if (!workers.contains(worker)) {
                        workers.add(worker);
                    }
                    jobToWorker.put(job, workers);
                }
        );

    }

    public Worker getWorkerForJob(LocalDate day, Job job) {
        logger.trace("starting worker search");
        Optional<Worker> maybeWorker = jobToWorker.getOrDefault(job, Collections.emptyList())
                .stream()
                .filter(worker -> !worker.hasJobDone(job))
                .min(Comparator.comparing(worker -> worker.hasJobDone(job)));

        if (maybeWorker.isPresent()) {
            if (maybeWorker.get().isOnHoliday(day)) {
                // on vacation, mark done and ask again (keep same order of list)
                // some kind of business logic
                maybeWorker.get().doJob(job);
                return getWorkerForJob(day, job);
            } else {
                maybeWorker.get().doJob(job);
            }
        } else {
            if (jobToWorker.getOrDefault(job, Collections.emptyList()).stream().findAny().isPresent()
                    && jobToWorker.getOrDefault(job, Collections.emptyList())
                    .stream().noneMatch(worker -> worker.isOnHoliday(day))) {
                logger.info("Round over, starting over next one: {}", job);
                jobToWorker.get(job).forEach(worker -> worker.resetJobDone(job));
                return getWorkerForJob(day, job);
            } else {
                try {
                    throw new IllegalJobException("No workers found for this job or all on vacation");
                } catch (IllegalJobException e) {
                    logger.error("" + e + ", " + job);
                }
            }

        }

        logger.trace("finished worker search");
        return maybeWorker.orElse(null);
    }

    public void addWorkers(List<Worker> workers) {
        workers.forEach(this::addWorker);
    }


    public Calendar getAllCalendarEntries() {
        return allCalendarEntries;
    }

    public void combineJobAndWorkerAndRegisterOnDescription(Set<Holiday> holidays,
                                                            List<Worker> workers,
                                                            List<JobDescription> jobDescriptions,
                                                            LocalDate myDay,
                                                            LocalDate endDay) {
        LaborMarket laborMarket = new LaborMarket(jobDescriptions, holidays);
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

            LocalDate finalMyDay = myDay;
            jobQueue.parallelStream().forEach(jobDescription -> {
                String jobDescriptionName = jobDescription.getName();
                Worker foundWorker = jobCenter.getWorkerForJob(finalMyDay, new Job(jobDescriptionName));
                logger.trace("Found {} for {} @ {}", foundWorker, jobDescriptionName, finalMyDay);
                VEvent vEvent = jobDescription.registerWorkerOnDate(finalMyDay, foundWorker);
                allCalendarEntries.getComponents().add(vEvent);

                logger.trace("registration done: {} {} @ {}", finalMyDay, foundWorker, jobDescriptionName);
            });

            myDay = myDay.plusDays(1L);
        }
    }
}
