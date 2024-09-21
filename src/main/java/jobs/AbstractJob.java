package jobs;

import job.Status;

import rabbitmq.RabbitMQHandler;

public abstract class AbstractJob {
    protected String jobName;
    protected volatile Status currentStatus = Status.PAUSED;

    protected volatile RabbitMQHandler rabbitMQ = new RabbitMQHandler();

    public abstract void pauseJob();

    public abstract void stopJob();

    public abstract void resumeJob();

    protected void checkStatus() {
        String state = rabbitMQ.read(jobName);
        if (state != null) {
            System.out.println("State: " + state);
            if (state.equals("PAUSED") && currentStatus == Status.RUNNING) {
                currentStatus = Status.PAUSED;
                pauseJob();
            } else if (state.equals("STOPPED")) {
                currentStatus = Status.STOPPED;
                stopJob();
            } else if (state.equals("RUNNING") && currentStatus == Status.PAUSED) {
                currentStatus = Status.RUNNING;
                System.out.println("Resuming job");
                resumeJob();
            }
        }
    }
}
