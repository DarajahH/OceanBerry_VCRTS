package models;

import java.math.BigDecimal;

public class Vehicle {
    private final String vehicleId;
    private final String ownerUserId;
    private final String vehicleInfo;
    private final int residencyHours;
    private int jobsWorked;
    private int jobsInProgress;
    private BigDecimal totalEarnings;

    public Vehicle(
            String vehicleId,
            String ownerUserId,
            String vehicleInfo,
            int residencyHours,
            int jobsWorked,
            int jobsInProgress,
            BigDecimal totalEarnings) {
        this.vehicleId = vehicleId;
        this.ownerUserId = ownerUserId;
        this.vehicleInfo = vehicleInfo;
        this.residencyHours = residencyHours;
        this.jobsWorked = jobsWorked;
        this.jobsInProgress = jobsInProgress;
        this.totalEarnings = totalEarnings == null ? BigDecimal.ZERO : totalEarnings;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getVehicleInfo() {
        return vehicleInfo;
    }

    public int getResidencyHours() {
        return residencyHours;
    }

    public int getJobsWorked() {
        return jobsWorked;
    }

    public void setJobsWorked(int jobsWorked) {
        this.jobsWorked = jobsWorked;
    }

    public int getJobsInProgress() {
        return jobsInProgress;
    }

    public void setJobsInProgress(int jobsInProgress) {
        this.jobsInProgress = jobsInProgress;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void addEarnings(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        totalEarnings = totalEarnings.add(amount);
    }
}
