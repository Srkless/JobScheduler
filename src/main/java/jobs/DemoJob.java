package jobs;

public class DemoJob {
    private String jobName;
    private int progress;

    public DemoJob(String jobName, int progress) {
        this.jobName = jobName;
        this.progress = progress;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a job name");
            return;
        }
        DemoJob job = new DemoJob(args[0], Integer.parseInt(args[1]));
        for (int i = 1; i <= job.progress; i++) {
            System.out.println("Job: " + job.jobName + " is running - " + i + "/" + job.progress);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
