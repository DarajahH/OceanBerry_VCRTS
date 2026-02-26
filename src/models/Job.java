package models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Job {
    private final String jobId;
    private final String clientId;
    private final String description;
    private final int approxDurationHours;
    private LocalDateTime deadline;
    private final BigDecimal costForJob;
    private int interrupts;
    private int failures;
    private JobStatus status;
    private final LocalDateTime createdAt;
    private final List<String> reportLines;
    private int priorityRank;
    private String assignedVehicleId;

    public Job(
            String jobId,
            String clientId,
            String description,
            int approxDurationHours,
            LocalDateTime deadline,
            BigDecimal costForJob,
            int interrupts,
            int failures,
            JobStatus status,
            LocalDateTime createdAt,
            int priorityRank) {
        this.jobId = jobId;
        this.clientId = clientId;
        this.description = description;
        this.approxDurationHours = approxDurationHours;
        this.deadline = deadline;
        this.costForJob = costForJob;
        this.interrupts = interrupts;
        this.failures = failures;
        this.status = status;
        this.createdAt = createdAt;
        this.reportLines = new ArrayList<>();
        this.priorityRank = priorityRank;
        this.assignedVehicleId = "";
    }

    public String getJobId() {
        return jobId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSubmitterUserId() {
        return clientId;
    }

    public String getDescription() {
        return description;
    }

    public int getApproxDurationHours() {
        return approxDurationHours;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getCostForJob() {
        return costForJob;
    }

    public int getInterrupts() {
        return interrupts;
    }

    public void setInterrupts(int interrupts) {
        this.interrupts = interrupts;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void addReportLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        reportLines.add(line.trim());
    }

    public List<String> getReportLines() {
        return Collections.unmodifiableList(reportLines);
    }

    public int getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(int priorityRank) {
        this.priorityRank = priorityRank;
    }

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(String assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId == null ? "" : assignedVehicleId.trim();
    }
}
