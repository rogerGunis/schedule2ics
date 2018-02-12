package de.gunis.roger.jobService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static de.gunis.roger.jobService.ClearingHouse.setLoggingLevel;

public class MainUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setLoggingLevel("TRACE");
        String sceneFile = "/initFrontend.fxml";
        // ClassLoader.getSystemResource
        Parent layout = FXMLLoader.load(getClass().getResource(sceneFile));
        loadStyle(layout);

        UIController UIController = new UIController();

        FXMLLoader loader = new FXMLLoader(this.getClass().getClassLoader().getResource(sceneFile));
        loader.setController(UIController);

        primaryStage.setTitle("Scheduler2ics");
        primaryStage.setScene(new Scene(layout, 440, 300));
        primaryStage.show();
    }

    private void loadStyle(Parent node) {
        node.getStylesheets().clear();
        node.getStylesheets().add(getClass().getResource("/schedule2ics.css").toExternalForm());
    }
}
