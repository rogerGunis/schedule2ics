package de.gunis.roger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class WithPostProcessing {
    private static final Logger logger = LoggerFactory.getLogger("WithPostProcessing.class");


    @Test
    public void withPostprocessing() {

        String schedulerTestDir = System.getProperty("SCHEDULER_TEST_DIR", "/var/tmp/scheduler");
        String folder = new File(this.getClass().getClassLoader().getResource("WithPostProcessing/bin/ics2html")
                .getFile()).getParentFile().toString();

        String ics2html = this.getClass().getClassLoader().getResource("WithPostProcessing/bin/ics2html").getFile();
        String holidays = this.getClass().getClassLoader().getResource("WithPostProcessing/inputData/Holidays.csv").getFile();
        String jobDecriptions = this.getClass().getClassLoader().getResource("WithPostProcessing/inputData/JobDescription.csv").getFile();
        String workers = this.getClass().getClassLoader().getResource("WithPostProcessing/inputData/Workers.csv").getFile();

        EmployeeSearch main = new EmployeeSearch();

        main.setInputFilePathHolidays(holidays);
        main.setInputFilePathJobDescriptions(jobDecriptions);
        main.setInputFilePathWorkers(workers);
        main.setOutputFilePath(schedulerTestDir);

        ClearingHouse.setLoggingLevel("TRACE");
        main.setLoggingLevel("TRACE");

        main.runEmploymentAgency();

        main.doPostProcessing(ics2html.toString() + " " + schedulerTestDir + " " + folder);
        main.doPostProcessing("cp " + folder + "/../js/jquery.min.js" + " " + schedulerTestDir);
        main.doPostProcessing("cp " + folder + "/../css/calendar.css" + " " + schedulerTestDir);

//        try {
//            Files.copy(new File(folder + "/css/calendar.css").toPath(), new File(schedulerTestDir + "/css/calendar.css").toPath(), REPLACE_EXISTING);
//            Files.copy(new File(folder + "/js/jquery.min.js").toPath(), new File(schedulerTestDir + "/js/jquery.min.js").toPath(), REPLACE_EXISTING);
//        } catch (IOException e) {
//            logger.trace(e.getMessage());
//        }
    }

}