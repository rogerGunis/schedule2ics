package de.gunis.roger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class UIController {

    private static final Logger logger = LoggerFactory.getLogger("UIController.class");
    private EmployeeSearch hiringProcess = new EmployeeSearch();
    @FXML // fx:id="outputPath"
    @SuppressWarnings("unused")
    private Button outputPath;
    @FXML // fx:id="holidays"
    @SuppressWarnings("unused")
    private Button holidays;
    @FXML // fx:id="workers"
    @SuppressWarnings("unused")
    private Button workers;
    @FXML // fx:id="jobDescriptions"
    @SuppressWarnings("unused")
    private Button jobDescriptions;
    @FXML // fx:id="createAgenda"
    @SuppressWarnings("unused")
    private Button createAgenda;
    @FXML // fx:id="dateFormat"
    @SuppressWarnings("unused")
    private TextField dateFormat;
    @FXML // fx:id="sayItTF"
    @SuppressWarnings("unused")
    private TextField information;
    private String previousDirectory;

    public UIController() {
    }

    public void setHiringProcess(EmployeeSearch hiringProcess) {
        this.hiringProcess = hiringProcess;
    }

    @FXML
    void getCsvFileAndInformPath(ActionEvent event) {
        String buttonId = ((Button) event.getSource()).getId();
        File fileOrPath;
        if ("outputPath".equals(buttonId)) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("directory selection");
            fileOrPath = directoryChooser.showDialog(new Stage());
        } else if ("createAgenda".equals(buttonId)) {
            if (!"".equals(dateFormat.getText())) {
                hiringProcess.setDateFormat(dateFormat.getText());
            }

            Platform.runLater(() -> {
                try {
                    //an event with a button maybe
//                    System.out.println("button is clicked");
                    hiringProcess.runEmploymentAgency();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            });
//            Task task = new Task<Void>() {
//                @Override
//                public Void call() {
//                    if (isCancelled()) {
//                    }
//                    return null;
//                }
//            };
//            ProgressBar bar = new ProgressBar();
//            bar.progressProperty().bind(task.progressProperty());

//            new Thread(task).start();
            return;
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("csv file selection for " + buttonId);
            String property = previousDirectory != null ? previousDirectory : System.getProperty("user.home");
            fileChooser.setInitialDirectory(new File(property));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
            fileChooser.getExtensionFilters().add(extFilter);
            fileOrPath = fileChooser.showOpenDialog(new Stage());
            previousDirectory = fileOrPath.getParent();
        }

        if (fileOrPath != null) {
            information.setText(fileOrPath.getAbsolutePath());

            if ("holidays".equals(buttonId)) {
                logger.debug("Setting holidays file to: {}", fileOrPath.getAbsolutePath());
                hiringProcess.setInputFilePathHolidays(fileOrPath.getAbsolutePath());
            } else if ("workers".equals(buttonId)) {
                logger.debug("Setting workers file to: {}", fileOrPath.getAbsolutePath());
                hiringProcess.setInputFilePathWorkers(fileOrPath.getAbsolutePath());
            } else if ("jobDescriptions".equals(buttonId)) {
                logger.debug("Setting jobDescriptions file to: {}", fileOrPath.getAbsolutePath());
                hiringProcess.setInputFilePathJobDescriptions(fileOrPath.getAbsolutePath());
            } else if ("outputPath".equals(buttonId)) {
                hiringProcess.setOutputFilePath(fileOrPath.getAbsolutePath());
            }
        }


    }

}
