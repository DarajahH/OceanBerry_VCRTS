package models.vehicle;

import java.time.LocalDateTime;
import models.job.Job;

public class Vehicle {
    private String vehicleId;
    private String status;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private boolean availability;

    public void startJob(Job job) {
        status = job == null ? "IDLE" : "IN_PROGRESS";
        availability = job == null;
    }

    public void stopJob() {
        status = "IDLE";
        availability = true;
    }

    public Checkpoint createCheckpoint() {
        return new Checkpoint(vehicleId, status, LocalDateTime.now());
    }

    public void loadCheckpoint(Checkpoint checkpoint) {
        if (checkpoint != null) {
            status = checkpoint.getStatus();
        }
    }

    public void eraseData() {
        arrivalTime = null;
        departureTime = null;
        status = "IDLE";
        availability = true;
    }
}
