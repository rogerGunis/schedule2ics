package de.gunis.roger.jobService;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.gunis.roger.jobService.calendar.Holiday;
import de.gunis.roger.jobService.calendar.HolidayInformationCenter;
import de.gunis.roger.jobService.calendar.ICalendarAccess;
import de.gunis.roger.jobService.exports.CalendarWriter;
import de.gunis.roger.jobService.exports.FileExtractorFromJar;
import de.gunis.roger.jobService.imports.CsvFileLoader;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import de.gunis.roger.jobService.workersAvailable.JobCenter;
import de.gunis.roger.jobService.workersAvailable.Worker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class EmployeeSearch {
  private static final Logger logger = LoggerFactory.getLogger("EmployeeSearch.class");

  private static final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
      (t, e) -> {
        logger.error("got exception from Thread " + t + ". Going to exit.", e);
        System.exit(1);

      };

  @SuppressWarnings("FieldCanBeLocal")
  @Parameter(names = {"--help", "-h"},
      description = "This help", help = true)
  private static boolean help = false;
  @SuppressWarnings("unused FieldCanBeLocal")
  @Parameter(names = {"-withP", "--withPostprocessing"},
      description = "generate postprocessing files")
  private static Boolean withPostprocessing;
  @Parameter(required = true, names = {"--holidays", "-hs"},
      description = "File for Holidays <begin, end,event>")
  private String inputFilePathHolidays;
  @Parameter(required = true, names = {"--jobDescriptions", "-js"},
      description = "File for Jobs <name,dayOfWeek (mo=1,...,sun=7)," +
          "duration,begin,end,infoInDayOfWeek (off=0, mo=1,...,sun=7)>")
  private String inputFilePathJobDescriptions;
  @Parameter(required = true, names = {"--workers", "-ws"},
      description = "File for Workers <name,jobA jobB..,Holiday (begin-end)>")
  private String inputFilePathWorkers;
  @Parameter(required = true, names = {"--outputFilePath", "-out"},
      description = "Path for ics file created per JobDescription")
  private String outputFilePath = "";
  @SuppressWarnings("unused FieldCanBeLocal")
  @Parameter(names = {"--dateFormat", "-dateFormat"},
      description = "Format of the Dates (begin|end|holiday) in all files [yyyy-MM-dd|dd.MM.yyyy|...]")
  private String dateFormat = "dd.MM.yyyy";
  @SuppressWarnings("unused FieldCanBeLocal")
  @Parameter(names = {"-log", "-loggingLevel"},
      description = "Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]")
  private String loggingLevel;
  private List<Worker> workers;
  private List<JobDescription> jobDescriptions;
  private Set<Holiday> holidaysFromFile;
  private CsvFileLoader csvFileLoader = new CsvFileLoader(dateFormat);

  EmployeeSearch() {


  }

  public static void main(String[] args) throws URISyntaxException, IOException {

    Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

    EmployeeSearch main = new EmployeeSearch();
    JCommander jCommander = new JCommander(main, args);

    if (!ClearingHouse.askedForInstructions(jCommander, EmployeeSearch.help)) {
      main.runEmploymentAgency();
    }

    if (EmployeeSearch.withPostprocessing) {
      main.withPostprocessing();
    }

  }

  private static void createDirectory(String scheduleTestDir) {
    File dir = new File(scheduleTestDir);

    // attempt to create the directory here
    boolean successful = dir.mkdir();
    if (successful) {
      // creating the directory succeeded
      logger.info("directory was created successfully");
    } else {
      // creating the directory failed
      logger.info("failed trying to create the directory");
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
    } else {
      this.inputFilePathJobDescriptions = "";
      jobDescriptions = null;
    }
  }

  public void setInputFilePathWorkers(String inputFilePathWorkers) {
    if (inputFilePathWorkers != null) {
      this.inputFilePathWorkers = inputFilePathWorkers;

      workers = csvFileLoader.importWorkerFromFile(inputFilePathWorkers);
    } else {
      this.inputFilePathWorkers = "";
      workers = null;
    }
  }

  public void setOutputFilePath(String outputFilePath) {
    this.outputFilePath = outputFilePath;
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

    OptionalLong optionalMin = jobDescriptions.stream()
        .mapToLong(job -> job.getBegin().atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant().getEpochSecond()).min();
    long startOfInterval = optionalMin.isPresent() ? optionalMin.getAsLong() : 0L;

    OptionalLong optionalMax = jobDescriptions.stream()
        .mapToLong(job -> job.getEnd().atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant().getEpochSecond()).max();
    long endOfInterval = optionalMax.isPresent() ? optionalMax.getAsLong() : 0L;

    String pdfStart = System.getProperty("PDF_START");
    String pdfEnd = System.getProperty("PDF_END");

    LocalDate startOfReports = getDatesFromString(pdfStart, startOfInterval);
    LocalDate endOfReports = getDatesFromString(pdfEnd, endOfInterval + 60 * 60 * 24 + 1);

    Pair<LocalDate, LocalDate> reportRange = new ImmutablePair(startOfReports, endOfReports);
    LocalDate beginOfJobSearch =
        Instant.ofEpochMilli(startOfInterval * 1000).atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
    LocalDate endOfJobSearch = Instant.ofEpochMilli(endOfInterval * 1000)
        .atZone(ZoneId.of("Europe/Berlin")).toLocalDate();

    logger.info("Searching, between {} -> {} (days: {})", beginOfJobSearch,
        endOfJobSearch, endOfJobSearch.toEpochDay() - beginOfJobSearch.toEpochDay());

    HolidayInformationCenter.instance().setHolidays(holidaysFromFile);
    JobCenter.instance()
        .combineJobAndWorkerAndSubscribe(workers, jobDescriptions, beginOfJobSearch, endOfJobSearch);

    CalendarWriter.documentJobsAndWorkers(jobDescriptions.stream().map(job -> (ICalendarAccess) job)
        .collect(Collectors.toList()), outputFilePath, reportRange);

    CalendarWriter.documentJobsAndWorkers(workers.stream().map(worker -> (ICalendarAccess) worker)
        .collect(Collectors.toList()), outputFilePath, reportRange);

    try {
      CalendarWriter.writeCalendar(JobCenter.instance().getAllCalendarEntries(),
          Paths.get(outputFilePath, "allEvents.ics").toString(), reportRange);
    } catch (Exception e) {
      logger.error("Buggy Calender export, please try again {}", e);
    }

    logger.info("Finished");
    JobCenter.close();
    HolidayInformationCenter.close();

  }

  private LocalDate getDatesFromString(String date, long defaultValue) {
    long returnValue = defaultValue;
    if (null != date) {
      SimpleDateFormat simpleDateFormatter;
      simpleDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
      simpleDateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
      try {
        Date parse = simpleDateFormatter.parse(date);
        returnValue = parse.getTime() / 1000;
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    return Instant.ofEpochMilli(returnValue * 1000).atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
  }

  void doPostProcessingCommand(String[] execCommand) {

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
      logger.error("Here is the standard error of the command (if any):\n");
      while ((s = stdError.readLine()) != null) {
        logger.info(s);
      }

    } catch (IOException e) {
      logger.error("exception happened: " + e.getMessage());
      System.exit(1);
    }
  }

  // special handling withPostprocessing
  public void withPostprocessing() throws URISyntaxException, IOException {

    final String scheduleTestDir = System.getProperty("SCHEDULER_TEST_DIR", "/var/tmp/schedule/");
    createDirectory(scheduleTestDir);
    createDirectory(scheduleTestDir+"/WithPostProcessing/");
    createDirectory(scheduleTestDir+"/WithPostProcessing/bin");
    createDirectory(scheduleTestDir+"/WithPostProcessing/js");
    createDirectory(scheduleTestDir+"/WithPostProcessing/lib");
    createDirectory(scheduleTestDir+"/WithPostProcessing/css");
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    final ZoneId zoneId = ZoneId.systemDefault();
    final CsvFileLoader csvFileLoader = new CsvFileLoader("dd.MM.yyyy");
    Optional<LocalDate> startDate = csvFileLoader.importJobDescriptionFromFile(this.inputFilePathJobDescriptions)
        .stream().map(JobDescription::getBegin)
        .sorted(Comparator.comparingLong(i -> i.atStartOfDay(zoneId).toEpochSecond()))
        .findFirst();

    LocalDate begin;
    if (startDate.isPresent()) {
      begin = startDate.get();
    } else {
      throw new IllegalArgumentException("beginDate not defined");
    }


    Optional<LocalDate> endDate = csvFileLoader.importJobDescriptionFromFile(this.inputFilePathJobDescriptions).stream()
        .map(JobDescription::getEnd)
        .sorted(Comparator.comparingLong(i -> i.atStartOfDay(zoneId).toEpochSecond()))
        .sorted(Comparator.reverseOrder())
        .findFirst();

    LocalDate end;
    if (endDate.isPresent()) {
      end = endDate.get();
    } else {
      throw new IllegalArgumentException("endDate not defined");
    }

    System.setProperty("PDF_START", begin.toString());
    System.setProperty("PDF_END", end.toString());
    final String prefixInJar = "WithPostProcessing/";
    final String ics2html = FileExtractorFromJar.with(prefixInJar + "bin/ics2html",scheduleTestDir).getPath();
    logger.info("ics2html {}", ics2html);
    final String ical2html = FileExtractorFromJar.with(prefixInJar + "bin/ical2html",scheduleTestDir).getPath();
    logger.info("ical2html {}", ical2html);
    final String jquery = FileExtractorFromJar.with(prefixInJar + "js/jquery.min.js",scheduleTestDir).getPath();
    logger.info("jquery {}", jquery);
    final String style_cols = FileExtractorFromJar.with(prefixInJar + "js/style_cols.js",scheduleTestDir).getPath();
    final String calendar = FileExtractorFromJar.with(prefixInJar + "css/calendar.css",scheduleTestDir).getPath();
    final String libical = FileExtractorFromJar.with(prefixInJar + "lib/libical.so.1.0.1",scheduleTestDir).getPath();

    // final String folderWithConverterScripts = new File(classLoader.getResource(prefixInJar+"bin/ics2html").getFile()).getParentFile().toString();

//    final String copyOrUpdate = "cp -u ";
//    this.doPostProcessingCommand(new String[]{copyOrUpdate + jquery + " " + scheduleTestDir});
//    this.doPostProcessingCommand(new String[]{copyOrUpdate + style_cols + " " + scheduleTestDir});
//    this.doPostProcessingCommand(new String[]{copyOrUpdate + calendar + " " + scheduleTestDir});

    String preload = "LD_PRELOAD=" + libical + " ";
    final String[] command = {
        "/bin/bash", "-c",
        preload + " " + ics2html + " " + scheduleTestDir + " " + scheduleTestDir+"/WithPostProcessing/bin"
    };
    logger.error("Command: {}", Arrays.stream(command).collect(Collectors.toList()));
    this.doPostProcessingCommand(command);

    try {
      //File
      String filename = scheduleTestDir + "allEvents.sh";
      File file = new File(filename);
      Runtime.getRuntime().exec("chmod u+x " + filename);
      //Check the file is writable or read only
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      if (file.canWrite()) {
        try (FileWriter myWriter = new FileWriter(filename)) {
          String renderWithChrome = "google-chrome-stable --headless --disable-gpu --print-to-pdf=" +
              scheduleTestDir + "allEvents.pdf file://" + scheduleTestDir + "/allEvents.html";
          myWriter.write("/bin/bash -c '" + String.join(" ", renderWithChrome) + "'");
        }
      } else {
        System.err.println("File is read only <" + scheduleTestDir + "allEvents.pdf" + ">");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }


}
