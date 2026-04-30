package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import models.enums.JobStatus;
import models.job.Job;

public class DatabaseService {

    private Connection openConnection() throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        ensureCoreTables(conn);
        return conn;
    }

    private void ensureCoreTables(Connection conn) throws SQLException {
        try (PreparedStatement usersStmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "username VARCHAR(50) NOT NULL UNIQUE, "
                    + "password VARCHAR(100) NOT NULL, "
                    + "role ENUM('CLIENT', 'OWNER', 'ADMIN') NOT NULL DEFAULT 'CLIENT', "
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)")) {
            usersStmt.execute();
        }

        try (PreparedStatement jobsStmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS jobs ("
                    + "job_id VARCHAR(50) PRIMARY KEY, "
                    + "submitter_id VARCHAR(50), "
                    + "description VARCHAR(255) NOT NULL, "
                    + "duration_hours INT NOT NULL, "
                    + "arrival_time DATETIME, "
                    + "deadline_time DATETIME, "
                    + "jobStatus ENUM('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED') DEFAULT 'QUEUED', "
                    + "completionTime INT, "
                    + "vehicle_id VARCHAR(50), "
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)")) {
            jobsStmt.execute();
        }
        runMaintenanceSql(conn, "ALTER TABLE jobs ADD COLUMN submitter_id VARCHAR(50)");
        runMaintenanceSql(conn, "ALTER TABLE jobs ADD COLUMN completionTime INT");
        runMaintenanceSql(conn, "ALTER TABLE jobs ADD COLUMN vehicle_id VARCHAR(50)");
        runMaintenanceSql(conn, "ALTER TABLE jobs ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP");

        try (PreparedStatement vehiclesStmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS vehicles ("
                    + "vehicle_id VARCHAR(50) PRIMARY KEY, "
                    + "owner_id VARCHAR(50), "
                    + "vehicle_info VARCHAR(255), "
                    + "vehicle_model VARCHAR(100), "
                    + "vehicle_vin VARCHAR(17), "
                    + "vehicle_make VARCHAR(100), "
                    + "vehicle_year VARCHAR(20), "
                    + "residency_hours INT NOT NULL DEFAULT 0, "
                    + "vehicle_status VARCHAR(50) DEFAULT 'IDLE', "
                    + "vehicle_availability VARCHAR(20) DEFAULT 'open', "
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)")) {
            vehiclesStmt.execute();
        }
        runMaintenanceSql(conn, "ALTER TABLE vehicles ADD COLUMN vehicle_model VARCHAR(100)");
        runMaintenanceSql(conn, "ALTER TABLE vehicles ADD COLUMN vehicle_vin VARCHAR(17)");
        runMaintenanceSql(conn, "ALTER TABLE vehicles ADD COLUMN vehicle_make VARCHAR(100)");
        runMaintenanceSql(conn, "ALTER TABLE vehicles ADD COLUMN vehicle_year VARCHAR(20)");
        runMaintenanceSql(conn, "ALTER TABLE vehicles ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP");
        runMaintenanceSql(conn, "ALTER TABLE vehicles MODIFY COLUMN vehicle_availability VARCHAR(20) DEFAULT 'open'");
        runMaintenanceSql(conn, "ALTER TABLE vehicles MODIFY COLUMN vehicle_vin VARCHAR(17)");
    }

    private void runMaintenanceSql(Connection conn, String sql) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void registerUser(String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
        }
    }

    public boolean validateUser(String username, String password) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public String getUserRole(String username) throws SQLException {
        String sql = "SELECT role FROM users WHERE username = ?";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }

        return "CLIENT";
    }

    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertJob(Job job) throws SQLException {
        String sql = "INSERT INTO jobs (job_id, submitter_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime, vehicle_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, job.getJobId());
            pstmt.setString(2, job.getSubmitterId());
            pstmt.setString(3, job.getDescription());
            pstmt.setInt(4, job.getDuration());
            pstmt.setTimestamp(5, job.getArrivalTime() == null ? null : Timestamp.valueOf(job.getArrivalTime()));
            pstmt.setTimestamp(6, job.getDeadline() == null ? null : Timestamp.valueOf(job.getDeadline()));
            pstmt.setString(7, job.getStatus() == null ? "QUEUED" : job.getStatus().name());

            if (job.getCompletionTime() == null) {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(8, job.getCompletionTime());
            }

            if (job.getVehicleId() == null || job.getVehicleId().isBlank()) {
                pstmt.setNull(9, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(9, job.getVehicleId());
            }

            pstmt.executeUpdate();
        }
    }

    public List<Job> getAllJobs() throws SQLException {
        String sql = "SELECT job_id, submitter_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime, vehicle_id FROM jobs";
        List<Job> jobs = new ArrayList<>();

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Timestamp arrivalTs = rs.getTimestamp("arrival_time");
                Timestamp deadlineTs = rs.getTimestamp("deadline_time");
                int completion = rs.getInt("completionTime");
                boolean completionWasNull = rs.wasNull();
                String statusStr = rs.getString("jobStatus");
                JobStatus status = (statusStr == null || statusStr.isBlank())
                    ? JobStatus.QUEUED
                    : JobStatus.valueOf(statusStr);
                String jobId = rs.getString("job_id");

                Job.registerExistingJobId(jobId);
                jobs.add(new Job(
                    jobId,
                    rs.getString("submitter_id"),
                    rs.getString("description"),
                    rs.getInt("duration_hours"),
                    arrivalTs == null ? null : arrivalTs.toLocalDateTime(),
                    deadlineTs == null ? null : deadlineTs.toLocalDateTime(),
                    status,
                    completionWasNull ? null : completion,
                    rs.getString("vehicle_id")
                ));
            }
        }

        return jobs;
    }

    public List<java.util.Map<String, String>> getAllVehicles() throws SQLException {
        String sql = "SELECT vehicle_id, owner_id, vehicle_info, vehicle_model, vehicle_vin, vehicle_make, vehicle_year, residency_hours, vehicle_status, vehicle_availability FROM vehicles ORDER BY vehicle_id ASC";
        List<java.util.Map<String, String>> vehicles = new ArrayList<>();

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, String> record = new java.util.LinkedHashMap<>();
                record.put("VEHICLE_ID", rs.getString("vehicle_id"));
                record.put("OWNER_ID", rs.getString("owner_id"));
                record.put("VEHICLE_INFO", rs.getString("vehicle_info"));
                record.put("MODEL", rs.getString("vehicle_model"));
                record.put("VIN", rs.getString("vehicle_vin"));
                record.put("MAKE", rs.getString("vehicle_make"));
                record.put("YEAR", rs.getString("vehicle_year"));
                record.put("RESIDENCY_HOURS", String.valueOf(rs.getInt("residency_hours")));
                record.put("STATUS", rs.getString("vehicle_status"));
                record.put("AVAILABILITY", rs.getString("vehicle_availability"));
                vehicles.add(record);
            }
        }

        return vehicles;
    }

    public void insertVehicle(
        String ownerId,
        String vehicleId,
        String vehicleInfo,
        String model,
        String vin,
        String make,
        String year,
        int residencyHours,
        String status,
        String availability
    ) throws SQLException {
        String sql = "REPLACE INTO vehicles (vehicle_id, owner_id, vehicle_info, vehicle_model, vehicle_vin, vehicle_make, vehicle_year, residency_hours, vehicle_status, vehicle_availability) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleId);
            pstmt.setString(2, ownerId);
            pstmt.setString(3, vehicleInfo);
            pstmt.setString(4, model);
            pstmt.setString(5, vin);
            pstmt.setString(6, make);
            pstmt.setString(7, year);
            pstmt.setInt(8, Math.max(residencyHours, 0));
            pstmt.setString(9, status);
            pstmt.setString(10, availability);
            pstmt.executeUpdate();
        }
    }
}
