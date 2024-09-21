package jobs;


public class DemoJob {
    private String jobName;

    public DemoJob(String jobName) {
        this.jobName = jobName;
        // RabbitMQHandler.createQueue(jobName);
    }

    // public void pauseJob() {
    //     this.currentStatus = Status.PAUSED;
    //     rabbitMQ.write("[" + jobName + "]: " + "PAUSED", jobName);
    // }
    //
    // public void stopJob() {
    //     rabbitMQ.write("[" + jobName + "]: " + "STOPPED", jobName);
    // }
    //
    // public void resumeJob() {
    //     rabbitMQ.write("[" + jobName + "]: " + "RUNNING", jobName);
    // }

    public static void main(String[] args) {
        DemoJob job = new DemoJob("DemoJob");
        for (int i = 0; i < 20; i++) {
            // job.checkStatus();
            // if (job.currentStatus == Status.STOPPED) {
            //     return;
            // } else if (job.currentStatus == Status.PAUSED) {
            //     while (job.currentStatus == Status.PAUSED) {
            //         job.checkStatus();
            //     }
            // }
            System.out.println("Job: " + job.jobName + " is running - " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
