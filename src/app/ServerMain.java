package app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import models.job.Job;
import services.CloudDataService;

public class ServerMain {

    static ServerSocket serverSocket;

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
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            
            // Read the submission entry from client
            String entry = inputStream.readUTF();
            System.out.println("Request received from client: \"" + entry + "\"");

            // Step 1: Send acknowledge back to client
            outputStream.writeUTF("ACK");
            System.out.println("ACK sent to client.");

            // Step 2: VC Controller decision (Accept/Reject)
            String[] options = {"Accept", "Reject"};

            int decision = JOptionPane.showOptionDialog(
                null,
                "VC Controller Review:\n\n" + entry + "\n\nAccept or Reject this request?",
                "VC Controller Decision",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );

            boolean accepted = (decision == 0);

            if (accepted) {
                //  decide if we save a job ! ! ! ! ! ! ! ! ! !!! !! ! 
                String role = parseField(entry, "ROLE");
                if ("CLIENT".equals(role)) {
                    String id = parseField(entry, "ID");
                    String info = parseField(entry, "INFO");
                    int duration = Integer.parseInt(parseField(entry, "DURATION"));
                    String deadlineStr = parseField(entry, "DEADLINE");

                    LocalDateTime arrivalTime = LocalDateTime.now();
                    LocalDateTime deadlineTime = "N/A".equals(deadlineStr) ? null
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
}
