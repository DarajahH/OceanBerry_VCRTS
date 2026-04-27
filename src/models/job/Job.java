package models.job;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import models.enums.JobStatus;

public class Job {
    private static final Set<String> jobIdSet = new HashSet<>();

    private final String jobId;
    private final String description;
    private final int duration;
    private final LocalDateTime arrivalTime;
    private final LocalDateTime deadline;
    private final String vehicleId;
    private JobStatus status;
    private Integer completionTime;

    public Job(
        String jobId,
        String description,
        int duration,
        LocalDateTime arrivalTime,
        LocalDateTime deadline,
        JobStatus status,
        Integer completionTime
    ) {
        this(jobId, description, duration, arrivalTime, deadline, status, completionTime, null);
    }

    public Job(
        String jobId,
        String description,
        int duration,
        LocalDateTime arrivalTime,
        LocalDateTime deadline,
        JobStatus status,
        Integer completionTime,
        String vehicleId
    ) {
        this.jobId = jobId;
        this.description = description;
        this.duration = duration;
        this.arrivalTime = arrivalTime;
        this.deadline = deadline;
        this.vehicleId = vehicleId;
        this.status = status;
        this.completionTime = completionTime;
    }

    public static Job createJob(
        String jobId,
        String description,
        int duration,
        LocalDateTime arrivalTime,
        LocalDateTime deadline
    ) {
        if (jobIdSet.contains(jobId)) {
            throw new IllegalArgumentException("Job Id invalid because it already exists.");
        }

        Job createdJob = new Job(
            jobId,
            description,
            duration,
            arrivalTime,
            deadline,
            JobStatus.QUEUED,
            null,
            null
        );
        jobIdSet.add(jobId);
        return createdJob;
    }

    public static Job createJob(
        String jobId,
        String description,
        int duration,
        LocalDateTime arrivalTime,
        LocalDateTime deadline,
        String vehicleId
    ) {
        if (jobIdSet.contains(jobId)) {
            throw new IllegalArgumentException("Job Id invalid because it already exists.");
        }

        Job createdJob = new Job(
            jobId,
            description,
            duration,
            arrivalTime,
            deadline,
            JobStatus.QUEUED,
            null,
            vehicleId
        );
        jobIdSet.add(jobId);
        return createdJob;
    }

    public static void registerExistingJobId(String jobId) {
        if (jobId != null && !jobId.isBlank()) {
            jobIdSet.add(jobId);
        }
    }

    public String getJobId() {
        return jobId;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Integer getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Integer completionTime) {
        this.completionTime = completionTime;
    }
}
