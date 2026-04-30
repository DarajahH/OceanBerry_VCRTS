package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import models.enums.JobStatus;
import models.job.Job;

public class DatabaseService {
    // Register a new user in the users table
    public void registerUser(String username, String password, String role) throws SQLException {
        // Create a string variable to store the sql query to insert the user into the database with the username, password, and role as parameters
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        // Try to register the user if the username and password are not empty
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
        }
    }

    // Validate user credentials
    public boolean validateUser(String username, String password) throws SQLException {
         // Create a string variable to store the sql query to select the user from the database where the username and password match the parameters
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        // use the try loop to provide the connection to the database and the prepared statement to execute the sql query with the username and password as parameters
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the values of the prepared statement to the username and password parameters
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            // use the try loop to provide the result set to execute the query and check if the user exists in the database.
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Look up a user's role which helps to create a public string method to get the current user role
    public String getUserRole(String username) throws SQLException {//DH - This should stay
        String sql = "SELECT role FROM users WHERE username = ?";
        // use the try loop to provide the connection to the database and the prepared statement to execute the sql query with the username as a parameter
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the value of the prepared statement to the username parameter
            pstmt.setString(1, username);
            // use the try loop to provide the result set to execute the query and get the role of the user from the database.
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Return the role of the user from the database
                    return rs.getString("role");
                }
            }
        }
        return "CLIENT";
    }

    // Check if a username is already registered
    public boolean userExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Insert a new job record into jobs table
    public void insertJob(Job job) throws SQLException {
        String sql = "insert into jobs (job_id, submitter_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime, vehicle_id) "
                   + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
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

    // Read all jobs from the jobs table returning them as Job objects so we can aggregate data nicely
    public List<Job> getAllJobs() throws SQLException {
        String sql = "SELECT job_id, submitter_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime, vehicle_id FROM jobs";
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String jobId = rs.getString("job_id");
                String submitterId = rs.getString("submitter_id");
                String description = rs.getString("description");
                int duration = rs.getInt("duration_hours");
                Timestamp arrivalTs = rs.getTimestamp("arrival_time");
                Timestamp deadlineTs = rs.getTimestamp("deadline_time");
                String statusStr = rs.getString("jobStatus");
                int completion = rs.getInt("completionTime");
                boolean completionWasNull = rs.wasNull();
                String vehicleIdValue = rs.getString("vehicle_id");

                LocalDateTime arrivalTime = arrivalTs == null ? null : arrivalTs.toLocalDateTime();
                LocalDateTime deadline = deadlineTs == null ? null : deadlineTs.toLocalDateTime();
                JobStatus status = (statusStr == null || statusStr.isBlank())
                    ? JobStatus.QUEUED : JobStatus.valueOf(statusStr);
                Integer completionTime = completionWasNull ? null : completion;

                Job.registerExistingJobId(jobId);
                jobs.add(new Job(jobId, submitterId, description, duration, arrivalTime, deadline, status, completionTime, vehicleIdValue));
            }
        }
        return jobs;
    }

    public List<java.util.Map<String, String>> getAllVehicles() throws SQLException {
        String sql = "SELECT vehicle_id, owner_id, vehicle_info, vehicle_model, vehicle_vin, vehicle_make, vehicle_year, residency_hours, vehicle_status, vehicle_availability FROM vehicles ORDER BY vehicle_id ASC";
        List<java.util.Map<String, String>> vehicles = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
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

    // Insert a new vehicle submission into the vehicles table
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
        try (Connection conn = DatabaseConnection.getConnection();
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
