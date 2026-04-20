package app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.*;
import services.CloudDataService;
import models.job.Job;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ServerMain {//Philip

    static ServerSocket serverSocket;
    static DataInputStream inputStream;
    static DataOutputStream outputStream;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void main(String[] args) {

        CloudDataService service = new CloudDataService(
            java.nio.file.Paths.get("vcrts_log.txt"),
            java.nio.file.Paths.get("users.txt")
        );

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        System.out.println("----------$$$ This is the VC Controller (Server) $$$--------");
        System.out.println("waiting for client to connect...");

        // Start server on a background thread so GUI don't freeze
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(9806);
                System.out.println("Server started on port 9806!");

                // Keep accepting client connections
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client connected!");

                    // Handle each client on its OWN thread (important)
                    new Thread(() -> handleClient(socket, service)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleClient(Socket socket, CloudDataService service) {

        try {
            // Read message (from client)
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Read the submission entry and submitter username from client
            String entry = inputStream.readUTF();
            String submitter = inputStream.readUTF();
            System.out.println("Request received from client \"" + submitter + "\": \"" + entry + "\"");

            // Step 1: Send acknowledge back to client
            outputStream.writeUTF("ACK");
            System.out.println("ACK sent to client.");

            String requestId = UUID.randomUUID().toString();
            service.clearAdminDecision();
            service.writePendingRequest(requestId, entry, submitter);
            System.out.println("Pending admin review for request " + requestId);

            boolean accepted = waitForAdminDecision(service, requestId);

            if (accepted) {
                //  decide if we save a job ! ! ! ! ! ! ! ! ! !!! !! ! 
                String role = parseField(entry, "ROLE");
                if ("CLIENT".equals(role)) {
                    String id = parseField(entry, "ID");
                    String info = parseField(entry, "INFO");
                    int duration = Integer.parseInt(parseField(entry, "DURATION"));
                    String deadlineStr = parseField(entry, "DEADLINE");

                    LocalDateTime arrivalTime = LocalDateTime.now();
                    LocalDateTime deadlineTime = (deadlineStr.isBlank() || "N/A".equals(deadlineStr)) ? null
                        : LocalDateTime.parse(deadlineStr, dtf);

                    Job job = Job.createJob(id, info, duration, arrivalTime, deadlineTime);
                    service.appendJob(job);
                }

                service.appendLog(entry);
                outputStream.writeUTF("ACCEPTED");
                System.out.println("Request ACCEPTED. Data saved to file.");
            } else {
                outputStream.writeUTF("REJECTED");
                System.out.println("Request REJECTED. Nothing saved.");
            }

            service.clearPendingRequest();
            service.clearAdminDecision();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper to parse a field value from the entry string format:
    // [timestamp] ROLE:CLIENT | ID:123 | INFO:test | DURATION:2 | DEADLINE:N/A
    private static String parseField(String entry, String fieldName) {
        String[] parts = entry.split("\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            // Remove leading timestamp bracket if present
            if (trimmed.startsWith("[")) {
                int closeBracket = trimmed.indexOf(']');
                if (closeBracket >= 0) {
                    trimmed = trimmed.substring(closeBracket + 1).trim();
                }
            }
            if (trimmed.startsWith(fieldName + ":")) {
                return trimmed.substring(fieldName.length() + 1).trim();
            }
        }
        return "";
    }

    private static boolean waitForAdminDecision(CloudDataService service, String requestId)
        throws IOException, InterruptedException {
        while (true) {
            String decision = service.readAdminDecision(requestId);
            if ("ACCEPTED".equalsIgnoreCase(decision)) {
                return true;
            }
            if ("REJECTED".equalsIgnoreCase(decision)) {
                return false;
            }
            Thread.sleep(250);
        }
    }
}
