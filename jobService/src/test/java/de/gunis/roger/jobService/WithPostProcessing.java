package de.gunis.roger.jobService;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class WithPostProcessing {

    // @Ignore
    @Test
    public void withPostprocessing() throws IOException {

        String scheduleTestDir = System.getProperty("SCHEDULER_TEST_DIR", "/var/tmp/schedule/");
        String folderWithConverterScripts = new File(this.getClass().getClassLoader().getResource("WithPostProcessing/bin/ics2html")
                .getFile()).getParentFile().toString();

        Properties props = new Properties();
        String inputDataPath = "WithPostProcessing/inputData/";
        String configFilePath = this.getClass().getClassLoader().getResource(inputDataPath + "AFTER_SUCCESS_VARIABLES.txt").getFile();
        props.load(new FileReader(configFilePath));

        System.setProperty("PDF_START", props.getProperty("PDF_START"));
        System.setProperty("PDF_END", props.getProperty("PDF_END"));

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
        main.doPostProcessing("cp -u " + folderWithConverterScripts + "/../js/jquery.min.js" + " " + scheduleTestDir);
        main.doPostProcessing("cp -u " + folderWithConverterScripts + "/../css/calendar.css" + " " + scheduleTestDir);

        String library = this.getClass().getClassLoader().getResource("WithPostProcessing/lib/libical.so.1.0.1").getFile();
        String preload = "export LD_PRELOAD="+library+";";
        String command = preload+" "+ics2html.toString() + " " + scheduleTestDir + " " + folderWithConverterScripts + " " + System.getProperty("PDF_START") + " " + System.getProperty("PDF_END");
        System.out.println("Command: "+command);
        main.doPostProcessing(command);

      	try {
    	 	//File
     	 	File file = new File(scheduleTestDir + "allEvents.pdf");
     	 	//Check the file is writable or read only
            if(!file.exists()){
                file.createNewFile();
            }
            else{
                file.delete();
                file.createNewFile();
            }
     	 	if(file.canWrite()){
        		main.doPostProcessing("google-chrome-stable --headless --disable-gpu --print-to-pdf=" +
                		scheduleTestDir + "allEvents.pdf file://" + scheduleTestDir + "/allEvents.html");
         	}
            else{
     	    	System.err.println("File is read only <"+scheduleTestDir + "allEvents.pdf"+">");
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
