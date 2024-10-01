package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private static final String schedulerApp = "schedulerPage.fxml";

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource(schedulerApp));
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Job Scheduler");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
