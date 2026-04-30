package models.vehicle;

import java.time.LocalDateTime;
import models.job.Job;

public class Vehicle {
    private String vehicleId;
    private String model;
    private String vin;
    private String make;
    private String year;
    private String status;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private boolean availability;

    public Vehicle() {
        this("", "", "", "", "", "IDLE", true);
    }

    public Vehicle(
        String vehicleId,
        String model,
        String vin,
        String make,
        String year,
        String status,
        boolean availability
    ) {
        this.vehicleId = vehicleId;
        this.model = model;
        this.vin = vin;
        this.make = make;
        this.year = year;
        this.status = status == null || status.isBlank() ? "IDLE" : status;
        this.availability = availability;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

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
