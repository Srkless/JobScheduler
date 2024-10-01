package gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import job.ConvolutionJob;
import job.Job;
import job.Status;

import jobScheduler.JobScheduler;

import rabbitmq.RabbitMQHandler;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private VBox processesVBox;
    @FXML private VBox progressVBox;
    @FXML private JFXButton bCreateJob;
    @FXML private ScrollPane progressScrollPane;
    private static MainController instance;

    RabbitMQHandler rabbitMQ = new RabbitMQHandler();
    private JobScheduler jobScheduler;

    public void setJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @FXML
    public void createJob() {
        FXMLLoader loader = new FXMLLoader();
        try {
            Parent root =
                    loader.load(getClass().getClassLoader().getResourceAsStream("addJobPage.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Job j1 =
        //         new Job(
        //                 "java src/main/java/jobs/DemoJob.java DemoJob1 30",
        //                 "DemoJob1",
        //                 LocalDateTime.now().plusSeconds(5),
        //                 LocalDateTime.now().plusSeconds(100),
        //                 Duration.ofSeconds(100),
        //                 1);
        // Job j2 =
        //         new Job(
        //                 "java src/main/java/jobs/DemoJob.java DemoJob2 20",
        //                 "DemoJob2",
        //                 LocalDateTime.now().plusSeconds(7),
        //                 LocalDateTime.now().plusSeconds(100),
        //                 Duration.ofSeconds(100),
        //                 1);
        ConvolutionJob j1 =
                new ConvolutionJob(
                        "java src/main/java/jobs/Convolution.java",
                        new File("Images/"),
                        new File("Output/"),
                        new File("edgeDetectionKernel.txt"),
                        "CONV_A",
                        LocalDateTime.now().plusSeconds(5),
                        LocalDateTime.now().plusSeconds(100),
                        Duration.ofSeconds(100),
                        1);

        addProcess(j1);

        // addProcess(j2);
    }

    public static MainController getInstance() {
        return instance;
    }

    public void addProcess(Job job) {
        HBox process = new HBox();
        process.setPadding(new javafx.geometry.Insets(0, 0, 0, 20));

        JFXToggleButton toggleButton = new JFXToggleButton();
        toggleButton.setUnToggleColor(Paint.valueOf("#e53935"));
        toggleButton.setUnToggleLineColor(Paint.valueOf("#ef9a9a"));
        toggleButton.setToggleColor(Paint.valueOf("#43a047"));
        toggleButton.setToggleLineColor(Paint.valueOf("#a5d6a7"));
        toggleButton.setMaxHeight(20);
        toggleButton.setMinHeight(20);
        toggleButton.setPrefHeight(20);
        toggleButton.setText("");

        Label label = new Label(job.getJobPriority() + " - " + job.getJobName());
        label.setStyle("-fx-font-size: 15px;-fx-text-fill: white;");
        // label.setPadding(new javafx.geometry.Insets(0, 00, 0, 10));

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxHeight(20);
        progressBar.setMinHeight(20);
        progressBar.setMinWidth(220);
        progressBar.setMaxWidth(220);
        progressBar.setProgress(0);

        CheckBox checkBox = new CheckBox();
        checkBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 10));
        toggleButton.setUserData(job);
        toggleButton
                .selectedProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            Job job1 = (Job) toggleButton.getUserData();
                            if (newValue) {
                                // Ako je ToggleButton uključen (desno), postavi ProgressBar na 100%
                                if (job1.getStatus() == Status.NOT_STARTED) {
                                    toggleButton.setDisable(true);
                                    jobScheduler.addJob(job1);
                                    jobScheduler.startJob(job1);
                                } else {
                                    jobScheduler.executeJobWithTimeout(job1);
                                }

                            } else {
                                // Ako je isključen (levo), postavi ProgressBar na 0%
                                jobScheduler.manualPauseJob(job1);
                            }
                        });

        Thread thread =
                new Thread(
                        () -> {
                            try {
                                RabbitMQHandler.createQueue(job.getJobName());
                                while (true) {
                                    String read = rabbitMQ.read(job.getJobName());
                                    if (read != null && !read.contains("STOPPED")) {
                                        toggleButton.setDisable(false);

                                        Platform.runLater(
                                                () -> {
                                                    Label progressLabel = new Label(read + " ");
                                                    progressLabel.setStyle(
                                                            " -fx-background-color:"
                                                                + " linear-gradient(to right,"
                                                                + " #e3848a,#ed3b47);-fx-background-radius:"
                                                                + " 15; -fx-text-fill: white;"
                                                                + " -fx-prompt-text-fill: white;");

                                                    progressLabel.setPadding(
                                                            new javafx.geometry.Insets(
                                                                    0, 0, 0, 15));

                                                    if (progressVBox.getChildren().size() > 500) {
                                                        // Očistite najstariji element
                                                        progressVBox
                                                                .getChildren()
                                                                .remove(0); // Uklonite prvi element
                                                    }
                                                    progressVBox.getChildren().add(progressLabel);
                                                    progressScrollPane.layout();
                                                    progressScrollPane.setVvalue(1.0);
                                                });
                                        Double progress =
                                                (double) job.getProgressStep() / job.getProgress();
                                        if (progress >= 1) {
                                            progressBar.getStyleClass().removeAll("progress-bar");
                                            progressBar
                                                    .getStyleClass()
                                                    .add("progress-bar-complete");
                                        } else {
                                            progressBar
                                                    .getStyleClass()
                                                    .removeAll("progress-bar-complete");
                                            progressBar.getStyleClass().add("progress-bar");
                                        }
                                        progressBar.setProgress(progress);
                                    }
                                    ;
                                }
                            } catch (Exception e) {
                                // TODO: handle exception
                                e.printStackTrace();
                            }
                        });

        thread.setDaemon(true);
        thread.start();
        process.getChildren().addAll(label, toggleButton, progressBar, checkBox);
        process.setUserData(job);
        processesVBox.getChildren().add(process);
        System.out.println("Process added");
    }

    @FXML
    public void stopJob(MouseEvent event) {
        List<Job> selectedJobs = getSelectedJobs();
        for (Job job : selectedJobs) {
            for (javafx.scene.Node node : processesVBox.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    JFXToggleButton toggleButton = null;
                    for (javafx.scene.Node innerNode : hbox.getChildren()) {
                        if (innerNode instanceof JFXToggleButton) {
                            toggleButton = (JFXToggleButton) innerNode;
                        }
                    }
                    if (toggleButton != null && toggleButton.getUserData().equals(job)) {
                        toggleButton.setDisable(true);
                        jobScheduler.stopJob(job);
                        break;
                    }
                }
            }
        }
        unselectAllCheckBoxes();
    }

    public void deleteJob(MouseEvent event) {
        System.out.println("Delete job");
        List<Job> selectedJobs = getSelectedJobs();
        for (Job job : selectedJobs) {
            for (javafx.scene.Node node : processesVBox.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    JFXToggleButton toggleButton = null;
                    for (javafx.scene.Node innerNode : hbox.getChildren()) {
                        if (innerNode instanceof JFXToggleButton) {
                            toggleButton = (JFXToggleButton) innerNode;
                        }
                    }
                    if (toggleButton != null && toggleButton.getUserData().equals(job)) {
                        processesVBox.getChildren().remove(hbox);
                        jobScheduler.removeJob(job);
                        break;
                    }
                }
            }
        }
        unselectAllCheckBoxes();
    }

    public List<Job> getSelectedJobs() {
        List<Job> selectedJobs = new ArrayList<>();

        for (javafx.scene.Node node : processesVBox.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                CheckBox selectedCheckBox = null;
                JFXToggleButton toggleButton = null;

                // Iteracija kroz sve elemente unutar HBox-a
                for (javafx.scene.Node innerNode : hbox.getChildren()) {
                    if (innerNode instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) innerNode;
                        if (checkBox.isSelected()) {
                            selectedCheckBox = checkBox; // Pronađi selektovani CheckBox
                        }
                    }
                    if (innerNode instanceof JFXToggleButton) {
                        toggleButton = (JFXToggleButton) innerNode; // Pronađi toggle button
                    }
                }

                // Ako imamo selektovani CheckBox i toggleButton, dodaj Job u listu
                if (selectedCheckBox != null && toggleButton != null) {
                    Job selectedJob = (Job) toggleButton.getUserData();
                    selectedJobs.add(selectedJob);
                }
            }
        }

        return selectedJobs; // Vrati listu selektovanih poslova
    }

    public void unselectAllCheckBoxes() {
        // Iteriramo kroz sve elemente unutar VBox-a
        for (javafx.scene.Node node : processesVBox.getChildren()) {
            if (node instanceof HBox) { // Ako je trenutni čvor HBox
                HBox hbox = (HBox) node;

                // Iteriramo kroz sve elemente unutar HBox-a
                for (javafx.scene.Node innerNode : hbox.getChildren()) {
                    if (innerNode instanceof CheckBox) { // Ako je unutrašnji čvor CheckBox
                        CheckBox checkBox = (CheckBox) innerNode;
                        checkBox.setSelected(false); // Postavljamo CheckBox na "deselected"
                    }
                }
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO Auto-generated method stub
        instance = this;
    }
}
