package gui;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import jobScheduler.JobScheduler;

import java.io.IOException;

public class SchedulerController {
    @FXML private JFXButton bCreate;

    @FXML private TextField numberField;

    @FXML
    void createScheduler(MouseEvent event) {
        int maxConcurrentJobs = Integer.parseInt(numberField.getText());
        JobScheduler jobScheduler = new JobScheduler(maxConcurrentJobs);

        FXMLLoader loader = new FXMLLoader();
        try {
            Parent root =
                    loader.load(getClass().getClassLoader().getResourceAsStream("mainPage.fxml"));
            MainController controller = loader.getController();
            controller.setJobScheduler(jobScheduler);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
            ((Stage) bCreate.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
