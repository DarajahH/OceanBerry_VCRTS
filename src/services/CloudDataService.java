package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private final Path logPath;
    private final Path userPath;
    private final Path jobPath;
    private final Path pendingRequestPath;
    private final Path adminDecisionPath;

    public CloudDataService(Path logPath, Path userPath) {
        this(logPath, userPath, logPath.resolveSibling("jobs.txt"));
    }

    public CloudDataService(Path logPath, Path userPath, Path jobPath) {
        this.logPath = logPath;
        this.userPath = userPath;
        this.jobPath = jobPath;
        this.pendingRequestPath = logPath.resolveSibling("pending_request.txt");
        this.adminDecisionPath = logPath.resolveSibling("admin_decision.txt");
    }

    public void appendLog(String entry) throws IOException {
        Files.writeString(
            logPath,
            entry + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public List<String> readAllLogs() throws IOException {
        if (!Files.exists(logPath)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(logPath, StandardCharsets.UTF_8);
    }

    public void registerUser(String username, String password) throws IOException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        if (userExists(cleanUsername)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        String entry = cleanUsername + ":" + cleanPassword + System.lineSeparator();
        Files.writeString(
            userPath,
            entry,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public boolean validateUser(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = password == null ? "" : password.trim();

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            return false;
        }

        try {
            if (!Files.exists(userPath)) {
                return false;
            }
            List<String> users = Files.readAllLines(userPath, StandardCharsets.UTF_8);
            for (String line : users) {
                if (line.equals(cleanUsername + ":" + cleanPassword)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean userExists(String username) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty()) {
            return false;
        }
        try {
            if (!Files.exists(userPath)) {
                return false;
            }
            List<String> users = Files.readAllLines(userPath, StandardCharsets.UTF_8);
            for (String line : users) {
                if (line.startsWith(cleanUsername + ":")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void appendJob(Job job) throws IOException {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null.");
        }
        String entry = serializeJob(job) + System.lineSeparator();
        Files.writeString(
            jobPath,
            entry,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public List<Job> readJobs() throws IOException {
        if (!Files.exists(jobPath)) {
            return Collections.emptyList();
        }
        List<String> lines = Files.readAllLines(jobPath, StandardCharsets.UTF_8);
        List<Job> jobs = new ArrayList<>();
        for (String line : lines) {
            Job job = parseJobLine(line);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    public List<String> readClientLogs() throws IOException {
        List<String> clientLogs = new ArrayList<>();
        for (String line : readAllLogs()) {
            if ("CLIENT".equals(parseLogEntry(line).get("ROLE"))) {
                clientLogs.add(line);
            }
        }
        return clientLogs;
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

    public synchronized void writePendingRequest(String requestId, String entry) throws IOException {
        String payload = "REQUEST_ID:" + encodeField(requestId) + System.lineSeparator()
            + "ENTRY:" + encodeField(entry) + System.lineSeparator();
        Files.writeString(
            pendingRequestPath,
            payload,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public synchronized Map<String, String> readPendingRequest() throws IOException {
        if (!Files.exists(pendingRequestPath)) {
            return Collections.emptyMap();
        }

        List<String> lines = Files.readAllLines(pendingRequestPath, StandardCharsets.UTF_8);
        Map<String, String> pending = new LinkedHashMap<>();
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = decodeField(line.substring(separatorIndex + 1).trim());
            pending.put(key, value);
        }
        return pending;
    }

    public synchronized void clearPendingRequest() throws IOException {
        Files.deleteIfExists(pendingRequestPath);
    }

    public synchronized void writeAdminDecision(String requestId, String decision) throws IOException {
        String payload = "REQUEST_ID:" + encodeField(requestId) + System.lineSeparator()
            + "DECISION:" + encodeField(decision) + System.lineSeparator();
        Files.writeString(
            adminDecisionPath,
            payload,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public synchronized String readAdminDecision(String requestId) throws IOException {
        if (!Files.exists(adminDecisionPath)) {
            return null;
        }

        Map<String, String> decisionEntry = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(adminDecisionPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = decodeField(line.substring(separatorIndex + 1).trim());
            decisionEntry.put(key, value);
        }

        if (!requestId.equals(decisionEntry.get("REQUEST_ID"))) {
            return null;
        }

        return decisionEntry.get("DECISION");
    }

    public synchronized void clearAdminDecision() throws IOException {
        Files.deleteIfExists(adminDecisionPath);
    }

    //Serializes a job object to a string - NAEEM
    private String serializeJob(Job job) {
        return String.join(
            JOB_FIELD_DELIMITER,
            encodeField(job.getJobId()),
            encodeField(job.getDescription()),
            Integer.toString(job.getDuration()),
            job.getArrivalTime() == null ? "" : job.getArrivalTime().toString(),
            job.getDeadline() == null ? "" : job.getDeadline().toString(),
            job.getStatus() == null ? "" : job.getStatus().name(),
            job.getCompletionTime() == null ? "" : Integer.toString(job.getCompletionTime())
        );
    }

    //Parses a job object from a string - NAEEM
    private Job parseJobLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] fields = line.split(JOB_FIELD_DELIMITER, -1);
        if (fields.length < 7) {
            return null;
        }

        try {
            String jobId = decodeField(fields[0]);
            String description = decodeField(fields[1]);
            int duration = Integer.parseInt(fields[2]);
            LocalDateTime arrivalTime = parseDateTime(fields[3]);
            LocalDateTime deadline = parseDateTime(fields[4]);
            JobStatus status = fields[5].isBlank() ? JobStatus.QUEUED : JobStatus.valueOf(fields[5]);
            Integer completionTime = fields[6].isBlank() ? null : Integer.parseInt(fields[6]);

            Job.registerExistingJobId(jobId);
            return new Job(jobId, description, duration, arrivalTime, deadline, status, completionTime);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    //Parses a date time string to a LocalDateTime object - NAEEM
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

    //Encodes a string to a format that can be stored in a file - NAEEM
    private String encodeField(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    //Decodes a string from a format that can be stored in a file - NAEEM
    private String decodeField(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char currentChar = value.charAt(i);
            if (escaping) {
                switch (currentChar) {
                    case 't':
                        result.append('\t');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case '\\':
                        result.append('\\');
                        break;
                    default:
                        result.append(currentChar);
                        break;
                }
                escaping = false;
            } else if (currentChar == '\\') {
                escaping = true;
            } else {
                result.append(currentChar);
            }
        }

        if (escaping) {
            result.append('\\');
        }

        return result.toString();
    }
}
