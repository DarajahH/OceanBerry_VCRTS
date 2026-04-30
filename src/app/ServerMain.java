package app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.swing.*;
import models.job.Job;
import services.CloudDataService;

public class ServerMain {//Philip

    static ServerSocket serverSocket;
    static DataInputStream inputStream;
    static DataOutputStream outputStream;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void main(String[] args) {

        CloudDataService service = new CloudDataService(); //NO path needed anymore - DH
        ThemeWrapper.apply();

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

        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {
            // Read the submission entry and submitter username from client
            String entry = inputStream.readUTF();
            String submitter = inputStream.readUTF();
            System.out.println("Request received from client \"" + submitter + "\": \"" + entry + "\"");

            // Generate request ID first so we can ship it with the ACK
            String requestId = UUID.randomUUID().toString();

            // Send acknowledge + request ID back to client
            outputStream.writeUTF("ACK");
            outputStream.writeUTF(requestId);
            System.out.println("ACK sent to client (requestId=" + requestId + ").");

            service.clearAdminDecision();
            service.writePendingRequest(requestId, entry, submitter);
            System.out.println("Pending admin review for request " + requestId);

            boolean accepted = waitForAdminDecision(service, requestId);

            if (accepted) {
                try {
                    String role = normalizeRole(parseField(entry, "ROLE"));
                    if ("CLIENT".equals(role) || "TASK_OWNER".equals(role)) {
                        String jobId = firstNonBlank(parseField(entry, "TASK_ID"), parseField(entry, "ID"));
                        String description = firstNonBlank(
                            parseField(entry, "DESCRIPTION"),
                            parseField(entry, "TASK"),
                            parseField(entry, "INFO")
                        );
                        int duration = parseInteger(firstNonBlank(parseField(entry, "DURATION"), parseField(entry, "RESIDENCY")), 0);
                        String deadlineStr = parseField(entry, "DEADLINE");
                        String vehicleId = parseField(entry, "VEHICLE");

                        LocalDateTime arrivalTime = LocalDateTime.now();
                        LocalDateTime deadlineTime = parseDeadline(deadlineStr);

                        Job job = Job.createJob(jobId, submitter, description, duration, arrivalTime, deadlineTime, vehicleId);
                        service.appendJobAndLog(job, entry);
                    } else if ("VEHICLE_OWNER".equals(role)) {
                        String ownerId = firstNonBlank(parseField(entry, "ID"), submitter);
                        String vehicleId = firstNonBlank(parseField(entry, "VEHICLE"), parseField(entry, "INFO"));
                        int residencyHours = parseInteger(firstNonBlank(parseField(entry, "RESIDENCY"), parseField(entry, "DURATION")), 0);
                        String status = firstNonBlank(parseField(entry, "STATUS"), "IDLE");
                        String availability = firstNonBlank(parseField(entry, "AVAILABILITY"), "open");
                        service.appendVehicle(ownerId, vehicleId, residencyHours, status, availability);
                        service.appendLog(entry);
                    } else {
                        service.appendLog(entry);
                    }
                } catch (IllegalArgumentException ex) {
                    outputStream.writeUTF("REJECTED");
                    System.out.println("Accepted request could not be persisted: " + ex.getMessage());
                    service.clearPendingRequest(requestId);
                    service.clearAdminDecision(requestId);
                    return;
                }

                outputStream.writeUTF("ACCEPTED");
                System.out.println("Request ACCEPTED. Data saved to database.");
            } else {
                outputStream.writeUTF("REJECTED");
                System.out.println("Request REJECTED. Nothing saved.");
            }

            service.clearPendingRequest(requestId);
            service.clearAdminDecision(requestId);

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

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().replace(' ', '_').toUpperCase();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static LocalDateTime parseDeadline(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, dtf);
        } catch (Exception e) {
            return null;
        }
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
