package services;

import database.DatabaseConnection;
import database.DatabaseService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import models.enums.JobStatus;
import models.job.Job;

public class CloudDataService {
    private static final String JOB_FIELD_DELIMITER = "\t";
    
    private final DatabaseService db = new DatabaseService();
    private final Path logPath;
    private final Path userPath;
    private final Path jobPath;
    private final Path pendingRequestPath;
    private final Path adminDecisionPath;
    private final Path notificationsPath;
    private final boolean databaseReady;
    private String currentUsername;

    // No paths needed anymore! 
    public CloudDataService() {
        this(Paths.get("vcrts_log.txt"), Paths.get("users.txt"));
    }

    public CloudDataService(Path logPath, Path userPath) {
        this(logPath, userPath, logPath.resolveSibling("jobs.txt"));
    }

    public CloudDataService(Path logPath, Path userPath, Path jobPath) {
        this.logPath = logPath;
        this.userPath = userPath;
        this.jobPath = jobPath;
        this.pendingRequestPath = logPath.resolveSibling("pending_request.txt");
        this.adminDecisionPath = logPath.resolveSibling("admin_decision.txt");
        this.notificationsPath = logPath.resolveSibling("notifications.txt");
        this.databaseReady = databaseAvailable();
    }

    private boolean databaseAvailable() {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized List<Map<String, String>> readAllPendingRequests() throws IOException {
        if (databaseReady) {
            try { return db.getAllPendingRequests(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readPendingRequestsFromFile();
    }

    public void appendLog(String entry) throws IOException {
        if (databaseReady) {
            try { db.insertLog(entry); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendLine(logPath, entry);
    }

    public List<String> readAllLogs() throws IOException {
        if (databaseReady) {
            try { return db.getAllLogs(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readLines(logPath);
    }

    public void registerUser(String username, String password, String role) throws IOException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();
        String cleanRole = role == null ? "CLIENT" : role.trim().toUpperCase();

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) throw new IllegalArgumentException("Username and password are required.");
        if (userExists(cleanUsername)) throw new IllegalArgumentException("Username already exists.");
        if (!cleanRole.equals("CLIENT") && !cleanRole.equals("OWNER") && !cleanRole.equals("ADMIN")) cleanRole = "CLIENT";

        if (databaseReady) {
            try { db.registerUser(cleanUsername, cleanPassword, cleanRole); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        registerUserInFile(cleanUsername, cleanPassword, cleanRole);
    }

    public boolean validateUser(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();
        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) return false;

        if (databaseReady) {
            try {
                if (db.validateUser(cleanUsername, cleanPassword)) {
                    currentUsername = cleanUsername;
                    return true;
                }
            } catch (SQLException e) { /* fall back to file */ }
        }

        if (validateUserInFile(cleanUsername, cleanPassword)) {
            currentUsername = cleanUsername;
            return true;
        }
        return false;
    }

    public boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        if (databaseReady) {
            try { return db.userExists(username.trim()); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return userExistsInFile(username.trim());
    }

    public String getCurrentUsername() { return currentUsername; }

    public String getCurrentUserRole() {
        return currentUsername == null ? "CLIENT" : getUserRole(currentUsername);
    }

    public String getUserRole(String username) {
        if (username == null || username.trim().isEmpty()) return "CLIENT";
        if (databaseReady) {
            try {
                String role = db.getUserRole(username.trim());
                if (role != null && !role.isBlank()) return role.toUpperCase();
            } catch (SQLException e) { /* fall back to file */ }
        }
        return getUserRoleInFile(username.trim());
    }

    public void appendJob(Job job) throws IOException {
        if (job == null) throw new IllegalArgumentException("Job cannot be null.");
        if (databaseReady) {
            try { db.insertJob(job); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendJobToFile(job);
    }

    public void appendJobAndLog(Job job, String logEntry) throws IOException {
        if (databaseReady) {
            try {
                if (job != null) {
                    db.insertJob(job);
                }
                if (logEntry != null && !logEntry.isBlank()) {
                    db.insertLog(logEntry);
                }
                return;
            } catch (SQLException e) {
                // fall back to file
            }
        }
        if (job != null) {
            appendJobToFile(job);
        }
        if (logEntry != null && !logEntry.isBlank()) {
            appendLine(logPath, logEntry);
        }
    }

    public List<Job> readJobs() throws IOException {
        if (databaseReady) {
            try { return db.getAllJobs(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readJobsFromFile();
    }

    public List<Map<String, String>> readAllVehicles() throws IOException {
        if (databaseReady) {
            try { return db.getAllVehicles(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readVehiclesFromFile();
    }

    public List<Map<String, String>> readClientJobRecords() throws IOException {
        List<Map<String, String>> clientJobs = new ArrayList<>();
        for (String line : readAllLogs()) {
            Map<String, String> parsedLine = parseLogEntry(line);
            if ("CLIENT".equals(parsedLine.get("ROLE"))) {
                clientJobs.add(parsedLine);
            }
        }
        return clientJobs;
    }

    public void appendVehicle(String ownerId, String vehicleInfo, int residencyHours, String status, String availability) throws IOException {
        if (databaseReady) {
            try { db.insertVehicle(ownerId, vehicleInfo, residencyHours, status, availability); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        String entry = String.format("[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | RESIDENCY:%d | STATUS:%s | AVAILABILITY:%s",
            LocalDateTime.now().toString(),
            safeFileValue(ownerId),
            safeFileValue(vehicleInfo),
            residencyHours,
            safeFileValue(status),
            safeFileValue(availability));
        appendLine(logPath, entry);
    }

    public synchronized void writePendingRequest(String requestId, String entry, String submitter) throws IOException {
        if (databaseReady) {
            try { db.insertPendingRequest(requestId, entry, submitter); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        writePendingRequestToFile(requestId, entry, submitter);
    }

    public synchronized Map<String, String> readPendingRequest() throws IOException {
        if (databaseReady) {
            try { return db.getPendingRequest(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readPendingRequestFromFile();
    }

    public synchronized void writeAdminDecision(String requestId, String decision) throws IOException {
        if (databaseReady) {
            try { db.updateAdminDecision(requestId, decision); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        writeAdminDecisionToFile(requestId, decision);
    }

    public synchronized String readAdminDecision(String requestId) throws IOException {
        if (databaseReady) {
            try { return db.getAdminDecision(requestId); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readAdminDecisionFromFile(requestId);
    }

    // Because SQL uses statuses (PENDING -> ACCEPTED), we no longer need to manually delete entries!
    public synchronized void clearPendingRequest() throws IOException {
        if (databaseReady) {
            try { db.clearPendingRequest(); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        clearFile(pendingRequestPath);
    }
    public synchronized void clearAdminDecision() throws IOException {
        if (databaseReady) {
            try { db.clearAdminDecision(); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        clearFile(adminDecisionPath);
    }

    public synchronized void addNotification(String username, String message) throws IOException {
        if (username == null || username.isBlank() || message == null || message.isBlank()) return;
        if (databaseReady) {
            try { db.insertNotification(username, message); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendLine(notificationsPath, username + "\t" + message + "\tUNREAD");
    }

    public synchronized List<String> getUnreadNotifications(String username) throws IOException {
        if (username == null || username.isBlank()) return Collections.emptyList();
        if (databaseReady) {
            try { return db.getUnreadNotifications(username); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return getUnreadNotificationsFromFile(username);
    }

    public synchronized void markNotificationsRead(String username) throws IOException {
        if (username == null || username.isBlank()) return;
        if (databaseReady) {
            try { db.markNotificationsRead(username); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        markNotificationsReadInFile(username);
    }

    private void appendLine(Path path, String entry) throws IOException {
        if (entry == null) {
            return;
        }
        Files.writeString(
            path,
            entry + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private void clearFile(Path path) throws IOException {
        Files.writeString(
            path,
            "",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private List<String> readLines(Path path) throws IOException {
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    private void registerUserInFile(String username, String password, String role) throws IOException {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        if (userExistsInFile(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }
        appendLine(userPath, username + ":" + password + ":" + role);
    }

    private boolean validateUserInFile(String username, String password) {
        try {
            if (!Files.exists(userPath)) {
                return false;
            }
            for (String line : Files.readAllLines(userPath, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    private boolean userExistsInFile(String username) {
        try {
            if (!Files.exists(userPath)) {
                return false;
            }
            for (String line : Files.readAllLines(userPath, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 1 && parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    private String getUserRoleInFile(String username) {
        try {
            if (!Files.exists(userPath)) {
                return "CLIENT";
            }
            for (String line : Files.readAllLines(userPath, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 2 && parts[0].equals(username)) {
                    if (parts.length == 3 && !parts[2].isBlank()) {
                        return parts[2].trim().toUpperCase();
                    }
                    return username.equalsIgnoreCase("admin") ? "ADMIN" : "CLIENT";
                }
            }
        } catch (IOException ignored) {}
        return username.equalsIgnoreCase("admin") ? "ADMIN" : "CLIENT";
    }

    private void appendJobToFile(Job job) throws IOException {
        appendLine(jobPath, serializeJob(job));
    }

    private List<Job> readJobsFromFile() throws IOException {
        List<Job> jobs = new ArrayList<>();
        for (String line : readLines(jobPath)) {
            Job job = parseJobLine(line);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private String serializeJob(Job job) {
        return String.join(
            JOB_FIELD_DELIMITER,
            safeFileValue(job.getJobId()),
            safeFileValue(job.getDescription()),
            Integer.toString(job.getDuration()),
            job.getArrivalTime() == null ? "" : job.getArrivalTime().toString(),
            job.getDeadline() == null ? "" : job.getDeadline().toString(),
            job.getStatus() == null ? JobStatus.QUEUED.name() : job.getStatus().name(),
            job.getCompletionTime() == null ? "" : Integer.toString(job.getCompletionTime()),
            safeFileValue(job.getVehicleId())
        );
    }

    private Job parseJobLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] fields = line.split(JOB_FIELD_DELIMITER, -1);
        if (fields.length < 7) {
            return null;
        }

        try {
            String jobId = safeFileValue(fields[0]);
            String description = safeFileValue(fields[1]);
            int duration = Integer.parseInt(fields[2]);
            LocalDateTime arrivalTime = parseDateTime(fields[3]);
            LocalDateTime deadline = parseDateTime(fields[4]);
            JobStatus status = fields[5].isBlank() ? JobStatus.QUEUED : JobStatus.valueOf(fields[5]);
            Integer completionTime = fields[6].isBlank() ? null : Integer.parseInt(fields[6]);
            String vehicleId = fields.length >= 8 ? safeFileValue(fields[7]) : null;

            Job.registerExistingJobId(jobId);
            return new Job(jobId, description, duration, arrivalTime, deadline, status, completionTime, vehicleId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<Map<String, String>> readVehiclesFromFile() throws IOException {
        Map<String, Map<String, String>> vehicles = new LinkedHashMap<>();
        for (String line : readAllLogs()) {
            Map<String, String> record = parseLogEntry(line);
            String role = normalizeRole(record.get("ROLE"));
            if (!"VEHICLE_OWNER".equals(role) && !"OWNER".equals(role)) {
                continue;
            }

            String vehicleId = safeValue(record.get("VEHICLE"));
            if (vehicleId.isBlank() || "N/A".equalsIgnoreCase(vehicleId)) {
                vehicleId = safeValue(record.get("INFO"));
            }
            if (vehicleId.isBlank()) {
                continue;
            }

            Map<String, String> vehicle = vehicles.get(vehicleId);
            if (vehicle == null) {
                vehicle = new LinkedHashMap<>();
                vehicle.put("VEHICLE_ID", vehicleId);
                vehicle.put("VEHICLE_INFO", safeValue(record.get("INFO")));
                vehicle.put("RESIDENCY_HOURS", safeValue(record.get("DURATION")));
                vehicle.put("STATUS", safeValue(record.get("STATUS")));
                vehicle.put("AVAILABILITY", safeValue(record.get("AVAILABILITY")));
                vehicles.put(vehicleId, vehicle);
            } else {
                if (!safeValue(record.get("STATUS")).equals("N/A")) {
                    vehicle.put("STATUS", safeValue(record.get("STATUS")));
                }
                if (!safeValue(record.get("AVAILABILITY")).equals("N/A")) {
                    vehicle.put("AVAILABILITY", safeValue(record.get("AVAILABILITY")));
                }
                if (!safeValue(record.get("INFO")).equals("N/A")) {
                    vehicle.put("VEHICLE_INFO", safeValue(record.get("INFO")));
                }
            }
        }

        return new ArrayList<>(vehicles.values());
    }

    private void writePendingRequestToFile(String requestId, String entry, String submitter) throws IOException {
        appendLine(pendingRequestPath, String.join(JOB_FIELD_DELIMITER, requestId, entry, submitter));
    }

    private Map<String, String> readPendingRequestFromFile() throws IOException {
        List<String> lines = readLines(pendingRequestPath);
        if (lines.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] parts = lines.get(0).split(JOB_FIELD_DELIMITER, 3);
        Map<String, String> map = new LinkedHashMap<>();
        if (parts.length >= 1) map.put("REQUEST_ID", parts[0]);
        if (parts.length >= 2) map.put("ENTRY", parts[1]);
        if (parts.length >= 3) map.put("SUBMITTER", parts[2]);
        return map;
    }

    private List<Map<String, String>> readPendingRequestsFromFile() throws IOException {
        List<Map<String, String>> requests = new ArrayList<>();
        Map<String, String> pendingRequest = readPendingRequestFromFile();
        if (!pendingRequest.isEmpty()) {
            requests.add(pendingRequest);
        }
        return requests;
    }

    private void writeAdminDecisionToFile(String requestId, String decision) throws IOException {
        clearFile(adminDecisionPath);
        appendLine(adminDecisionPath, String.join(JOB_FIELD_DELIMITER, requestId, decision));
    }

    private String readAdminDecisionFromFile(String requestId) throws IOException {
        List<String> lines = readLines(adminDecisionPath);
        if (lines.isEmpty()) {
            return "";
        }
        String[] parts = lines.get(0).split(JOB_FIELD_DELIMITER, 2);
        if (parts.length < 2) {
            return "";
        }
        if (requestId != null && !requestId.isBlank() && !requestId.equals(parts[0])) {
            return "";
        }
        return parts[1];
    }

    private List<String> getUnreadNotificationsFromFile(String username) throws IOException {
        List<String> unread = new ArrayList<>();
        for (String line : readLines(notificationsPath)) {
            String[] parts = line.split(JOB_FIELD_DELIMITER, 3);
            if (parts.length >= 3 && username.equals(parts[0]) && "UNREAD".equals(parts[2])) {
                unread.add(parts[1]);
            }
        }
        return unread;
    }

    private void markNotificationsReadInFile(String username) throws IOException {
        List<String> updated = new ArrayList<>();
        for (String line : readLines(notificationsPath)) {
            String[] parts = line.split(JOB_FIELD_DELIMITER, 3);
            if (parts.length >= 3 && username.equals(parts[0]) && "UNREAD".equals(parts[2])) {
                updated.add(parts[0] + JOB_FIELD_DELIMITER + parts[1] + JOB_FIELD_DELIMITER + "READ");
            } else {
                updated.add(line);
            }
        }
        Files.write(notificationsPath, updated, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String safeFileValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().replace(' ', '_').toUpperCase();
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
