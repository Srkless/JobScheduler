package gui;

import com.jfoenix.controls.JFXButton;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Callback;

import job.Job;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AddJobController implements Initializable {

    @FXML private JFXButton bCreate;

    @FXML private TextField commandField;

    @FXML private DatePicker deadlinePicker;

    @FXML private TextField durationField;

    @FXML private TextField jobNameField;

    @FXML private TextField priorityField;

    @FXML private DatePicker startDatePicker;

    @FXML private TextField startHourField;

    @FXML private TextField startMinuteField;

    @FXML private TextField deadlineHourField;

    @FXML private TextField deadlineMinuteField;

    @FXML
    private void createJob() {
        String jobName = jobNameField.getText();
        String command = commandField.getText();
        int priority = Integer.parseInt(priorityField.getText());
        int duration = Integer.parseInt(durationField.getText());
        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourField.getText());
        int startMinute = Integer.parseInt(startMinuteField.getText());
        LocalDate deadline = deadlinePicker.getValue();
        int deadlineHour = Integer.parseInt(deadlineHourField.getText());
        int deadlineMinute = Integer.parseInt(deadlineMinuteField.getText());

        LocalDateTime startDateTime =
                LocalDateTime.of(
                        startDate.getYear(),
                        startDate.getMonthValue(),
                        startDate.getDayOfMonth(),
                        startHour,
                        startMinute);
        LocalDateTime deadlineDateTime =
                LocalDateTime.of(
                        deadline.getYear(),
                        deadline.getMonthValue(),
                        deadline.getDayOfMonth(),
                        deadlineHour,
                        deadlineMinute);

        try {
            MainController controller = MainController.getInstance();
            controller.addProcess(
                    new Job(
                            command,
                            jobName,
                            startDateTime,
                            deadlineDateTime,
                            Duration.ofSeconds(duration),
                            priority));
            ((Stage) bCreate.getScene().getWindow()).close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startDatePicker.setDayCellFactory(
                new Callback<DatePicker, DateCell>() {
                    @Override
                    public DateCell call(final DatePicker datePicker) {
                        return new DateCell() {
                            @Override
                            public void updateItem(LocalDate item, boolean empty) {
                                super.updateItem(item, empty);

                                // Ako je datum manji od današnjeg, onemogući ga
                                if (item.isBefore(LocalDate.now())) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #ffc0cb;"); // Opcionalno,
                                }
                            }
                        };
                    }
                });

        deadlinePicker.setDayCellFactory(
                new Callback<DatePicker, DateCell>() {
                    @Override
                    public DateCell call(final DatePicker datePicker) {
                        return new DateCell() {
                            @Override
                            public void updateItem(LocalDate item, boolean empty) {
                                super.updateItem(item, empty);

                                // Ako je datum manji od današnjeg, onemogući ga
                                if (item.isBefore(LocalDate.now())) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #ffc0cb;"); // Opcionalno,
                                }
                            }
                        };
                    }
                });
        applyLimitListener(startHourField, 23);
        applyLimitListener(startMinuteField, 59);
        applyLimitListener(deadlineHourField, 23);
        applyLimitListener(deadlineMinuteField, 59);
        applyNumberListener(priorityField);
        applyNumberListener(durationField);
    }

    private void applyNumberListener(TextField textField) {
        textField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches("\\d*")) {
                                textField.setText(
                                        newValue.replaceAll(
                                                "[^\\d]", "")); // Sprečava unos nebrojnih karaktera
                            }
                        });
    }

    private void applyLimitListener(TextField textField, int limit) {
        textField
                .textProperty()
                .addListener(
                        new ChangeListener<String>() {
                            @Override
                            public void changed(
                                    ObservableValue<? extends String> observable,
                                    String oldValue,
                                    String newValue) {
                                try {
                                    int value = Integer.parseInt(newValue);
                                    if (value > limit || value < 0) {
                                        textField.setText(
                                                "0"); // Ako je vrednost veća od 24, postavi na 0
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignoriši greške ako unesena vrednost nije broj
                                }
                            }
                        });
        applyNumberListener(textField);
    }
}
