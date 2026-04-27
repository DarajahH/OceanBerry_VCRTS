package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import models.enums.JobStatus;
import models.job.Job;

public class DatabaseService {

    private void ensureWorkflowTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS logs ("
                    + "log_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "log_message VARCHAR(255) NOT NULL, "
                    + "log_timestamp DATETIME NOT NULL)"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notifications ("
                    + "notification_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "notification_message VARCHAR(255) NOT NULL, "
                    + "notification_timestamp DATETIME NOT NULL)"
            );
            try {
                stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN username VARCHAR(50) NOT NULL DEFAULT ''");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE notifications ADD COLUMN status ENUM('UNREAD', 'READ') DEFAULT 'UNREAD'");
            } catch (SQLException ignored) {}

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admin_decisions ("
                    + "request_id VARCHAR(50) PRIMARY KEY, "
                    + "entry TEXT NOT NULL, "
                    + "submitter VARCHAR(50), "
                    + "decision ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING', "
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
            try {
                stmt.executeUpdate("ALTER TABLE admin_decisions DROP COLUMN user_id");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE admin_decisions MODIFY COLUMN request_id VARCHAR(50) NOT NULL");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE admin_decisions ADD COLUMN submitter VARCHAR(50)");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate(
                    "ALTER TABLE admin_decisions ADD COLUMN decision "
                        + "ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING'"
                );
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE admin_decisions ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException ignored) {}
        }
    }

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

    public List<java.util.Map<String, String>> getAllPendingRequests() throws SQLException {
        String sql = "SELECT request_id, entry, submitter FROM admin_decisions WHERE decision = 'PENDING' ORDER BY created_at ASC";
        List<java.util.Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
                    map.put("REQUEST_ID", rs.getString("request_id"));
                    map.put("ENTRY", rs.getString("entry"));
                    map.put("SUBMITTER", rs.getString("submitter"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    // Insert a new vehicle submission into the vehicles table
    public void insertVehicle(String ownerId, String vehicleInfo, int residencyHours, String status, String availability) throws SQLException {
        String sql = "INSERT INTO vehicles (owner_id, vehicle_info, residency_hours, vehicle_status, vehicle_availability) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (ownerId == null || ownerId.isBlank()) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, Integer.parseInt(ownerId));
            }
            pstmt.setString(2, vehicleInfo);
            pstmt.setInt(3, residencyHours);
            pstmt.setString(4, status);

            if ("true".equalsIgnoreCase(availability)
                    || "yes".equalsIgnoreCase(availability)
                    || "available".equalsIgnoreCase(availability)
                    || "1".equals(availability)) {
                pstmt.setBoolean(5, true);
            } else {
                pstmt.setBoolean(5, false);
            }

            pstmt.executeUpdate();
        }
    }

// --- NEW METHODS FOR FULL SQL MIGRATION --- DH 

    public void insertLog(String message) throws SQLException {
        String sql = "INSERT INTO logs (log_message, log_timestamp) VALUES (?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.executeUpdate();
            }
        }
    }

    public List<String> getAllLogs() throws SQLException {
        String sql = "SELECT log_message FROM logs ORDER BY log_timestamp ASC";
        List<String> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(rs.getString("log_message"));
                }
            }
        }
        return logs;
    }

    public void insertPendingRequest(String requestId, String entry, String submitter) throws SQLException {
        String sql = "INSERT INTO admin_decisions (request_id, entry, submitter, decision, created_at) " +
                     "VALUES (?, ?, ?, 'PENDING', NOW())";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestId);
            pstmt.setString(2, entry);
            pstmt.setString(3, submitter);
            pstmt.executeUpdate();
            }
        }
    }

    public java.util.Map<String, String> getPendingRequest() throws SQLException {
        String sql = "SELECT request_id, entry, submitter FROM admin_decisions WHERE decision = 'PENDING' ORDER BY created_at ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
                    map.put("REQUEST_ID", rs.getString("request_id"));
                    map.put("ENTRY", rs.getString("entry"));
                    map.put("SUBMITTER", rs.getString("submitter"));
                    return map;
                }
            }
        }
        return java.util.Collections.emptyMap();
    }

    public void updateAdminDecision(String requestId, String decision) throws SQLException {
        String sql = "UPDATE admin_decisions SET decision = ? WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, decision);
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
            }
        }
    }

    public String getAdminDecision(String requestId) throws SQLException {
        String sql = "SELECT decision FROM admin_decisions WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, requestId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("decision");
                    }
                }
            }
        }
        return null;
    }

    public void insertNotification(String username, String message) throws SQLException {
        String sql = "INSERT INTO notifications (username, notification_message, notification_timestamp, status) VALUES (?, ?, NOW(), 'UNREAD')";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            }
        }
    }

    public List<String> getUnreadNotifications(String username) throws SQLException {
        String sql = "SELECT notification_message FROM notifications WHERE username = ? AND status = 'UNREAD' ORDER BY notification_timestamp ASC";
        List<String> notifs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        notifs.add(rs.getString("notification_message"));
                    }
                }
            }
        }
        return notifs;
    }

    public void markNotificationsRead(String username) throws SQLException {
        String sql = "UPDATE notifications SET status = 'READ' WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureWorkflowTables(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }
        }
    }

}
