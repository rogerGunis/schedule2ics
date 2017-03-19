package de.gunis.roger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static de.gunis.roger.ClearingHouse.setLoggingLevel;

public class MainUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setLoggingLevel("TRACE");
        String sceneFile = "/initFrontend.fxml";
        // ClassLoader.getSystemResource
        Parent root = FXMLLoader.load(getClass().getResource(sceneFile));

        UIController UIController = new UIController();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(sceneFile));
        loader.setController(UIController);

        primaryStage.setTitle("Scheduler2ics");
        primaryStage.setScene(new Scene(root, 580, 300));
        primaryStage.show();
    }
}
