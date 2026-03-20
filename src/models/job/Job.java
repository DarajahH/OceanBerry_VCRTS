package models.job;

import models.enums.JobStatus;

public class Job {

    private static Set<String> jobIdSet = new Set<String>();

    private String jobId;
    private String status;
    private int redundancyLevel;
    private LocalDateTime deadline;
    private String resultStatus;
    private float progress = 0;

    // Naeem:
    public void updateProgress(float value) {
        
    }

    public static Job createJob(
        String jobId,
        JobStatus status,
        int redundancyLevel,
        LocalDateTime deadline,
        String resultStatus
    ) {
        if (Job.jobIdSet.contains(jobId)) {
            throw new IllegalArgumentException("Job Id invalid because it already exists.")
        }

        return new Job(
            jobId, status, redundancyLevel, deadline, resultStatus
        )
    }
}
