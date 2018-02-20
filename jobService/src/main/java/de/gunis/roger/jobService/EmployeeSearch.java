package de.gunis.roger.jobService;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.calendar.ICalendarAccess;
import de.gunis.roger.jobService.exports.CalendarWriter;
import de.gunis.roger.jobService.imports.CsvFileLoader;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.workersAvailable.JobCenter;
import de.gunis.roger.jobService.workersAvailable.Worker;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EmployeeSearch {
    private static final Logger logger = LoggerFactory.getLogger("EmployeeSearch.class");

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(names = {"--help", "-h"}, description = "This help", help = true)
    private static boolean help = false;

    @Parameter(required = true, names = {"--holidays", "-hs"}, description = "File for Holidays <begin, end,event>")
    private String inputFilePathHolidays;

    @Parameter(required = true, names = {"--jobDescriptions", "-js"}, description = "File for Jobs <name,dayOfWeek (mo=1,...,sun=7),duration,begin,end,infoInDayOfWeek (off=0, mo=1,...,sun=7)>")
    private String inputFilePathJobDescriptions;

    @Parameter(required = true, names = {"--workers", "-ws"}, description = "File for Workers <name,jobA jobB..,Holiday (begin-end)>")
    private String inputFilePathWorkers;

    @Parameter(required = true, names = {"--outputFilePath", "-out"}, description = "Path for ics file created per JobDescription")
    private String outputFilePath = "";

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"--dateFormat", "-dateFormat"}, description = "Format of the Dates (begin|end|holiday) in all files [yyyy-MM-dd|dd.MM.yyyy|...]")
    private String dateFormat = "dd.MM.yyyy";

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"-log", "-loggingLevel"}, description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
    private String loggingLevel;

    private IntegerProperty amountOfWorkers = new SimpleIntegerProperty(0);
    private IntegerProperty amountOfJobDescriptions = new SimpleIntegerProperty(0);

    private IntegerProperty holidays = new SimpleIntegerProperty(0);

    private List<Worker> workers;
    private List<JobDescription> jobDescriptions;
    private Set<Holiday> holidaysFromFile;
    private CsvFileLoader csvFileLoader = new CsvFileLoader(dateFormat);

    EmployeeSearch() {
    }

    public static void main(String[] args) {

        EmployeeSearch main = new EmployeeSearch();
        JCommander jCommander = new JCommander(main, args);

        if (!ClearingHouse.askedForInstructions(jCommander, EmployeeSearch.help)) {
            main.runEmploymentAgency();
        }

    }


    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public void setInputFilePathHolidays(String inputFilePathHolidays) {
        if (inputFilePathHolidays != null) {
            this.inputFilePathHolidays = inputFilePathHolidays;
            holidaysFromFile = csvFileLoader.importHolidaysFromFile(inputFilePathHolidays);
        } else {
            this.inputFilePathHolidays = "";
        }

    }

    public void setInputFilePathJobDescriptions(String inputFilePathJobDescriptions) {
        if (inputFilePathJobDescriptions != null) {
            this.inputFilePathJobDescriptions = inputFilePathJobDescriptions;

            jobDescriptions = csvFileLoader.importJobDescriptionFromFile(inputFilePathJobDescriptions);
            this.amountOfJobDescriptions.set(jobDescriptions.size());
        } else {
            this.inputFilePathJobDescriptions = "";
            jobDescriptions = null;
            this.amountOfJobDescriptions.set(0);
        }
    }

    public void setInputFilePathWorkers(String inputFilePathWorkers) {
        if (inputFilePathWorkers != null) {
            this.inputFilePathWorkers = inputFilePathWorkers;

            workers = csvFileLoader.importWorkerFromFile(inputFilePathWorkers);
            this.amountOfWorkers.set(workers.size());
        } else {
            this.inputFilePathWorkers = "";
            workers = null;
            this.amountOfWorkers.set(0);
        }
    }

    public void setOutputFilePath(String outputFilePath) {
        if (outputFilePath != null) {
            this.outputFilePath = outputFilePath;
        } else {
            this.outputFilePath = "";
        }
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    void runEmploymentAgency() {

        setInputFilePathHolidays(inputFilePathHolidays);
        setInputFilePathJobDescriptions(inputFilePathJobDescriptions);
        setInputFilePathWorkers(inputFilePathWorkers);
        setOutputFilePath(outputFilePath);

        logger.info("starting with: {},\n {},\n {},\n export to {}", inputFilePathHolidays,
                inputFilePathWorkers, inputFilePathJobDescriptions, outputFilePath);
        ClearingHouse.setLoggingLevel(loggingLevel);

        JobCenter.open();
        csvFileLoader = new CsvFileLoader(dateFormat);

        HolidayInformationCenter.open();

        OptionalInt optionalMin = jobDescriptions.stream().mapToInt(job -> (int) job.getBegin().toEpochDay()).min();
        int startOffset = optionalMin.isPresent() ? optionalMin.getAsInt() : 0;

        OptionalLong optionalMax = jobDescriptions.stream().mapToLong(job -> job.getEnd().toEpochDay()).max();
        long endOffset = optionalMax.isPresent() ? optionalMax.getAsLong() : 0;

        String pdf_end = System.getProperty("PDF_END");

        if (null != pdf_end) {
            SimpleDateFormat simpleDateFormatter;
            simpleDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
            simpleDateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
            try {
                Date parse = simpleDateFormatter.parse(pdf_end);
                endOffset = parse.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        LocalDate beginOfJobSearch = LocalDate.ofEpochDay(startOffset);
        LocalDate endOfJobSearch = LocalDate.ofEpochDay(endOffset);

        logger.info("Searching, between {} -> {} (days: {})", beginOfJobSearch, endOfJobSearch, endOfJobSearch.toEpochDay() - beginOfJobSearch.toEpochDay());

        HolidayInformationCenter.instance().setHolidays(holidaysFromFile);
        JobCenter.instance().combineJobAndWorkerAndSubscribe(workers, jobDescriptions, beginOfJobSearch, endOfJobSearch);

        logger.debug("Exporting all calendar entries to {}", outputFilePath);

        CalendarWriter.documentJobsAndWorkers(jobDescriptions.stream().map(job -> (ICalendarAccess) job)
                .collect(Collectors.toList()), outputFilePath);

        CalendarWriter.documentJobsAndWorkers(workers.stream().map(worker -> (ICalendarAccess) worker)
                .collect(Collectors.toList()), outputFilePath);

        try {
            CalendarWriter.writeCalendar(JobCenter.instance().getAllCalendarEntries(),
                    Paths.get(outputFilePath, "allEvents.ics").toString());
        } catch (Exception e) {
            logger.error("Buggy Calender export, please try again {}", e);
        }

        logger.info("Finished");
        JobCenter.close();
        HolidayInformationCenter.close();

    }

    BooleanBinding hasEnoughInformations() {
        return amountOfJobDescriptions.greaterThan(0).and(amountOfWorkers.greaterThan(0));
    }

    void doPostProcessing(String execCommand) {

        String s;

        try {

            // run the Unix "ps -ef" command
            // using the Runtime exec method:
            Process p = Runtime.getRuntime().exec(execCommand);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            logger.info("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                logger.info(s);
            }

            // read any errors from the attempted command
            logger.info("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                logger.info(s);
            }

        } catch (IOException e) {
            logger.error("exception happened: " + e.getMessage());
        }
    }
}
