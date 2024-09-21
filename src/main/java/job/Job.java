package job;

import rabbitmq.RabbitMQHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Job extends Thread implements Comparable<Job> {
    private String command;
    private String jobName = "Job_";
    private Status status = Status.PAUSED;
    private int jobPriority;
    private volatile Process process;
    private volatile Status currentStatus = Status.PAUSED;
    private volatile RabbitMQHandler rabbitMQ = new RabbitMQHandler();

    private static JobController jobController = new JobController();

    public Job(String command, String jobName, int priority) {
        this.command = command;
        this.jobName += jobName;
        this.jobPriority = priority;
        start();
    }

    public RabbitMQHandler getRabbitMQ() {
        return rabbitMQ;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName += jobName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getJobPriority() {
        return jobPriority;
    }

    public void setJobPriority(int jobPriority) {
        this.jobPriority = jobPriority;
    }

    public void startJob() {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.redirectErrorStream(true);
        try {
            this.process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkStatus() {
        String state = rabbitMQ.read(jobName);
        if (state != null) {
            if (state.equals("PAUSED") && currentStatus == Status.RUNNING) {
                currentStatus = Status.PAUSED;
                jobController.execute("PAUSE", this);
            } else if (state.equals("STOPPED")) {
                currentStatus = Status.STOPPED;
                jobController.execute("STOP", this);
            } else if (state.equals("RUNNING") && currentStatus == Status.PAUSED) {
                currentStatus = Status.RUNNING;
                jobController.execute("RESUME", this);
            }
        }
    }

    @Override
    public void run() {
        startJob();
        int i = 0;
        while (true) {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    checkStatus();
                    System.out.println(line);
                    if (i++ == 5) jobController.execute("PAUSE", this);
                }
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int compareTo(Job job) {
        return this.jobPriority - job.jobPriority;
    }

    // public static void main(String[] args) {
    //     // Job job =
    //     //         new Job(
    //     //                 "java -cp \"target/classes:target/dependency/*\" jobs.DemoJob",
    //     //                 "DemoJob",
    //
    //     Job job = new Job("java -cp target/classes jobs.DemoJob", "DemoJob", 1);
    //     Job job2 = new Job("java -cp target/classes jobs.DemoJob", "DemoJob2", 1);
    //
    //     try {
    //         Thread.sleep(15000);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //     jobController.execute("RESUME", job);
    // }
}
