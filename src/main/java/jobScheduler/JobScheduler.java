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

    public void removeJob(Job job) {
        if (job.getProcess() != null) job.stop();
        jobQueue.remove(job);
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

    public void pauseJob(Job job) {
        if (job.getStatus() == Status.RUNNING) {
            job.pause();
            runningJobs--;
            if (runningJobs < 0) runningJobs = 0;
            job.setStatus(Status.PAUSED);
            nextJob();
        }
        System.out.println("Job " + job.getJobName() + " is paused..." + runningJobs);
    }

    public void manualPauseJob(Job job) {
        if (job.getStatus() == Status.RUNNING) {
            job.pause();
            runningJobs--;
            if (runningJobs < 0) runningJobs = 0;
            job.setStatus(Status.MANUALLY_PAUSED);
            nextJob();
        }
        System.out.println("Job " + job.getJobName() + " is manually paused..." + runningJobs);
    }

    public void resumeJob(Job job) {
        if (job.getStatus() == Status.PAUSED
                || job.getStatus() == Status.WAITING
                || job.getStatus() == Status.MANUALLY_PAUSED) {
            runningJobs++;
            job.setStatus(Status.RUNNING);
            job.resume();
            // executeJobWithTimeout(job);
        }
    }

    public void stopJob(Job job) {
        job.stop();
        runningJobs--;
        if (runningJobs < 0) runningJobs = 0;
        jobQueue.remove(job);
        nextJob();
    }

    public void executeJobWithTimeout(Job job) {
        System.out.println("ULAZ: " + job.getJobName() + " " + job.getStatus() + " " + runningJobs);

        if (runningJobs < maxRunningJobs) {
            System.out.println("Running jobs: " + runningJobs);
            if (LocalDateTime.now().isAfter(job.getEndTime())) {
                System.out.println(
                        "Job " + job.getJobName() + " missed the deadline. Cancelling...");
                job.stop();
                runningJobs--;
                return;
            }

            if (job.getStatus() == Status.NOT_STARTED) {
                job.setStatus(Status.RUNNING);
                runningJobs++;
                System.out.println("Job " + job.getJobName() + " is running...");
                Future<?> future =
                        threadPool.submit(
                                () -> {
                                    job.start();
                                    job.setStatus(Status.COMPLETED);
                                    runningJobs--;
                                    if (runningJobs < 0) runningJobs = 0;
                                    jobQueue.remove(job);
                                    nextJob();

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
                                runningJobs--;
                                if (runningJobs < 0) runningJobs = 0;
                            }
                        },
                        job.getDuration().toMillis(),
                        TimeUnit.MILLISECONDS);
            } else if (job.getStatus() == Status.PAUSED
                    || job.getStatus() == Status.MANUALLY_PAUSED
                    || job.getStatus() == Status.WAITING) {
                resumeJob(job);
            }
        } else {
            if (scheduleRunningJobs(job)) {
                if (job.getStatus() == Status.NOT_STARTED) {
                    startJob(job);
                } else {
                    executeJobWithTimeout(job);
                }
            } else if (job.getStatus() != Status.NOT_STARTED) {
                System.out.println("Job " + job.getJobName() + " is waiting...");
                job.setStatus(Status.WAITING);
                pauseJob(job);
            }
        }
        System.out.println("IZLAZ: " + job.getJobName() + " " + job.getStatus());
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
            System.out.println("Pausing job: " + jobToPause.getJobName());
            pauseJob(jobToPause);
            runningJobs--;
            if (runningJobs < 0) runningJobs = 0;
            return true;
        }
        return false;
    }

    public void nextJob() {
        int highestPriority = Integer.MIN_VALUE;
        Job nextJob = null;

        if (!jobQueue.isEmpty()) {
            for (Job job : jobQueue) {
                // if (nextJob == null) {
                //     nextJob = job;
                //     System.out.println("Next job: " + nextJob.getJobName());
                // } else
                if (job.getStatus() == Status.PAUSED
                        || job.getStatus() == Status.WAITING
                        || job.getStatus() == Status.NOT_STARTED)
                    if (nextJob == null || job.compareTo(nextJob) > highestPriority) {
                        nextJob = job;
                        highestPriority = job.compareTo(nextJob);
                    }
                // if (job.getStatus() == Status.NOT_STARTED) {
                //     nextJob = job;
                // }
            }
            if (nextJob != null) {
                System.out.println("Next job: " + nextJob.getJobName());
                if (nextJob.getStatus() == Status.NOT_STARTED) {
                    startJob(nextJob);
                } else {
                    resumeJob(nextJob);
                }
            }
        }
    }

    public void startScheduler() {
        Job job1 =
                new Job(
                        "java src/main/java/jobs/DemoJob.java DemoJob1",
                        "DemoJob1",
                        LocalDateTime.now().plusSeconds(5),
                        LocalDateTime.now().plusSeconds(100),
                        Duration.ofSeconds(100),
                        1);

        Job job2 =
                new Job(
                        "java src/main/java/jobs/DemoJob.java DemoJob2",
                        "DemoJob2",
                        LocalDateTime.now().plusSeconds(7),
                        LocalDateTime.now().plusSeconds(100),
                        Duration.ofSeconds(100),
                        1);

        JobScheduler scheduler = new JobScheduler(2);
        scheduler.addJob(job1);
        scheduler.addJob(job2);

        scheduler.startJob(job1);
        scheduler.startJob(job2);
    }

    // public static void main(String[] args) {
    //     Job job1 =
    //             new Job(
    //                     "java src/main/java/jobs/DemoJob.java DemoJob1",
    //                     "DemoJob1",
    //                     LocalDateTime.now().plusSeconds(5),
    //                     LocalDateTime.now().plusSeconds(15),
    //                     Duration.ofSeconds(100),
    //                     1);
    //
    //     Job job2 =
    //             new Job(
    //                     "java src/main/java/jobs/DemoJob.java DemoJob2",
    //                     "DemoJob2",
    //                     LocalDateTime.now().plusSeconds(10),
    //                     LocalDateTime.now().plusSeconds(15),
    //                     Duration.ofSeconds(5),
    //                     2);
    //
    //     Job test =
    //             new Job(
    //                     "~/test.sh",
    //                     "Test",
    //                     LocalDateTime.now().plusSeconds(10),
    //                     LocalDateTime.now().plusSeconds(15),
    //                     Duration.ofSeconds(5),
    //                     2);
    //     JobScheduler scheduler = new JobScheduler(2);
    //     scheduler.addJob(job1);
    //     scheduler.addJob(job2);
    //     scheduler.addJob(test);
    //
    //     scheduler.startJob(job1);
    //     scheduler.startJob(job2);
    //     scheduler.startJob(test);
    // }
}
