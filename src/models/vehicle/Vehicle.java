package models.vehicle;

 import java.time.LocalDateTime;

public class Vehicle {
    private String vehicleId;
    private String status;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private boolean availability;

    public void startJob(JobAssignment assignment) {
    }

    public void stopJob() {
    }

    public Checkpoint createCheckpoint() {
        return null;
    }

    public void loadCheckpoint(Checkpoint checkpoint) {
    }

    public void eraseData() {
    }
}