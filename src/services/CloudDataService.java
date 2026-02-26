package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class CloudDataService {
    private final Path logPath;
    private static final Path USER_FILE = Paths.get("users.txt");

    public CloudDataService(Path logPath) {
        this.logPath = logPath;
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

    public static void registerUser(String username, String password) throws IOException {
        String entry = username + ":" + password + System.lineSeparator();
        Files.writeString(USER_FILE, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static boolean validateUser(String username, String password) {
        try {
            if (!Files.exists(USER_FILE)) return false;
            List<String> users = Files.readAllLines(USER_FILE);
            for (String line : users) {
                if (line.equals(username + ":" + password)) return true;
            }
        } catch (IOException e) { e.printStackTrace(); }
        return false;
    }
}
