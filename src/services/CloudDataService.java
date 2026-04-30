package services;

import database.DatabaseService;
import database.DatabaseConnection;
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
    private final Path vehiclePath;
    private final Path pendingRequestPath;
    private final Path notificationsPath;
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
        this.vehiclePath = logPath.resolveSibling("vehicles.txt");
        this.pendingRequestPath = logPath.resolveSibling("pending_request.txt");
        this.notificationsPath = logPath.resolveSibling("notifications.txt");
    }

    private boolean databaseAvailable() {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean shouldUseDatabase() {
        return databaseAvailable();
    }

    public synchronized List<Map<String, String>> readAllPendingRequests() throws IOException {
        if (shouldUseDatabase()) {
            try { return db.getAllPendingRequests(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readPendingRequestsFromFile();
    }

    public void appendLog(String entry) throws IOException {
        if (shouldUseDatabase()) {
            try { db.insertLog(entry); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendLine(logPath, entry);
    }

    public List<String> readAllLogs() throws IOException {
        if (shouldUseDatabase()) {
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

        if (shouldUseDatabase()) {
            try { db.registerUser(cleanUsername, cleanPassword, cleanRole); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        registerUserInFile(cleanUsername, cleanPassword, cleanRole);
    }

    public boolean validateUser(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();
        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) return false;

        if (shouldUseDatabase()) {
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
        if (shouldUseDatabase()) {
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
        if (shouldUseDatabase()) {
            try {
                String role = db.getUserRole(username.trim());
                if (role != null && !role.isBlank()) return role.toUpperCase();
            } catch (SQLException e) { /* fall back to file */ }
        }
        return getUserRoleInFile(username.trim());
    }

    public void appendJob(Job job) throws IOException {
        if (job == null) throw new IllegalArgumentException("Job cannot be null.");
        if (shouldUseDatabase()) {
            try { db.insertJob(job); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendJobToFile(job);
    }

    public void appendJobAndLog(Job job, String logEntry) throws IOException {
        if (shouldUseDatabase()) {
            boolean wroteJobToDatabase = false;
            try {
                if (job != null) {
                    db.insertJob(job);
                    wroteJobToDatabase = true;
                }
                if (logEntry != null && !logEntry.isBlank()) {
                    db.insertLog(logEntry);
                }
                return;
            } catch (SQLException e) {
                if (wroteJobToDatabase) {
                    if (logEntry != null && !logEntry.isBlank()) {
                        appendLine(logPath, logEntry);
                    }
                    return;
                }
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
        if (shouldUseDatabase()) {
            try { return db.getAllJobs(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readJobsFromFile();
    }

    public List<Map<String, String>> readAllVehicles() throws IOException {
        if (shouldUseDatabase()) {
            try { return db.getAllVehicles(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readVehiclesFromFile();
    }

    public List<Map<String, String>> readClientJobRecords() throws IOException {
        List<Map<String, String>> clientJobs = new ArrayList<>();
        for (Job job : readJobs()) {
            clientJobs.add(toJobRecord(job));
        }
        return clientJobs;
    }

    public void appendVehicle(
        String ownerId,
        String vehicleId,
        String model,
        String vin,
        String make,
        String year,
        Integer residencyHours,
        String status,
        String availability
    ) throws IOException {
        Map<String, String> existing = findExistingVehicleRecord(vehicleId);
        String resolvedOwnerId = firstNonBlank(ownerId, existing.get("OWNER_ID"), "");
        String resolvedVehicleId = firstNonBlank(vehicleId, existing.get("VEHICLE_ID"), "");
        String resolvedModel = firstNonBlank(model, existing.get("MODEL"), "");
        String resolvedVin = firstNonBlank(vin, existing.get("VIN"), "");
        String resolvedMake = firstNonBlank(make, existing.get("MAKE"), "");
        String resolvedYear = firstNonBlank(year, existing.get("YEAR"), "");
        int resolvedResidencyHours = residencyHours != null
            ? Math.max(residencyHours, 0)
            : parseInteger(firstNonBlank(existing.get("RESIDENCY_HOURS"), "0", "0"), 0);
        String resolvedStatus = firstNonBlank(status, existing.get("STATUS"), "IDLE");
        String resolvedAvailability = firstNonBlank(availability, existing.get("AVAILABILITY"), "open");
        String resolvedVehicleInfo = composeVehicleInfo(
            resolvedVehicleId,
            resolvedMake,
            resolvedModel,
            resolvedYear,
            resolvedVin
        );

        if (shouldUseDatabase()) {
            try {
                db.insertVehicle(
                    resolvedOwnerId,
                    resolvedVehicleId,
                    resolvedVehicleInfo,
                    resolvedModel,
                    resolvedVin,
                    resolvedMake,
                    resolvedYear,
                    resolvedResidencyHours,
                    resolvedStatus,
                    resolvedAvailability
                );
                return;
            } catch (SQLException e) { /* fall back to file */ }
        }
        writeVehicleToFile(
            resolvedOwnerId,
            resolvedVehicleId,
            resolvedModel,
            resolvedVin,
            resolvedMake,
            resolvedYear,
            resolvedResidencyHours,
            resolvedStatus,
            resolvedAvailability
        );
    }

    public synchronized void writePendingRequest(String requestId, String entry, String submitter) throws IOException {
        if (shouldUseDatabase()) {
            try { db.insertPendingRequest(requestId, entry, submitter); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        writePendingRequestToFile(requestId, entry, submitter);
    }

    public synchronized Map<String, String> readPendingRequest() throws IOException {
        if (shouldUseDatabase()) {
            try { return db.getPendingRequest(); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readPendingRequestFromFile();
    }

    public synchronized void writeAdminDecision(String requestId, String decision) throws IOException {
        if (shouldUseDatabase()) {
            try { db.updateAdminDecision(requestId, decision); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        writeAdminDecisionToFile(requestId, decision);
    }

    public synchronized String readAdminDecision(String requestId) throws IOException {
        if (shouldUseDatabase()) {
            try { return db.getAdminDecision(requestId); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return readAdminDecisionFromFile(requestId);
    }

    // Because SQL uses statuses (PENDING -> ACCEPTED), we no longer need to manually delete entries!
    public synchronized void clearPendingRequest() throws IOException {
        if (shouldUseDatabase()) {
            try { db.clearPendingRequest(); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        clearFile(pendingRequestPath);
    }

    public synchronized void clearPendingRequest(String requestId) throws IOException {
        if (requestId == null || requestId.isBlank()) {
            clearPendingRequest();
            return;
        }
        if (shouldUseDatabase()) {
            try { db.clearPendingRequest(requestId); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        removeMatchingFileLine(pendingRequestPath, requestId);
    }

    public synchronized void clearAdminDecision() throws IOException {
        if (shouldUseDatabase()) {
            try { db.clearAdminDecision(); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        clearAdminDecisionFromLogFile(null);
    }

    public synchronized void clearAdminDecision(String requestId) throws IOException {
        if (requestId == null || requestId.isBlank()) {
            clearAdminDecision();
            return;
        }
        if (shouldUseDatabase()) {
            try { db.clearAdminDecision(requestId); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        clearAdminDecisionFromLogFile(requestId);
    }

    public synchronized void addNotification(String username, String message) throws IOException {
        if (username == null || username.isBlank() || message == null || message.isBlank()) return;
        if (shouldUseDatabase()) {
            try { db.insertNotification(username, message); return; }
            catch (SQLException e) { /* fall back to file */ }
        }
        appendLine(notificationsPath, username + "\t" + message + "\tUNREAD");
    }

    public synchronized List<String> getUnreadNotifications(String username) throws IOException {
        if (username == null || username.isBlank()) return Collections.emptyList();
        if (shouldUseDatabase()) {
            try { return db.getUnreadNotifications(username); }
            catch (SQLException e) { /* fall back to file */ }
        }
        return getUnreadNotificationsFromFile(username);
    }

    public synchronized void markNotificationsRead(String username) throws IOException {
        if (username == null || username.isBlank()) return;
        if (shouldUseDatabase()) {
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

    private void removeMatchingFileLine(Path path, String firstColumnValue) throws IOException {
        List<String> updated = new ArrayList<>();
        for (String line : readLines(path)) {
            String[] parts = line.split(JOB_FIELD_DELIMITER, 2);
            if (parts.length >= 1 && parts[0].equals(firstColumnValue)) {
                continue;
            }
            updated.add(line);
        }
        Files.write(path, updated, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
            safeFileValue(job.getSubmitterId()),
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
            boolean legacyRow = fields.length < 9;
            String submitterId = legacyRow ? null : safeFileValue(fields[1]);
            String description = safeFileValue(fields[legacyRow ? 1 : 2]);
            int duration = Integer.parseInt(fields[legacyRow ? 2 : 3]);
            LocalDateTime arrivalTime = parseDateTime(fields[legacyRow ? 3 : 4]);
            LocalDateTime deadline = parseDateTime(fields[legacyRow ? 4 : 5]);
            JobStatus status = fields[legacyRow ? 5 : 6].isBlank()
                ? JobStatus.QUEUED : JobStatus.valueOf(fields[legacyRow ? 5 : 6]);
            Integer completionTime = fields[legacyRow ? 6 : 7].isBlank()
                ? null : Integer.parseInt(fields[legacyRow ? 6 : 7]);
            String vehicleId = fields.length >= (legacyRow ? 8 : 9)
                ? safeFileValue(fields[legacyRow ? 7 : 8]) : null;

            Job.registerExistingJobId(jobId);
            return new Job(jobId, submitterId, description, duration, arrivalTime, deadline, status, completionTime, vehicleId);
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
        List<Map<String, String>> vehiclesFromFile = readVehicleRecordsFromFile();
        if (!vehiclesFromFile.isEmpty()) {
            return vehiclesFromFile;
        }

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
                vehicle.put("OWNER_ID", safeValue(record.get("ID")));
                vehicle.put("MODEL", safeValue(record.get("MODEL")));
                vehicle.put("VIN", safeValue(record.get("VIN")));
                vehicle.put("MAKE", safeValue(record.get("MAKE")));
                vehicle.put("YEAR", safeValue(record.get("YEAR")));
                vehicle.put("VEHICLE_INFO", composeVehicleInfo(
                    vehicleId,
                    safeFileValue(record.get("MAKE")),
                    safeFileValue(record.get("MODEL")),
                    safeFileValue(record.get("YEAR")),
                    safeFileValue(record.get("VIN"))
                ));
                vehicle.put("RESIDENCY_HOURS", firstNonBlank(record.get("RESIDENCY"), record.get("DURATION"), "0"));
                vehicle.put("STATUS", safeValue(record.get("STATUS")));
                vehicle.put("AVAILABILITY", safeValue(record.get("AVAILABILITY")));
                vehicles.put(vehicleId, vehicle);
            } else {
                if (!safeValue(record.get("ID")).equals("N/A")) {
                    vehicle.put("OWNER_ID", safeValue(record.get("ID")));
                }
                if (!safeValue(record.get("STATUS")).equals("N/A")) {
                    vehicle.put("STATUS", safeValue(record.get("STATUS")));
                }
                if (!safeValue(record.get("AVAILABILITY")).equals("N/A")) {
                    vehicle.put("AVAILABILITY", safeValue(record.get("AVAILABILITY")));
                }
                if (!safeValue(record.get("MODEL")).equals("N/A")) {
                    vehicle.put("MODEL", safeValue(record.get("MODEL")));
                }
                if (!safeValue(record.get("VIN")).equals("N/A")) {
                    vehicle.put("VIN", safeValue(record.get("VIN")));
                }
                if (!safeValue(record.get("MAKE")).equals("N/A")) {
                    vehicle.put("MAKE", safeValue(record.get("MAKE")));
                }
                if (!safeValue(record.get("YEAR")).equals("N/A")) {
                    vehicle.put("YEAR", safeValue(record.get("YEAR")));
                }
                if (!safeValue(record.get("INFO")).equals("N/A")) {
                    vehicle.put("VEHICLE_INFO", safeValue(record.get("INFO")));
                }
                String residency = firstNonBlank(record.get("RESIDENCY"), record.get("DURATION"), null);
                if (residency != null) {
                    vehicle.put("RESIDENCY_HOURS", residency);
                }
            }
            vehicle.put("VEHICLE_INFO", composeVehicleInfo(
                safeFileValue(vehicle.get("VEHICLE_ID")),
                safeFileValue(vehicle.get("MAKE")),
                safeFileValue(vehicle.get("MODEL")),
                safeFileValue(vehicle.get("YEAR")),
                safeFileValue(vehicle.get("VIN"))
            ));
        }

        return new ArrayList<>(vehicles.values());
    }

    private void writeVehicleToFile(
        String ownerId,
        String vehicleId,
        String model,
        String vin,
        String make,
        String year,
        int residencyHours,
        String status,
        String availability
    )
        throws IOException {
        List<String> updated = new ArrayList<>();
        boolean replaced = false;
        for (String line : readLines(vehiclePath)) {
            String[] parts = line.split(JOB_FIELD_DELIMITER, -1);
            if (parts.length >= 1 && parts[0].equals(vehicleId)) {
                updated.add(String.join(
                    JOB_FIELD_DELIMITER,
                    safeFileValue(vehicleId),
                    safeFileValue(ownerId),
                    safeFileValue(model),
                    safeFileValue(vin),
                    safeFileValue(make),
                    safeFileValue(year),
                    Integer.toString(Math.max(residencyHours, 0)),
                    safeFileValue(status),
                    safeFileValue(availability)
                ));
                replaced = true;
            } else {
                updated.add(line);
            }
        }
        if (!replaced) {
            updated.add(String.join(
                JOB_FIELD_DELIMITER,
                safeFileValue(vehicleId),
                safeFileValue(ownerId),
                safeFileValue(model),
                safeFileValue(vin),
                safeFileValue(make),
                safeFileValue(year),
                Integer.toString(Math.max(residencyHours, 0)),
                safeFileValue(status),
                safeFileValue(availability)
            ));
        }
        Files.write(vehiclePath, updated, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<Map<String, String>> readVehicleRecordsFromFile() throws IOException {
        List<Map<String, String>> vehicles = new ArrayList<>();
        for (String line : readLines(vehiclePath)) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] parts = line.split(JOB_FIELD_DELIMITER, -1);
            if (parts.length < 5) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            record.put("VEHICLE_ID", safeValue(parts[0]));
            record.put("OWNER_ID", safeValue(parts[1]));
            if (parts.length >= 9) {
                record.put("MODEL", safeValue(parts[2]));
                record.put("VIN", safeValue(parts[3]));
                record.put("MAKE", safeValue(parts[4]));
                record.put("YEAR", safeValue(parts[5]));
                record.put("RESIDENCY_HOURS", safeValue(parts[6]));
                record.put("STATUS", safeValue(parts[7]));
                record.put("AVAILABILITY", safeValue(parts[8]));
            } else {
                record.put("MODEL", "N/A");
                record.put("VIN", "N/A");
                record.put("MAKE", "N/A");
                record.put("YEAR", "N/A");
                record.put("RESIDENCY_HOURS", safeValue(parts[2]));
                record.put("STATUS", safeValue(parts[3]));
                record.put("AVAILABILITY", safeValue(parts[4]));
            }
            record.put("VEHICLE_INFO", composeVehicleInfo(
                safeFileValue(record.get("VEHICLE_ID")),
                safeFileValue(record.get("MAKE")),
                safeFileValue(record.get("MODEL")),
                safeFileValue(record.get("YEAR")),
                safeFileValue(record.get("VIN"))
            ));
            vehicles.add(record);
        }
        return vehicles;
    }

    private void writePendingRequestToFile(String requestId, String entry, String submitter) throws IOException {
        appendLine(pendingRequestPath, String.join(JOB_FIELD_DELIMITER, requestId, entry, submitter));
    }

    private Map<String, String> readPendingRequestFromFile() throws IOException {
        List<Map<String, String>> requests = readPendingRequestsFromFile();
        if (requests.isEmpty()) {
            return Collections.emptyMap();
        }
        return requests.get(0);
    }

    private List<Map<String, String>> readPendingRequestsFromFile() throws IOException {
        List<Map<String, String>> requests = new ArrayList<>();
        for (String line : readLines(pendingRequestPath)) {
            String[] parts = line.split(JOB_FIELD_DELIMITER, 3);
            if (parts.length < 3) {
                continue;
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("REQUEST_ID", parts[0]);
            map.put("ENTRY", parts[1]);
            map.put("SUBMITTER", parts[2]);
            requests.add(map);
        }
        return requests;
    }

    private void writeAdminDecisionToFile(String requestId, String decision) throws IOException {
        appendLine(logPath, serializeAdminDecisionEntry(requestId, decision));
    }

    private String readAdminDecisionFromFile(String requestId) throws IOException {
        List<String> lines = readLines(logPath);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String decision = parseAdminDecisionEntry(lines.get(i), requestId);
            if (decision != null) {
                return decision;
            }
        }
        return "";
    }

    private void clearAdminDecisionFromLogFile(String requestId) throws IOException {
        List<String> updated = new ArrayList<>();
        for (String line : readLines(logPath)) {
            if (parseAdminDecisionEntry(line, requestId) != null) {
                continue;
            }
            updated.add(line);
        }
        Files.write(logPath, updated, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String serializeAdminDecisionEntry(String requestId, String decision) {
        return String.join(
            JOB_FIELD_DELIMITER,
            "ADMIN_DECISION",
            safeFileValue(requestId),
            safeFileValue(decision)
        );
    }

    private String parseAdminDecisionEntry(String line, String requestId) {
        String[] parts = line.split(JOB_FIELD_DELIMITER, 3);
        if (parts.length < 3 || !"ADMIN_DECISION".equals(parts[0])) {
            return null;
        }
        if (requestId == null || requestId.isBlank() || requestId.equals(parts[1])) {
            return parts[2];
        }
        return null;
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

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
    }

    private int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Map<String, String> findExistingVehicleRecord(String vehicleId) throws IOException {
        if (vehicleId == null || vehicleId.isBlank()) {
            return Collections.emptyMap();
        }
        for (Map<String, String> record : readAllVehicles()) {
            if (vehicleId.trim().equals(safeFileValue(record.get("VEHICLE_ID")))) {
                return record;
            }
        }
        return Collections.emptyMap();
    }

    private String composeVehicleInfo(String vehicleId, String make, String model, String year, String vin) {
        List<String> segments = new ArrayList<>();
        if (make != null && !make.isBlank()) {
            segments.add(make.trim());
        }
        if (model != null && !model.isBlank()) {
            segments.add(model.trim());
        }
        if (year != null && !year.isBlank()) {
            segments.add(year.trim());
        }
        if (vin != null && !vin.isBlank()) {
            segments.add("VIN " + vin.trim());
        }
        if (!segments.isEmpty()) {
            return String.join(" ", segments);
        }
        return safeFileValue(vehicleId);
    }

    private Map<String, String> toJobRecord(Job job) {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("ROLE", "CLIENT");
        record.put("JOB_ID", safeValue(job.getJobId()));
        record.put("ID", safeValue(job.getJobId()));
        record.put("SUBMITTER", safeValue(job.getSubmitterId()));
        record.put("INFO", safeValue(job.getDescription()));
        record.put("DESCRIPTION", safeValue(job.getDescription()));
        record.put("DURATION", Integer.toString(job.getDuration()));
        record.put("DEADLINE", job.getDeadline() == null ? "N/A" : job.getDeadline().toString());
        record.put("STATUS", job.getStatus() == null ? JobStatus.QUEUED.name() : job.getStatus().name());
        record.put("VEHICLE", safeValue(job.getVehicleId()));
        return record;
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
