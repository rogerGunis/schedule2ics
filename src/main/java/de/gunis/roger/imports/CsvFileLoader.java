package de.gunis.roger.imports;

import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.jobsToDo.Job;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.Worker;
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

@Slf4j
public class CsvFileLoader {
    private static final Logger logger = LoggerFactory.getLogger("CsvFileLoader.class");
    static Function<String, String> trimLine =
            line -> line.replaceAll("(,\\p{javaSpaceChar}+)|(\\p{javaSpaceChar}+,)", ",");
    static DateTimeFormatter dateFormatter;
    static final Function<String, JobDescription> mapToJobDescription = line -> {
        String[] p = line.split(",");// a CSV has comma separated lines
        Set<DayOfWeek> dayOfWeeks = Arrays.stream(p[1].split(" "))
                .map(String::trim)
                .map(nr -> DayOfWeek.of(Integer.valueOf(nr)))
                .collect(Collectors.toSet());

        DayOfWeek info2DayOfWeek = null;
        if (p.length == 6) {
            Integer dayOfWeekInt = Integer.valueOf(p[5]);
            if (dayOfWeekInt > 0) {
                info2DayOfWeek = DayOfWeek.of(dayOfWeekInt);
            }
        }

        return new JobDescription(p[0], dayOfWeeks, Integer.parseInt(p[2]),
                LocalDate.parse(p[3], dateFormatter), LocalDate.parse(p[4], dateFormatter),
                info2DayOfWeek);
    };
    private static final Function<String, Holiday> mapToHoliday = line -> {
        String[] p = line.split(",");

        return new Holiday(LocalDate.parse(p[0], dateFormatter), LocalDate.parse(p[1], dateFormatter), p[2]);
    };
    static final Function<String, Worker> mapToWorker = line -> {
        String[] p = line.split(",");// a CSV has comma separated lines

        Set<Job> jobs = Arrays.stream(p[1].split("\\s+")).map(Job::new).collect(Collectors.toSet());
        List<LocalDate> vacations = null;
        if (p.length == 3) {
            vacations = Arrays.stream(p[2].split("\\s+"))
                    .map(date -> LocalDate.parse(date, dateFormatter))
                    .collect(Collectors.toList());
        }

        return new Worker(p[0], jobs, vacations);
    };

    public CsvFileLoader(String datePattern) {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    public List<Worker> importWorkerFromFile(String inputFilePath) {
        logger.info("Loading {}", inputFilePath);
        List<Worker> inputList = new ArrayList<>();
        try (
                InputStream inputFS = new FileInputStream(new File(inputFilePath));
                BufferedReader br = new BufferedReader(new InputStreamReader(inputFS))
        ) {
            // skip the header of the csv
            inputList = br.lines().skip(1).map(mapToWorker).collect(Collectors.toList());
            br.close();
        } catch (IOException e) {
            logger.warn("Exception" + e);
        }
        return inputList;
    }

    public List<JobDescription> importJobDescriptionFromFile(String inputFilePath) {
        List<JobDescription> inputList = new ArrayList<>();
        try (
                InputStream inputFS = new FileInputStream(new File(inputFilePath));
                BufferedReader br = new BufferedReader(new InputStreamReader(inputFS))
        ) {
            // skip the header of the csv
            inputList = br.lines().skip(1).map(trimLine).map(mapToJobDescription).collect(Collectors.toList());
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
            inputList = br.lines().skip(1).map(trimLine).map(mapToHoliday).collect(Collectors.toSet());
            br.close();
        } catch (IOException e) {
            logger.warn("Exception" + e);
        }
        return inputList;
    }
}
