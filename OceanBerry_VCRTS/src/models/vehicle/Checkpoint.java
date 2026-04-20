package models.vehicle;

import java.time.LocalDateTime;

public class Checkpoint {
    private final String vehicleId;
    private final String status;
    private final LocalDateTime createdAt;

    public Checkpoint(String vehicleId, String status, LocalDateTime createdAt) {
        this.vehicleId = vehicleId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
