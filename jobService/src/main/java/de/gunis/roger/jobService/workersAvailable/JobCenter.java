package de.gunis.roger.jobService.workersAvailable;

import de.gunis.roger.jobService.ClearingHouse;
import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.jobsToDo.Job;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.jobsToDo.LaborMarket;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JobCenter {

    private static final Logger logger = LoggerFactory.getLogger("JobCenter.class");
    private static volatile JobCenter instance;

    private Map<Worker, Set<Job>> workerToJobs = new HashMap<>();
    private Map<Job, List<Worker>> jobToWorker = new HashMap<>();
    private Map<String, Integer> roundOfJobsCounter = new HashMap<>();
    private Map<Job, Worker> firstWorkerForReoundIteration = new HashMap<>();
    private Calendar allCalendarEntries;
    private int JOB_CENTER_OPENED = Integer.MAX_VALUE;

    private JobCenter(Calendar allCalendarEntries) {
        this.allCalendarEntries = allCalendarEntries;
    }

    public static synchronized JobCenter open() {
        if (instance != null) {
            throw new RuntimeException(JobCenter.class.getSimpleName() + " is already open, terminating");
        }
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//allEvents//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);


        instance = new JobCenter(calendar);
        logger.info("Instance started");

        return instance;
    }

    public static synchronized void close() {
        instance = null;
        logger.info("JobCenter closed");
    }

    public static JobCenter instance() {
        if (instance == null) {
            throw new RuntimeException("JobCenter is closed");
        }
        return instance;
    }

    void addWorker(Worker worker) {
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

    Worker getWorkerForJob(LocalDate day, JobDescription jobDescription) {
        logger.trace("starting worker search");
        Job job = new Job(jobDescription.getName());
        Integer startOfJobCenter = roundOfJobsCounter.getOrDefault(jobDescription.getName(), JOB_CENTER_OPENED);

        List<Worker> workers = jobToWorker.getOrDefault(job, Collections.emptyList());
        firstWorkerForReoundIteration.putIfAbsent(job, workers.get(0));
        roundOfJobsCounter.putIfAbsent(jobDescription.getName(), 1);

        int startWithIx = 0;
        if (workerToBeShuffled(startOfJobCenter)) {
            OptionalInt indexOpt = IntStream.range(0, workers.size())
                    .filter(i -> jobDescription.getEndsWithWorker().equals(workers.get(i).name))
                    .findFirst();
            if (startWithIx == workers.size() || !indexOpt.isPresent()) {
                startWithIx = 0;
            } else {
                startWithIx = indexOpt.getAsInt() + 1;
            }
            Collections.rotate(workers, (workers.size() - startWithIx));

            jobToWorker.put(job, workers);
        }

        Optional<Worker> maybeWorker = workers
                .stream()
                .filter(worker -> !worker.hasJobDone(job))
                .min(Comparator.comparing(worker -> worker.hasJobDone(job)));

        logger.info("Round over, starting over next one: {}", job);
        if (maybeWorker.isPresent()) {
            if (maybeWorker.get().getName().equals(firstWorkerForReoundIteration.get(job).getName())) {
                roundOfJobsCounter.compute(jobDescription.getName(), (k, v) -> v == null ? 1 : v + 1);
            }
        }
        if (maybeWorker.isPresent()) {
            if (maybeWorker.get().isOnHoliday(day) || (maybeWorker.get().hasToggledState() == 1 && roundOfJobsCounter.get(jobDescription.getName()) % 2 == 0)) {
                // on vacation, mark done and ask again (keep same order of list)
                // some kind of business logic
                maybeWorker.get().doJob(job);

                return getWorkerForJob(day, jobDescription);
            } else {
                maybeWorker.get().doJob(job);
            }
        } else {
            if (workers.stream().findAny().isPresent()
                    && workers
                    .stream().anyMatch(worker -> !worker.isOnHoliday(day))) {

                jobToWorker.get(job).forEach(worker -> worker.resetJobDone(job));

                return getWorkerForJob(day, jobDescription);
            } else {
                try {
                    throw new IllegalJobException("No workers found for this job day: " + day + " or all on vacation");
                } catch (IllegalJobException e) {
                    logger.error("" + e + ", " + job);
                }
            }

        }

        logger.trace("finished worker search");
        return maybeWorker.orElse(null);
    }

    private boolean workerToBeShuffled(Integer startOfJobCenter) {
        return startOfJobCenter == JOB_CENTER_OPENED;
    }

    private void addWorkers(List<Worker> workers) {
        workers.forEach(this::addWorker);
    }


    public Calendar getAllCalendarEntries() {
        return allCalendarEntries;
    }

    public void combineJobAndWorkerAndSubscribe(List<Worker> workers,
                                                List<JobDescription> jobDescriptions,
                                                LocalDate myDay,
                                                LocalDate endDay) {

        LaborMarket laborMarket = new LaborMarket(jobDescriptions);
        JobCenter jobCenter = JobCenter.instance();
        jobCenter.addWorkers(workers);

        Set<Holiday> holidayAlreadyAddressed = new HashSet<>();
        while (!myDay.isEqual(endDay.plusDays(1L))) {
            logger.debug("Day: {}", myDay.toString());

            LocalDate finalMyDay1 = myDay;
            Optional<Holiday> mayBeHoliday = HolidayInformationCenter.instance().getHolidays().parallelStream().filter(holiday -> holiday.isWithinRange(finalMyDay1)).findFirst();
            List<JobDescription> jobQueue = laborMarket.getJobDescriptions(myDay, mayBeHoliday);

            if (mayBeHoliday.isPresent() && !holidayAlreadyAddressed.contains(mayBeHoliday.get())) {
                Holiday foundHoliday = mayBeHoliday.get();
                logger.info("Found holiday {} adding to calendar", foundHoliday);
                long jobDate = myDay.toEpochDay() * 86400 * 1000;
                String foundHolidayName = foundHoliday.getName();
                VEvent vEvent = new VEvent(new net.fortuna.ical4j.model.Date(jobDate),
                        foundHoliday.getDuration(), foundHolidayName);
                vEvent.getProperties().add(new Uid(String.valueOf(myDay.format(ClearingHouse.dateTimeFormatter)) + "_" + foundHolidayName));


                Categories holiday = new Categories("Holiday");
                vEvent.getProperties().add(holiday);

                allCalendarEntries.getComponents().add(vEvent);

                holidayAlreadyAddressed.add(foundHoliday);
            }

            if (jobQueue.isEmpty()) {
                logger.debug("No work for day: {}", myDay.toString());

                myDay = myDay.plusDays(1L);
                continue;
            }

            LocalDate finalMyDay = myDay;
            jobQueue.parallelStream().forEach(jobDescription -> {
                String jobDescriptionName = jobDescription.getName();
                Worker foundWorker = jobCenter.getWorkerForJob(finalMyDay, jobDescription);
                logger.trace("Found {} for {} @ {}", foundWorker, jobDescriptionName, finalMyDay);

                VEvent vEvent = jobDescription.registerWorkerOnDate(finalMyDay, foundWorker);

                String category = jobDescriptionName + "Round-" + roundOfJobsCounter.get(jobDescription.getName());
                Categories roundInfoAsCategory = new Categories(category);
                vEvent.getProperties().add(roundInfoAsCategory);

                allCalendarEntries.getComponents().add(vEvent);

                logger.trace("registration done: {} {} @ {}", finalMyDay, foundWorker, jobDescriptionName);
            });

            myDay = myDay.plusDays(1L);
        }
    }
}
