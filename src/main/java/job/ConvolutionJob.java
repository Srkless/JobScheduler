package job;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

public class ConvolutionJob extends Job {
    private File kernelFile;
    private File inputFolder;
    private File outputFolder;
    private ArrayList<Process> processes = new ArrayList<Process>();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile boolean paused = false;
    private final ExecutorService executorService;

    public ConvolutionJob(
            String command,
            File inputFolder,
            File outputFolder,
            File kernelFile,
            String jobName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Duration duration,
            int priority) {
        super(command, jobName, startTime, endTime, duration, priority);
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.kernelFile = kernelFile;
        System.out.println("Kernel file: " + kernelFile.getAbsolutePath());
        System.out.println("Input folder: " + inputFolder.getAbsolutePath());
        System.out.println("Output folder: " + outputFolder.getAbsolutePath());
        for (File file : inputFolder.listFiles()) {
            BufferedImage image;
            try {
                image = ImageIO.read(file);
                progress += image.getWidth() - 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.executorService = Executors.newCachedThreadPool();
    }

    public File getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(File inputFolder) {
        this.inputFolder = inputFolder;
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Override
    public void start() {

        int fileCount = inputFolder.listFiles().length;
        CountDownLatch latch = new CountDownLatch(fileCount);
        for (File file : inputFolder.listFiles()) {
            String imageCommand =
                    command
                            + " "
                            + kernelFile.getAbsolutePath()
                            + " "
                            + file.getAbsolutePath()
                            + " "
                            + outputFolder.getAbsolutePath();
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", imageCommand);
            processBuilder.redirectErrorStream(true);
            try {
                Process process = processBuilder.start();
                this.processes.add(process);
                System.out.println("Job: " + jobName + " is running....");
                Thread thread =
                        new Thread(
                                () -> {
                                    try {
                                        readProcessOutput(process);
                                    } finally {
                                        latch.countDown(); // Decrement the latch count when done
                                    }
                                });
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // After all processes are complete, set job status to COMPLETED
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        this.status = Status.COMPLETED;
        System.out.println("Job: " + jobName + " is completed....");
    }

    public void readProcessOutput(Process process) {
        try {
            while (true) {
                String line = "";
                long previousProgress = 0;
                try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while ((line = reader.readLine()) != null) {
                        lock.lock();
                        try {
                            while (paused) {
                                condition.await(); // Wait until the thread is resumed
                            }
                        } finally {
                            lock.unlock();
                        }

                        int currentProgress = Integer.parseInt(line.split(" - ")[1].split("/")[0]);

                        synchronized (this) {
                            progressStep +=
                                    (currentProgress
                                            - previousProgress); // Dodajemo samo razliku u napretku
                            previousProgress = currentProgress; // Ažuriramo prethodni napredak
                        }

                        // Umesto direktnog slanja, kreirajte zadatak za executor
                        rabbitMQ.write(line, jobName);
                    }
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        System.out.println("PAUSING CONV");
        for (Process process : processes) {
            if (jobController.execute("PAUSE", process)) {
                lock.lock();
                try {
                    paused = true;
                } finally {
                    lock.unlock();
                }

                this.status = Status.PAUSED;
                setStatus(Status.PAUSED);
                rabbitMQ.write("[" + jobName + "]: " + "PAUSED", jobName);
            }
        }
    }

    @Override
    public void stop() {
        for (Process process : processes) {
            if (jobController.execute("STOP", process)) {
                this.status = Status.STOPPED;
                rabbitMQ.write("[" + jobName + "]: " + "STOPPED", jobName);
            }
        }
    }

    @Override
    public void resume() {
        for (Process process : processes) {
            if (jobController.execute("RESUME", process)) {
                this.status = Status.RUNNING;
                lock.lock();
                try {
                    paused = false;
                    condition.signalAll(); // Signal the thread to resume
                } finally {
                    lock.unlock();
                }
                rabbitMQ.write("[" + jobName + "]: " + "RESUMED", jobName);
            }
        }
    }

    public static void main(String[] args) {
        ConvolutionJob j1 =
                new ConvolutionJob(
                        "java src/main/java/jobs/Convolution.java",
                        new File("Images/"),
                        new File("Output/"),
                        new File("edgeDetectionKernel.txt"),
                        "DemoJob1",
                        LocalDateTime.now().plusSeconds(5),
                        LocalDateTime.now().plusSeconds(100),
                        Duration.ofSeconds(100),
                        1);
        j1.start();
    }
}
