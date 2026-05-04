package app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import models.job.Job;
import services.CloudDataService;

public class ServerMain {//Philip

    private static final int DEFAULT_PORT = 9806;
    private static final String PORT_ENV = "VCRTS_SERVER_PORT";

    static ServerSocket serverSocket;
    static DataInputStream inputStream;
    static DataOutputStream outputStream;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void main(String[] args) {

        CloudDataService service = new CloudDataService(); //NO path needed anymore - DH
        ThemeWrapper.apply();

        System.out.println("----------$$$ This is the VC Controller (Server) $$$--------");
        int port = resolveServerPort();
        System.out.println("waiting for client to connect...");

        // Start server on a background thread so GUI don't freeze
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started on port " + port + "!");

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

    private static int resolveServerPort() {
        String configuredPort = System.getenv(PORT_ENV);
        if (configuredPort == null || configuredPort.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(configuredPort.trim());
            if (port < 1 || port > 65535) {
                System.out.println(PORT_ENV + " must be between 1 and 65535. Using " + DEFAULT_PORT + ".");
                return DEFAULT_PORT;
            }
            return port;
        } catch (NumberFormatException e) {
            System.out.println(PORT_ENV + " must be a number. Using " + DEFAULT_PORT + ".");
            return DEFAULT_PORT;
        }
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
                        Integer residencyHours = parseOptionalInteger(firstNonBlank(parseField(entry, "RESIDENCY"), parseField(entry, "DURATION")));
                        String model = parseField(entry, "MODEL");
                        String vin = parseField(entry, "VIN");
                        String make = parseField(entry, "MAKE");
                        String year = parseField(entry, "YEAR");
                        String status = firstNonBlank(parseField(entry, "STATUS"), "IDLE");
                        String availability = firstNonBlank(parseField(entry, "AVAILABILITY"), "open");
                        service.appendVehicle(ownerId, vehicleId, model, vin, make, year, residencyHours, status, availability);
                        service.appendLog(entry);
                    } else {
                        service.appendLog(entry);
                    }
                } catch (IllegalArgumentException | IOException ex) {
                    outputStream.writeUTF("REJECTED");
                    System.out.println("Accepted request could not be persisted: " + ex.getMessage());
                    notifySubmitter(
                        service,
                        submitter,
                        "Your submission was ACCEPTED by admin, but request " + requestId
                            + " could not be saved."
                    );
                    service.clearPendingRequest(requestId);
                    service.clearAdminDecision(requestId);
                    return;
                }

                outputStream.writeUTF("ACCEPTED");
                notifySubmitter(
                    service,
                    submitter,
                    "Your submission was ACCEPTED. Request ID: " + requestId
                );
                System.out.println("Request ACCEPTED. Data saved to database.");
            } else {
                outputStream.writeUTF("REJECTED");
                notifySubmitter(
                    service,
                    submitter,
                    "Your submission was REJECTED by admin.Request ID: " + requestId
                );
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

    private static Integer parseOptionalInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
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

    private static void notifySubmitter(CloudDataService service, String submitter, String message) {
        if (submitter == null || submitter.isBlank()) {
            return;
        }
        try {
            service.addNotification(submitter, message);
        } catch (IOException e) {
            System.out.println("Unable to write client notification: " + e.getMessage());
        }
    }
}
