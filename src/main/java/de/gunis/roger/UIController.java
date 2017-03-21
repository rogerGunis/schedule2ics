package de.gunis.roger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class UIController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger("UIController.class");
    private static final Map<String, UIController.FileOrPathChooserFunction> CHOOSER_FUNCTION_MAP;
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
    private static final Map<String, String> BUTTON_TOOLTIPS;
    private static EmployeeSearch hiringProcess = new EmployeeSearch();

    static {
        CHOOSER_FUNCTION_MAP = new HashMap<>();
        CHOOSER_FUNCTION_MAP.put("holidays", (o) -> hiringProcess.setInputFilePathHolidays(o));
        CHOOSER_FUNCTION_MAP.put("jobDescriptions", (o) -> hiringProcess.setInputFilePathJobDescriptions(o));
        CHOOSER_FUNCTION_MAP.put("workers", (o) -> hiringProcess.setInputFilePathWorkers(o));
        CHOOSER_FUNCTION_MAP.put("outputPath", (o) -> hiringProcess.setOutputFilePath(o));

        BUTTON_TOOLTIPS = new HashMap<>();
        BUTTON_TOOLTIPS.put("holidays", "Enter holiday information here");
        BUTTON_TOOLTIPS.put("jobDescriptions", "Enter jobDescriptions csv file");
        BUTTON_TOOLTIPS.put("workers", "Enter workers csv file");
        BUTTON_TOOLTIPS.put("outputPath", "Set output file for ics files");
    }

    public UIController() {
    }

    public void setHiringProcess(EmployeeSearch hiringProcess) {
        this.hiringProcess = hiringProcess;
    }

    @FXML
    void getCsvFileAndInformPath(ActionEvent event) {
        String buttonId = ((Button) event.getSource()).getId();
        File fileOrPath;
        if (CHOOSER_FUNCTION_MAP.containsKey(buttonId)) {
            if ("outputPath".equals(buttonId)) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(getInitialPath());
                directoryChooser.setTitle("directory selection");
                fileOrPath = directoryChooser.showDialog(new Stage());
            } else {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("csv file selection for " + buttonId);
                fileChooser.setInitialDirectory(getInitialPath());
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
                fileChooser.getExtensionFilters().add(extFilter);
                fileOrPath = fileChooser.showOpenDialog(new Stage());
            }
            if (fileOrPath != null) {
                previousDirectory = fileOrPath.getParent();
                String absolutePath = fileOrPath.getAbsolutePath();
                information.setText(absolutePath);
                logger.debug("Setting {} file to: {}", buttonId, absolutePath);
                CHOOSER_FUNCTION_MAP.get(buttonId).openFileOrPath(absolutePath);
            }

        } else if ("createAgenda".equals(buttonId)) {
            if (!"".equals(dateFormat.getText())) {
                hiringProcess.setDateFormat(dateFormat.getText());
            }

            Platform.runLater(() -> {
                try {
                    hiringProcess.runEmploymentAgency();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            });
            return;
        }


    }

    private File getInitialPath() {

        return new File(previousDirectory != null ? previousDirectory : System.getProperty("user.home"));
    }

    public void exit(ActionEvent actionEvent) {
        ClearingHouse.log("Bye");
        System.exit(0);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createAgenda.setTooltip(new Tooltip("create ics files"));
        workers.setTooltip(new Tooltip(BUTTON_TOOLTIPS.get("workers")));
    }

    @FunctionalInterface
    private interface FileOrPathChooserFunction {
        void openFileOrPath(String arg);
    }
}
