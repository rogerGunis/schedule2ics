package de.gunis.roger;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class WithPostProcessing {

    @Test
    public void withPostprocessing() {

        String scheduleTestDir = System.getProperty("SCHEDULER_TEST_DIR", "/var/tmp/schedule");
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
        main.setOutputFilePath(scheduleTestDir);

        ClearingHouse.setLoggingLevel("TRACE");
        main.setLoggingLevel("TRACE");

        main.runEmploymentAgency();

        main.doPostProcessing(ics2html.toString() + " " + scheduleTestDir + " " + folder);
        main.doPostProcessing("cp " + folder + "/../js/jquery.min.js" + " " + scheduleTestDir);
        main.doPostProcessing("cp " + folder + "/../css/calendar.css" + " " + scheduleTestDir);

//        try {
//            Files.copy(new File(folder + "/css/calendar.css").toPath(), new File(scheduleTestDir + "/css/calendar.css").toPath(), REPLACE_EXISTING);
//            Files.copy(new File(folder + "/js/jquery.min.js").toPath(), new File(scheduleTestDir + "/js/jquery.min.js").toPath(), REPLACE_EXISTING);
//        } catch (IOException e) {
//            logger.trace(e.getMessage());
//        }
    }

}
