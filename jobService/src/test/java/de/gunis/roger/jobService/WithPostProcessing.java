package de.gunis.roger.jobService;

import de.gunis.roger.jobService.imports.CsvFileLoader;
import de.gunis.roger.jobService.jobsToDo.JobDescription;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

public class WithPostProcessing {

    @Ignore
    @Test
    public void withPostprocessing() throws IOException {

        String scheduleTestDir = System.getProperty("SCHEDULER_TEST_DIR", "/var/tmp/schedule/");
        String folderWithConverterScripts = new File(this.getClass().getClassLoader().getResource("WithPostProcessing/bin/ics2html")
                .getFile()).getParentFile().toString();

        String inputDataPath = "WithPostProcessing/inputData/";
        String jobDescriptionFilePath = this.getClass().getClassLoader().getResource(inputDataPath + "JobDescription.csv").getFile();

        ZoneId zoneId = ZoneId.systemDefault();
        CsvFileLoader csvFileLoader = new CsvFileLoader("dd.MM.yyyy");
        LocalDate begin = csvFileLoader.importJobDescriptionFromFile(jobDescriptionFilePath).stream().map(JobDescription::getBegin)
                .sorted(Comparator.comparingLong(i -> i.atStartOfDay(zoneId).toEpochSecond()))
                .findFirst().get();

        LocalDate end = csvFileLoader.importJobDescriptionFromFile(jobDescriptionFilePath).stream().map(JobDescription::getEnd)
                .sorted(Comparator.comparingLong(i -> i.atStartOfDay(zoneId).toEpochSecond()))
                .sorted(Comparator.reverseOrder())
                .findFirst().get();

        System.setProperty("PDF_START", begin.toString());
        System.setProperty("PDF_END", end.toString());

        String ics2html = this.getClass().getClassLoader().getResource("WithPostProcessing/bin/ics2html").getFile();
        String holidays = this.getClass().getClassLoader().getResource(inputDataPath + "Holidays.csv").getFile();
        String jobDecriptions = this.getClass().getClassLoader().getResource(inputDataPath + "JobDescription.csv").getFile();
        String workers = this.getClass().getClassLoader().getResource(inputDataPath + "Workers.csv").getFile();

        EmployeeSearch main = new EmployeeSearch();

        main.setInputFilePathHolidays(holidays);
        main.setInputFilePathJobDescriptions(jobDecriptions);
        main.setInputFilePathWorkers(workers);
        main.setOutputFilePath(scheduleTestDir);

        ClearingHouse.setLoggingLevel("TRACE");
        main.setLoggingLevel("TRACE");

        main.runEmploymentAgency();
        main.doPostProcessing(new String[]{"cp -u " + folderWithConverterScripts + "/../js/jquery.min.js" + " " + scheduleTestDir});
        main.doPostProcessing(new String[]{"cp -u " + folderWithConverterScripts + "/../js/style_cols.js" + " " + scheduleTestDir});
        main.doPostProcessing(new String[]{"cp -u " + folderWithConverterScripts + "/../css/calendar.css" + " " + scheduleTestDir});

        String library = this.getClass().getClassLoader().getResource("WithPostProcessing/lib/libical.so.1.0.1").getFile();
        String preload = "LD_PRELOAD=" + library + " ";
        String[] command = {
                "/bin/bash", "-c",
                preload + " " + ics2html.toString() + " " + scheduleTestDir + " " + folderWithConverterScripts + " " + System.getProperty("PDF_START") + " " + System.getProperty("PDF_END")
        };
        System.out.println("Command: " + command.toString());
        main.doPostProcessing(command);

        try {
            //File
            File file = new File(scheduleTestDir + "allEvents.pdf");
            //Check the file is writable or read only
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            if (file.canWrite()) {
                main.doPostProcessing(new String[]{"/bin/bash", "-c", "google-chrome-stable --headless --disable-gpu --print-to-pdf=" +
                        scheduleTestDir + "allEvents.pdf file://" + scheduleTestDir + "/allEvents.html"});
            } else {
                System.err.println("File is read only <" + scheduleTestDir + "allEvents.pdf" + ">");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            Files.copy(new File(folderWithConverterScripts + "/css/calendar.css").toPath(), new File(scheduleTestDir + "/css/calendar.css").toPath(), REPLACE_EXISTING);
//            Files.copy(new File(folderWithConverterScripts + "/js/jquery.min.js").toPath(), new File(scheduleTestDir + "/js/jquery.min.js").toPath(), REPLACE_EXISTING);
//        } catch (IOException e) {
//            logger.trace(e.getMessage());
//        }
    }

}
