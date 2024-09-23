package jobScheduler;

import job.Job;
import job.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.concurrent.*;

public class JobScheduler {
    private PriorityQueue<Job> jobQueue = new PriorityQueue<Job>();
    private volatile int runningJobs = 0;
    private int maxRunningJobs = 0;
    private ThreadPoolExecutor threadPool;
    private ScheduledExecutorService scheduler;

    public JobScheduler(int maxRunningJobs) {
        this.maxRunningJobs = maxRunningJobs;
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
        scheduler = Executors.newScheduledThreadPool(10); // Tajmer za zakazivanje poslova
    }

    public void addJob(Job job) {
        jobQueue.add(job);
    }

    public void startJob(Job job) {
        if (job.getStartTime() != null && LocalDateTime.now().isBefore(job.getStartTime())) {
            Duration waitTime = Duration.between(LocalDateTime.now(), job.getStartTime());
            System.out.println(
                    "Job " + job.getJobName() + " is scheduled to start at: " + job.getStartTime());

            scheduler.schedule(
                    () -> executeJobWithTimeout(job), waitTime.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            executeJobWithTimeout(job);
        }
    }

    private void executeJobWithTimeout(Job job) {
        if (runningJobs < maxRunningJobs) {
            runningJobs++;
            job.setStatus(Status.RUNNING);

            // Proveri da li je deadline pre isteka vremena trajanja
            if (LocalDateTime.now().isAfter(job.getEndTime())) {
                System.out.println(
                        "Job " + job.getJobName() + " missed the deadline. Cancelling...");
                job.stop(); // Prekini posao jer je promašen deadline
                runningJobs--;
                return;
            }

            Future<?> future =
                    threadPool.submit(
                            () -> {
                                job.start();
                                job.setStatus(Status.COMPLETED);
                                return null;
                            });

            // Timeout za prekidanje posla ako ne završi na vreme
            scheduler.schedule(
                    () -> {
                        if (!future.isDone()) {
                            System.out.println(
                                    "Job "
                                            + job.getJobName()
                                            + " exceeded execution time. Cancelling...");
                            future.cancel(true); // Prekidanje future task-a
                            job.stop(); // Prekini i sam proces ako traje predugo
                        }
                    },
                    job.getDuration().toMillis(),
                    TimeUnit.MILLISECONDS);

            runningJobs--;
            jobQueue.remove(job);
            nextJob();
        } else {
            if (scheduleRunningJobs(job)) {
                startJob(job);
            }
        }
    }

    public boolean scheduleRunningJobs(Job job) {
        Job jobToPause = null;
        int priorityDiff = Integer.MAX_VALUE;
        for (Job j : jobQueue) {
            if ((j.getStatus() == Status.RUNNING)
                    && (j.compareTo(job) < priorityDiff)
                    && (j.compareTo(job) < 0)) {
                jobToPause = j;
                priorityDiff = j.compareTo(job);
            }
        }
        if (jobToPause != null) {
            jobToPause.pause();
            runningJobs--;
            return true;
        }
        return false;
    }

    public void nextJob() {
        int highestPriority = Integer.MIN_VALUE;
        Job nextJob = null;

        if (!jobQueue.isEmpty()) {
            for (Job job : jobQueue) {
                if (nextJob == null) {
                    nextJob = job;
                } else if (job.getStatus() == Status.PAUSED
                        && job.compareTo(nextJob) > highestPriority) {
                    nextJob = job;
                    highestPriority = job.compareTo(nextJob);
                }
            }
            if (nextJob != null) {
                nextJob.resume();
            }
        }
    }

    public static void main(String[] args) {
        Job job1 =
                new Job(
                        "java src/main/java/jobs/DemoJob.java DemoJob1",
                        "DemoJob1",
                        LocalDateTime.now().plusSeconds(5),
                        LocalDateTime.now().plusSeconds(15),
                        Duration.ofSeconds(100),
                        1);

        Job job2 =
                new Job(
                        "java src/main/java/jobs/DemoJob.java DemoJob2",
                        "DemoJob2",
                        LocalDateTime.now().plusSeconds(10),
                        LocalDateTime.now().plusSeconds(15),
                        Duration.ofSeconds(5),
                        2);

        JobScheduler scheduler = new JobScheduler(3);
        scheduler.addJob(job1);
        scheduler.addJob(job2);

        scheduler.startJob(job1);
        scheduler.startJob(job2);
    }
}
