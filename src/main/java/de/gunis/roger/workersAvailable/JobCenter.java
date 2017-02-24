package de.gunis.roger.workersAvailable;

import de.gunis.roger.jobsToDo.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobCenter {

    private static final Logger logger = LoggerFactory.getLogger("JobCenter.class");

    private static volatile JobCenter instance;
    Map<Worker, Set<Job>> workerToJobs = new HashMap<>();
    Map<Job, List<Worker>> jobToWorker = new HashMap<>();

    public static synchronized JobCenter start() {
        if (instance != null) {
            throw new RuntimeException("start called, but already started");
        }
        instance = new JobCenter();
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

        jobs.stream().forEach(job -> {
                    List<Worker> workers = jobToWorker.getOrDefault(job, Stream.of(worker).collect(Collectors.toList()));
                    if (!workers.contains(worker)) {
                        workers.add(worker);
                    }
                    jobToWorker.put(job, workers);
                }
        );

    }

    public Worker getWorkerForJob(LocalDate day, Job job) {
        Optional<Worker> maybeWorker = jobToWorker.getOrDefault(job, Collections.emptyList())
                .stream()
//                .filter(worker -> worker.isAvailable(day))
                .filter(worker -> !worker.hasJobDone(job))
                .min(Comparator.comparing(worker -> worker.hasJobDone(job)));

        if (maybeWorker.isPresent()) {
            if (!maybeWorker.get().isAvailable(day)) {
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
                    .stream().filter(worker -> worker.isAvailable(day)).findFirst().isPresent()) {
                logger.info("Round over, starting over next one");
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

        return maybeWorker.orElse(null);
    }

    public void addWorkers(List<Worker> workers) {
        workers.stream().forEach(this::addWorker);
    }
}
