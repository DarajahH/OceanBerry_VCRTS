package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudDataService {
    private final Path logPath;
    private final Path userPath;

    public CloudDataService(Path logPath, Path userPath) {
        this.logPath = logPath;
        this.userPath = userPath;
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
        String entry = username + ":" + password + System.lineSeparator();
        Files.writeString(
            userPath,
            entry,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public boolean validateUser(String username, String password) {
        try {
            if (!Files.exists(userPath)) {
                return false;
            }
            List<String> users = Files.readAllLines(userPath, StandardCharsets.UTF_8);
            for (String line : users) {
                if (line.equals(username + ":" + password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Path getLogPath() {
        return logPath;
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
}
