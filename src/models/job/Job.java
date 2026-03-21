package models.job;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Job {
    private static final Set<String> jobIdSet = new HashSet<>();

    private final String jobId;
    private final int duration;
    private final LocalDateTime deadline;

    public Job(String jobId, int duration, LocalDateTime deadline) {
        this.jobId = jobId;
        this.duration = duration;
        this.deadline = deadline;
    }

    public static Job createJob(String jobId, int duration, LocalDateTime deadline) {
        if (jobIdSet.contains(jobId)) {
            throw new IllegalArgumentException("Job Id invalid because it already exists.");
        }

        Job createdJob = new Job(jobId, duration, deadline);
        jobIdSet.add(jobId);
        return createdJob;
    }

    public String getJobId() {
        return jobId;
    }

    public int getDuration() {
        return duration;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
}
