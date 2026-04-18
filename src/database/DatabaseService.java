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
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
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
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Look up a user's role
    public String getUserRole(String username) throws SQLException {//DH - This should stay
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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
        String sql = "insert into jobs (job_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime) "
                   + "values (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(job.getJobId()));
            pstmt.setString(2, job.getDescription());
            pstmt.setInt(3, job.getDuration());
            pstmt.setTimestamp(4, job.getArrivalTime() == null ? null : Timestamp.valueOf(job.getArrivalTime()));
            pstmt.setTimestamp(5, job.getDeadline() == null ? null : Timestamp.valueOf(job.getDeadline()));
            pstmt.setString(6, job.getStatus() == null ? "QUEUED" : job.getStatus().name());
            if (job.getCompletionTime() == null) {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(7, job.getCompletionTime());
            }
            pstmt.executeUpdate();
        }
    }

    // Read all jobs from the jobs table returning them as Job objects so we can aggregate data nicely
    public List<Job> getAllJobs() throws SQLException {
        String sql = "SELECT job_id, description, duration_hours, arrival_time, deadline_time, jobStatus, completionTime FROM jobs";
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String jobId = String.valueOf(rs.getInt("job_id"));
                String description = rs.getString("description");
                int duration = rs.getInt("duration_hours");
                Timestamp arrivalTs = rs.getTimestamp("arrival_time");
                Timestamp deadlineTs = rs.getTimestamp("deadline_time");
                String statusStr = rs.getString("jobStatus");
                int completion = rs.getInt("completionTime");
                boolean completionWasNull = rs.wasNull();

                LocalDateTime arrivalTime = arrivalTs == null ? null : arrivalTs.toLocalDateTime();
                LocalDateTime deadline = deadlineTs == null ? null : deadlineTs.toLocalDateTime();
                JobStatus status = (statusStr == null || statusStr.isBlank())
                    ? JobStatus.QUEUED : JobStatus.valueOf(statusStr);
                Integer completionTime = completionWasNull ? null : completion;

                Job.registerExistingJobId(jobId);
                jobs.add(new Job(jobId, description, duration, arrivalTime, deadline, status, completionTime));
            }
        }
        return jobs;
    }

    // Insert a new vehicle submission into the vehicles table
    public void insertVehicle(String ownerId, String vehicleInfo, int residencyHours) throws SQLException {
        String sql = "INSERT INTO vehicles (owner_id, vehicle_info, residency_hours) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (ownerId == null || ownerId.isBlank()) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                    pstmt.setInt(1, Integer.parseInt(ownerId));
            }
            pstmt.setString(2, vehicleInfo);
            pstmt.setInt(3, residencyHours);
            pstmt.executeUpdate();
        }
    }

// --- NEW METHODS FOR FULL SQL MIGRATION --- DH 

    public void insertLog(String message) throws SQLException {
        String sql = "INSERT INTO logs (log_message, log_timestamp) VALUES (?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.executeUpdate();
        }
    }

    public List<String> getAllLogs() throws SQLException {
        String sql = "SELECT log_message FROM logs ORDER BY log_timestamp ASC";
        List<String> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                logs.add(rs.getString("log_message"));
            }
        }
        return logs;
    }

    public void insertPendingRequest(String requestId, String entry, String submitter) throws SQLException {
        String sql = "INSERT INTO admin_decisions (request_id, entry, submitter, decision, created_at) " +
                     "VALUES (?, ?, ?, 'PENDING', NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestId);
            pstmt.setString(2, entry);
            pstmt.setString(3, submitter);
            pstmt.executeUpdate();
        }
    }

    public java.util.Map<String, String> getPendingRequest() throws SQLException {
        String sql = "SELECT request_id, entry, submitter FROM admin_decisions WHERE decision = 'PENDING' ORDER BY created_at ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
                map.put("REQUEST_ID", rs.getString("request_id"));
                map.put("ENTRY", rs.getString("entry"));
                map.put("SUBMITTER", rs.getString("submitter"));
                return map;
            }
        }
        return java.util.Collections.emptyMap();
    }

    public void updateAdminDecision(String requestId, String decision) throws SQLException {
        String sql = "UPDATE admin_decisions SET decision = ? WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, decision);
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
        }
    }

    public String getAdminDecision(String requestId) throws SQLException {
        String sql = "SELECT decision FROM admin_decisions WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("decision");
                }
            }
        }
        return null;
    }

    public void insertNotification(String username, String message) throws SQLException {
        String sql = "INSERT INTO notifications (username, notification_message, notification_timestamp, status) VALUES (?, ?, NOW(), 'UNREAD')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        }
    }

    public List<String> getUnreadNotifications(String username) throws SQLException {
        String sql = "SELECT notification_message FROM notifications WHERE username = ? AND status = 'UNREAD' ORDER BY notification_timestamp ASC";
        List<String> notifs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifs.add(rs.getString("notification_message"));
                }
            }
        }
        return notifs;
    }

    public void markNotificationsRead(String username) throws SQLException {
        String sql = "UPDATE notifications SET status = 'READ' WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

}
