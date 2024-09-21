package job;

import java.util.HashMap;
import java.util.function.Consumer;

public class JobController {
    private HashMap<String, Consumer<Job>> controller = new HashMap<String, Consumer<Job>>();

    public JobController() {
        addJobs();
    }

    public void execute(String command, Job job) {
        if (controller.containsKey(command)) {
            controller.get(command).accept(job);
        }
    }

    public void addJobs() {
        controller.put("PAUSE", this::pauseJob);
        controller.put("STOP", this::stopJob);
        controller.put("RESUME", this::resumeJob);
    }

    private void pauseJob(Job job) {
        try {
            if (job.getProcess() != null && job.getProcess().isAlive()) {
                String command = "kill -STOP " + job.getProcess().pid();
                Runtime.getRuntime().exec(command.split(" "));
                job.setStatus(Status.PAUSED);
                job.getRabbitMQ()
                        .write("[" + job.getJobName() + "]: " + "PAUSED", job.getJobName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopJob(Job job) {
        job.getRabbitMQ().write("[" + job.getJobName() + "]: " + "STOPPED", job.getJobName());
        job.setStatus(Status.STOPPED);
    }

    public void resumeJob(Job job) {
        try {
            if (job.getProcess() != null && job.getProcess().isAlive()) {
                String command = "kill -CONT " + job.getProcess().pid();
                Runtime.getRuntime().exec(command.split(" "));
                job.setStatus(Status.RUNNING);
                job.getRabbitMQ()
                        .write("[" + job.getJobName() + "]: " + "RUNNING", job.getJobName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
