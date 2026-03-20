package models.job;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import models.enums.JobStatus;

public class Job {
    private static final Set<String> jobIdSet = new HashSet<>();

    private final String jobId;
    private JobStatus status;
    private int redundancyLevel;
    private LocalDateTime deadline;
    private String resultStatus;
    private float progress;

    public Job(
        String jobId,
        JobStatus status,
        int redundancyLevel,
        LocalDateTime deadline,
        String resultStatus
    ) {
        this.jobId = jobId;
        this.status = status;
        this.redundancyLevel = redundancyLevel;
        this.deadline = deadline;
        this.resultStatus = resultStatus;
        this.progress = 0;
    }

    public void updateProgress(float value) {
        progress = Math.max(0, Math.min(100, value));
    }

    public static Job createJob(
        String jobId,
        JobStatus status,
        int redundancyLevel,
        LocalDateTime deadline,
        String resultStatus
    ) {
        if (jobIdSet.contains(jobId)) {
            throw new IllegalArgumentException("Job Id invalid because it already exists.");
        }

        Job createdJob = new Job(jobId, status, redundancyLevel, deadline, resultStatus);
        jobIdSet.add(jobId);
        return createdJob;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getRedundancyLevel() {
        return redundancyLevel;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public float getProgress() {
        return progress;
    }
}
