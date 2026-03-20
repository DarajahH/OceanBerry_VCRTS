package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

//New Inserted Code

public class CloudDataService {
    private final Path logPath;
    private final Path userPath;

    // Inject both paths via constructor for better flexibility
    public CloudDataService(Path logPath, Path userPath) {
        this.logPath = logPath;
        this.userPath = userPath;
    }

    // Requirement B: Storing info on a file
    public void appendLog(String entry) throws IOException {
        Files.writeString(logPath, entry + System.lineSeparator(), 
            StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // Requirement C: Receive info of multiple users
    public List<String> readAllLogs() throws IOException {
        if (!Files.exists(logPath)) return Collections.emptyList();
        return Files.readAllLines(logPath, StandardCharsets.UTF_8);
    }

    public void registerUser(String username, String password) throws IOException {
        String entry = username + ":" + password + System.lineSeparator();
        Files.writeString(userPath, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public boolean validateUser(String username, String password) {
        try {
            if (!Files.exists(userPath)) return false;
            List<String> users = Files.readAllLines(userPath);
            for (String line : users) {
                if (line.equals(username + ":" + password)) return true;
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
        return false;
    }

    // Returns only CLIENT job entries from the log file
    // Used for FIFO completion time calculation 
    public java.util.List<String> readClientLogs () throws IOException {
        if (!Files.exists(logPath)) return java.util.Collections.emptyList();

        java.util.List<String> allLogs = Files.readAllLines(logPath, StandardCharsets.UTF_8);
        java.util.List<String> clientLogs = new java.util.ArrayList<>();

        for(String line : allLogs) {
            if (line.contains("ROLE:CLIENT")){
                clientLogs.add(line);
            }
      }
        return clientLogs;

    }
}