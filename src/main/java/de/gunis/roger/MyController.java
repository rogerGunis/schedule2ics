package de.gunis.roger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MyController {

    private Start main = null;

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

    public MyController() {
    }

    public void setMain(Start main) {
        this.main = main;
    }

    @FXML
    void getCsvFileAndInformPath(ActionEvent event) {
        String buttonId = ((Button) event.getSource()).getId();
        File fileOrPath;
        if ("outputPath".equals(buttonId)) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("directory selection");
            fileOrPath = directoryChooser.showDialog(new Stage());
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("csv file selection");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
            fileChooser.getExtensionFilters().add(extFilter);
            fileOrPath = fileChooser.showOpenDialog(new Stage());
        }

        if (fileOrPath != null) {
            information.setText(fileOrPath.getAbsolutePath());

            if ("createAgenda".equals(buttonId)) {
                if (!"".equals(dateFormat.getText())) {
                    main.setDateFormat(dateFormat.getText());
                }

                main.runEmploymentAgency();
            } else if ("holidays".equals(buttonId)) {
                main.setInputFilePathHolidays(fileOrPath.getAbsolutePath());
            } else if ("workers".equals(buttonId)) {
                main.setInputFilePathWorkers(fileOrPath.getAbsolutePath());
            } else if ("jobDescriptions".equals(buttonId)) {
                main.setInputFilePathJobDescriptions(fileOrPath.getAbsolutePath());
            } else if ("outputPath".equals(buttonId)) {
                main.setOutputFilePath("");
            }
        }


    }

}
