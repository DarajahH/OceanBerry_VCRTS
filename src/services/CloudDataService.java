package services;

import database.DatabaseService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import models.job.Job;

public class CloudDataService {
    
    private final DatabaseService db = new DatabaseService();
    private String currentUsername;

    // No paths needed anymore! 
    public CloudDataService() { }

    public synchronized List<Map<String, String>> readAllPendingRequests() throws IOException {
        try { return db.getAllPendingRequests(); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public void appendLog(String entry) throws IOException {
        try { db.insertLog(entry); } 
        catch (SQLException e) { throw new IOException("DB Error: " + e.getMessage(), e); }
    }

    public List<String> readAllLogs() throws IOException {
        try { return db.getAllLogs(); } 
        catch (SQLException e) { throw new IOException("DB Error: " + e.getMessage(), e); }
    }

    public void registerUser(String username, String password, String role) throws IOException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();
        String cleanRole = role == null ? "CLIENT" : role.trim().toUpperCase();

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) throw new IllegalArgumentException("Username and password are required.");
        if (userExists(cleanUsername)) throw new IllegalArgumentException("Username already exists.");
        if (!cleanRole.equals("CLIENT") && !cleanRole.equals("OWNER") && !cleanRole.equals("ADMIN")) cleanRole = "CLIENT";

        try { db.registerUser(cleanUsername, cleanPassword, cleanRole); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public boolean validateUser(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();
        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) return false;

        try {
            if (db.validateUser(cleanUsername, cleanPassword)) {
                currentUsername = cleanUsername;
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        try { return db.userExists(username.trim()); } 
        catch (SQLException e) { return false; }
    }

    public String getCurrentUsername() { return currentUsername; }

    public String getCurrentUserRole() {
        return currentUsername == null ? "CLIENT" : getUserRole(currentUsername);
    }

    public String getUserRole(String username) {
        if (username == null || username.trim().isEmpty()) return "CLIENT";
        try {
            String role = db.getUserRole(username.trim());
            if (role != null && !role.isBlank()) return role.toUpperCase();
        } catch (SQLException e) { e.printStackTrace(); }
        return "CLIENT";
    }

    public void appendJob(Job job) throws IOException {
        if (job == null) throw new IllegalArgumentException("Job cannot be null.");
        try { db.insertJob(job); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public List<Job> readJobs() throws IOException {
        try { return db.getAllJobs(); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public void appendVehicle(String ownerId, String vehicleInfo, int residencyHours) throws IOException {
        try { db.insertVehicle(ownerId, vehicleInfo, residencyHours); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized void writePendingRequest(String requestId, String entry, String submitter) throws IOException {
        try { db.insertPendingRequest(requestId, entry, submitter); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized Map<String, String> readPendingRequest() throws IOException {
        try { return db.getPendingRequest(); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized void writeAdminDecision(String requestId, String decision) throws IOException {
        try { db.updateAdminDecision(requestId, decision); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized String readAdminDecision(String requestId) throws IOException {
        try { return db.getAdminDecision(requestId); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    // Because SQL uses statuses (PENDING -> ACCEPTED), we no longer need to manually delete entries!
    public synchronized void clearPendingRequest() throws IOException { /* Handled by DB updates naturally */ }
    public synchronized void clearAdminDecision() throws IOException { /* Handled by DB updates naturally */ }

    public synchronized void addNotification(String username, String message) throws IOException {
        if (username == null || username.isBlank() || message == null || message.isBlank()) return;
        try { db.insertNotification(username, message); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized List<String> getUnreadNotifications(String username) throws IOException {
        if (username == null || username.isBlank()) return Collections.emptyList();
        try { return db.getUnreadNotifications(username); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public synchronized void markNotificationsRead(String username) throws IOException {
        if (username == null || username.isBlank()) return;
        try { db.markNotificationsRead(username); } 
        catch (SQLException e) { throw new IOException("DB error: " + e.getMessage(), e); }
    }

    public Map<String, String> parseLogEntry(String entry) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (entry == null || entry.isBlank()) {
            return fields;
        }

        String workingEntry = entry.trim();
        if (workingEntry.startsWith("[")) {
            int closingBracketIndex = workingEntry.indexOf(']');
            if (closingBracketIndex >= 0) {
                fields.put("TIMESTAMP", workingEntry.substring(1, closingBracketIndex));
                workingEntry = workingEntry.substring(closingBracketIndex + 1).trim();
            }
        }

        String[] tokens = workingEntry.split("\\s*\\|\\s*");
        for (String token : tokens) {
            int separatorIndex = token.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = token.substring(0, separatorIndex).trim();
            String value = token.substring(separatorIndex + 1).trim();
            fields.put(key, value);
        }

        return fields;
    }
    
}

