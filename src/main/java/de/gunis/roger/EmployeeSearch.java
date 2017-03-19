package de.gunis.roger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gunis.roger.calendar.Holiday;
import de.gunis.roger.calendar.ICalendarAccess;
import de.gunis.roger.exports.CalendarWriter;
import de.gunis.roger.imports.CsvFileLoader;
import de.gunis.roger.jobsToDo.JobDescription;
import de.gunis.roger.workersAvailable.JobCenter;
import de.gunis.roger.workersAvailable.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
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
    private String outputFilePath;

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"--dateFormat", "-dateFormat"}, description = "Format of the Dates (begin|end|holiday) in all files [yyyy-MM-dd|dd.MM.yyyy|...]")
    private String dateFormat = "dd.MM.yyyy";

    @SuppressWarnings("unused FieldCanBeLocal")
    @Parameter(names = {"-log", "-loggingLevel"}, description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
    private String loggingLevel;

    EmployeeSearch() {
    }

    public static void main(String[] args) {

        EmployeeSearch main = new EmployeeSearch();
        JCommander jCommander = new JCommander(main, args);

        if (!askedForInstructions(jCommander)) {
            main.runEmploymentAgency();
        }

    }


    private static boolean askedForInstructions(JCommander jCommander) {
        if (help) {
            jCommander.usage();
            logger.info("-------------\nAll elements marked with * are required\n-------------\n");
            return true;
        } else {
            return false;
        }
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public void setInputFilePathHolidays(String inputFilePathHolidays) {
        this.inputFilePathHolidays = inputFilePathHolidays;
    }

    public void setInputFilePathJobDescriptions(String inputFilePathJobDescriptions) {
        this.inputFilePathJobDescriptions = inputFilePathJobDescriptions;
    }

    public void setInputFilePathWorkers(String inputFilePathWorkers) {
        this.inputFilePathWorkers = inputFilePathWorkers;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    void runEmploymentAgency() {


        logger.info("starting with: {},\n {},\n {},\n export to {}", inputFilePathHolidays,
                inputFilePathWorkers, inputFilePathJobDescriptions, outputFilePath);
        ClearingHouse.setLoggingLevel(loggingLevel);

        JobCenter.open();
        CsvFileLoader csvFileLoader = new CsvFileLoader(dateFormat);

        Set<Holiday> holidays = csvFileLoader.importHolidaysFromFile(inputFilePathHolidays);
        List<Worker> workers = csvFileLoader.importWorkerFromFile(inputFilePathWorkers);
        List<JobDescription> jobDescriptions = csvFileLoader.importJobDescriptionFromFile(inputFilePathJobDescriptions);

        OptionalInt optionalMin = jobDescriptions.stream().mapToInt(job -> (int) job.getBegin().toEpochDay()).min();
        int startOffset = optionalMin.isPresent() ? optionalMin.getAsInt() : 0;

        OptionalInt optionalMax = jobDescriptions.stream().mapToInt(job -> (int) job.getEnd().toEpochDay()).max();
        int endOffset = optionalMax.isPresent() ? optionalMax.getAsInt() : 0;

        LocalDate beginOfJobSearch = LocalDate.ofEpochDay(startOffset);
        LocalDate endOfJobSearch = LocalDate.ofEpochDay(endOffset);

        logger.info("Searching, between {} -> {} (days: {})", beginOfJobSearch, endOfJobSearch, endOfJobSearch.toEpochDay() - beginOfJobSearch.toEpochDay());

        JobCenter.instance().combineJobAndWorkerAndSubscribe(holidays, workers, jobDescriptions, beginOfJobSearch, endOfJobSearch);

        logger.debug("Exporting all calendar entries to {}", outputFilePath);

        CalendarWriter.documentJobsAndWork(jobDescriptions.stream().map(job -> (ICalendarAccess) job)
                .collect(Collectors.toList()), outputFilePath);
        CalendarWriter.documentJobsAndWork(workers.stream().map(worker -> (ICalendarAccess) worker)
                .collect(Collectors.toList()), outputFilePath);

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
