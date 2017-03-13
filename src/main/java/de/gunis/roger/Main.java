package de.gunis.roger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String sceneFile = "/initFrontend.fxml";
        // ClassLoader.getSystemResource
        Parent root = FXMLLoader.load(getClass().getResource(sceneFile));

        MyController myController = new MyController();
        myController.setMain(new Start());
        FXMLLoader loader = new FXMLLoader(getClass().getResource(sceneFile));
        loader.setController(myController);

        primaryStage.setTitle("Scheduler2ics");
        primaryStage.setScene(new Scene(root, 580, 300));
        primaryStage.show();
    }
}
