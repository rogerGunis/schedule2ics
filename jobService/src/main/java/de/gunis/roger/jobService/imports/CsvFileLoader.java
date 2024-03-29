package de.gunis.roger.jobService.imports;

import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.jobsToDo.Job;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.workersAvailable.Worker;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CsvFileLoader {
    private static final Logger logger = LoggerFactory.getLogger("CsvFileLoader.class");
    static Function<String, String> trimLine =
            line -> line.replaceAll("(,\\p{javaSpaceChar}+)|(\\p{javaSpaceChar}+,)", ",");

    static Function<String, String> trimDateRange =
            line -> line.replaceAll("(\\p{javaSpaceChar}+)-(\\p{javaSpaceChar}+)", "-");

    static DateTimeFormatter dateFormatter;

    static final Function<String, JobDescription> mapToJobDescription = line -> {
        String[] p = line.split(",");// a CSV has comma separated lines
        Set<DayOfWeek> dayOfWeeks = Arrays.stream(p[1].split(" "))
                .map(String::trim)
                .map(nr -> DayOfWeek.of(Integer.valueOf(nr)))
                .collect(Collectors.toSet());

        DayOfWeek info2DayOfWeek = null;
        Integer dayOfWeekInt = Integer.valueOf(p[5]);
        if (dayOfWeekInt > 0) {
            info2DayOfWeek = DayOfWeek.of(dayOfWeekInt);
        }

        boolean reminder = p[6].equals("0") ? Boolean.FALSE : Boolean.TRUE;
        String[] onEmptyWeeksCheckReasonOfHoliday = p[7].split("-");
        String endsWith = p[8];

        return new JobDescription(p[0], dayOfWeeks, Integer.parseInt(p[2]),
                dateOf(p[3]), dateOf(p[4]), info2DayOfWeek, reminder,
                new HashSet<>(Arrays.asList(onEmptyWeeksCheckReasonOfHoliday)), endsWith);
    };
    static final Function<String, Worker> mapToWorker = line -> {
//        int name = 0;
//        int jobs = 1;
//        int toggle = 2;
//        int holidays = 3;
        try {
            String[] p = line.split(",");// a CSV has comma separated lines

            Set<Job> jobs = Arrays.stream(p[1].split("\\s+")).map(job -> {
                if (job.matches(".*\\(.*")) {
                    String jobProposal = job;
                    jobProposal = jobProposal.replaceAll(".*\\(|\\)", "");
                    job = job.replaceAll("\\(.*", "");
                    return new Job(job, jobProposal);
                } else {
                    return new Job(job);
                }
            }).collect(Collectors.toSet());

            List<Holiday> vacations = null;
            List<Worker> group = null;
            String nameOfWorker = p[0];
            int toggleTurnus = Integer.parseInt(p[2]);
            if (p.length >= 4) {
                vacations = Arrays.stream(trimDateRange.apply(trimLine.apply(p[3])).split("\\s+"))
                    .map(possibleDates -> {
                        String[] dates = possibleDates.split("-");
                        if (dates.length == 1) {
                            String date = dates[0];
                            dates = new String[2];
                            dates[0] = date;
                            dates[1] = date;
                        }
                        return new Holiday(dateOf(dates[0]), dateOf(dates[1]), "Vacation: " + nameOfWorker);
                    })
                    .collect(Collectors.toList());
            }
            return new Worker(nameOfWorker, jobs, vacations, toggleTurnus);
        }
        catch (Exception e){
            logger.error("ERROR on line {}", line);
            throw new RuntimeException(e);
        }

    };
    static final Function<String, Holiday> mapToHoliday = line -> {
        String[] p = line.split(",");
        String name = "0";
        if (p.length >= 4) {
            name = p[3];
        }

        return new Holiday(dateOf(p[0]), dateOf(p[1]), p[2], Boolean.getBoolean(name));
    };

    public CsvFileLoader(String datePattern) {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    private static LocalDate dateOf(String date) {
        return LocalDate.parse(date, dateFormatter);
    }

    public List<Worker> importWorkerFromFile(String inputFilePath) {
        logger.info("Loading {}", inputFilePath);
        List<Worker> inputList = new ArrayList<>();
        try (
            InputStream inputFS = new FileInputStream(inputFilePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS))
        ) {
            // skip the header of the csv
            inputList = streamWithoutHeader(br).map(mapToWorker).collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("Exception" + e);
        }
        return inputList;
    }

    public List<JobDescription> importJobDescriptionFromFile(String inputFilePath) {
        List<JobDescription> inputList = new ArrayList<>();
        try (
            InputStream inputFS = new FileInputStream(inputFilePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS))
        ) {
            // skip the header of the csv
            inputList = streamWithoutHeader(br).map(mapToJobDescription).collect(Collectors.toList());
            br.close();
        } catch (IOException e) {
            logger.warn("Exception" + e);
        }
        return inputList;
    }

    public Set<Holiday> importHolidaysFromFile(String inputFilePath) {
        Set<Holiday> inputList = new HashSet<>();
        try (
                InputStream inputFS = new FileInputStream(new File(inputFilePath));
                BufferedReader br = new BufferedReader(new InputStreamReader(inputFS))
        ) {
            // skip the header of the csv
            inputList = streamWithoutHeader(br).map(mapToHoliday).collect(Collectors.toSet());
            br.close();
        } catch (IOException e) {
            logger.warn("Exception" + e);
        }
        return inputList;
    }

    private Stream<String> streamWithoutHeader(BufferedReader br) {
        return br.lines()
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .skip(1)
                .map(trimLine);
    }
}
