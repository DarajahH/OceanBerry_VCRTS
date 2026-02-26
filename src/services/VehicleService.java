package services;

import models.Vehicle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VehicleService {
    public static final int MAX_VEHICLES_PER_OWNER = 3;

    private final Map<String, List<Vehicle>> vehiclesByOwner = new LinkedHashMap<>();

    public synchronized Vehicle registerVehicle(
            String ownerUserId,
            String vehicleInfo,
            int residencyHours) {
        List<Vehicle> ownerVehicles = vehiclesByOwner.computeIfAbsent(ownerUserId, key -> new ArrayList<>());
        if (ownerVehicles.size() >= MAX_VEHICLES_PER_OWNER) {
            throw new IllegalStateException("Vehicle limit reached (max 3 per vehicle owner).");
        }

        String vehicleId = String.format("V-%04d", countVehicles() + 1);
        Vehicle vehicle = new Vehicle(
                vehicleId,
                ownerUserId,
                vehicleInfo,
                residencyHours,
                0,
                0,
                BigDecimal.ZERO);
        ownerVehicles.add(vehicle);
        return vehicle;
    }

    public synchronized List<Vehicle> getVehiclesForOwner(String ownerUserId) {
        List<Vehicle> vehicles = vehiclesByOwner.get(ownerUserId);
        if (vehicles == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(vehicles);
    }

    public synchronized List<Vehicle> getAllVehicles() {
        List<Vehicle> all = new ArrayList<>();
        for (List<Vehicle> ownerVehicles : vehiclesByOwner.values()) {
            all.addAll(ownerVehicles);
        }
        return all;
    }

    public synchronized boolean removeVehicle(String vehicleId) {
        for (List<Vehicle> ownerVehicles : vehiclesByOwner.values()) {
            for (int i = 0; i < ownerVehicles.size(); i++) {
                if (ownerVehicles.get(i).getVehicleId().equalsIgnoreCase(vehicleId)) {
                    ownerVehicles.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized BigDecimal totalEarningsForOwner(String ownerUserId) {
        BigDecimal total = BigDecimal.ZERO;
        List<Vehicle> vehicles = vehiclesByOwner.get(ownerUserId);
        if (vehicles == null) {
            return total;
        }
        for (Vehicle vehicle : vehicles) {
            total = total.add(vehicle.getTotalEarnings());
        }
        return total;
    }

    public synchronized boolean addVehicleEarnings(String vehicleId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return false;
        }
        for (List<Vehicle> ownerVehicles : vehiclesByOwner.values()) {
            for (Vehicle vehicle : ownerVehicles) {
                if (vehicle.getVehicleId().equalsIgnoreCase(vehicleId)) {
                    vehicle.addEarnings(amount);
                    return true;
                }
            }
        }
        return false;
    }

    private int countVehicles() {
        int count = 0;
        for (List<Vehicle> vehicles : vehiclesByOwner.values()) {
            count += vehicles.size();
        }
        return count;
    }
}
