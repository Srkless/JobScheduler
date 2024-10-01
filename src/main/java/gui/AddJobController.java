package gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import job.ConvolutionJob;
import job.Job;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AddJobController implements Initializable {

    @FXML private JFXButton bCreate;

    @FXML private JFXButton bInputFolder;

    @FXML private JFXButton bOutputFolder;

    @FXML private JFXButton bChooseKernel;

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

    @FXML private JFXRadioButton radioConvJob;

    @FXML private JFXRadioButton radioJob;

    @FXML private ToggleGroup processType;

    @FXML private VBox jobFieldsVBox;

    @FXML private HBox convOptionsHBox;

    private File inputFolder;

    private File outputFolder;

    private File kernelFile;

    @FXML
    private void createJob() {

        if (jobNameField.getText().trim().isEmpty()
                || commandField.getText().trim().isEmpty()
                || priorityField.getText().trim().isEmpty()
                || durationField.getText().trim().isEmpty()
                || startDatePicker.getValue() == null
                || startHourField.getText().trim().isEmpty()
                || startMinuteField.getText().trim().isEmpty()
                || deadlinePicker.getValue() == null
                || deadlineHourField.getText().trim().isEmpty()
                || deadlineMinuteField.getText().trim().isEmpty()) {
            return;
        }
        String jobName = jobNameField.getText().trim();
        String command = commandField.getText().trim();
        int priority = Integer.parseInt(priorityField.getText().trim());
        int duration = Integer.parseInt(durationField.getText().trim());
        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourField.getText().trim());
        int startMinute = Integer.parseInt(startMinuteField.getText().trim());
        LocalDate deadline = deadlinePicker.getValue();
        int deadlineHour = Integer.parseInt(deadlineHourField.getText().trim());
        int deadlineMinute = Integer.parseInt(deadlineMinuteField.getText().trim());

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
            if (radioConvJob.isSelected()) {
                controller.addProcess(
                        new ConvolutionJob(
                                command,
                                inputFolder,
                                outputFolder,
                                kernelFile,
                                jobName,
                                startDateTime,
                                deadlineDateTime,
                                Duration.ofSeconds(duration),
                                priority));
            } else {
                controller.addProcess(
                        new Job(
                                command,
                                jobName,
                                startDateTime,
                                deadlineDateTime,
                                Duration.ofSeconds(duration),
                                priority));
            }
            ((Stage) bCreate.getScene().getWindow()).close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    // public JFXRadioButton getSelectedRadioButton() {
    //     Toggle selectedToggle = processType.getSelectedToggle();
    //     if (selectedToggle != null) {
    //         return (JFXRadioButton) selectedToggle;
    //     }
    //     return null; // Nijedan nije selektovan
    // }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        convOptionsHBox.setVisible(false);
        convOptionsHBox.setManaged(false);
        bCreate.setVisible(true);
        bCreate.setManaged(true);

        processType
                .selectedToggleProperty()
                .addListener(
                        (observable, oldToggle, newToggle) -> {
                            if (newToggle == radioJob) {
                                // Ako je selektovan "Job" RadioButton
                                convOptionsHBox.setVisible(false);
                                convOptionsHBox.setManaged(false);
                                bCreate.setVisible(true);
                                bCreate.setManaged(true);
                            } else if (newToggle == radioConvJob) {
                                // Ako je selektovan "Convolution" RadioButton
                                convOptionsHBox.setVisible(true);
                                convOptionsHBox.setManaged(true);
                                bCreate.setVisible(true);
                                bCreate.setManaged(true);
                            }
                        });
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

        bInputFolder.setOnAction(
                event -> {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Izaberite folder");

                    // Opcionalno: postavljamo početni direktorijum
                    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

                    // Prikazujemo dijalog i dobijamo izabrani folder
                    File selectedDirectory = directoryChooser.showDialog(new Stage());

                    if (selectedDirectory != null) {
                        inputFolder = selectedDirectory;
                        bInputFolder.setText(selectedDirectory.getName());
                    }
                });

        bOutputFolder.setOnAction(
                event -> {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Izaberite folder");

                    // Opcionalno: postavljamo početni direktorijum
                    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

                    // Prikazujemo dijalog i dobijamo izabrani folder
                    File selectedDirectory = directoryChooser.showDialog(new Stage());

                    if (selectedDirectory != null) {
                        outputFolder = selectedDirectory;
                        bOutputFolder.setText(selectedDirectory.getName());
                    }
                });

        bChooseKernel.setOnAction(
                event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Izaberite kernel fajl");

                    // Opcionalno: postavljamo početni direktorijum
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

                    // Prikazujemo dijalog i dobijamo izabrani folder
                    File selectedFile = fileChooser.showOpenDialog(new Stage());

                    if (selectedFile != null) {
                        kernelFile = selectedFile;
                        bChooseKernel.setText(selectedFile.getName());
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
