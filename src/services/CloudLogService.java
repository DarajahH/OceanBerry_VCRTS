package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class CloudLogService {
    private final Path logPath;

    public CloudLogService(Path logPath) {
        this.logPath = logPath;
    }

    public void append(String entry) throws IOException {
        Files.writeString(
                logPath,
                entry + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    public List<String> readAll() throws IOException {
        if (!Files.exists(logPath)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(logPath, StandardCharsets.UTF_8);
    }
}
