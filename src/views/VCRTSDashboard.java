package views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import services.CloudDataService;
import services.RequestClientService;
import services.VCController;
import services.VCController.JobCompletionRecord;

public class VCRTSDashboard {

    private final JFrame frame;
    private final CloudDataService service;
    private final VCController controller;
    private final RequestClientService requestClientService;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private JComboBox<String> roleBox;
    private JTextField idField;
    private JTextField infoField;
    private JTextField durField;
    private JTextField deadlineField;
    private JLabel idLabel;
    private JLabel infoLabel;
    private JLabel durLabel;
    private JLabel deadlineLabel;
    private JTextArea monitorArea;
    private JTextArea adminJobArea;
    private JTextArea adminNotesArea;

    public VCRTSDashboard(CloudDataService service) {
        this.service = service;
        this.controller = new VCController(service);
        this.requestClientService = new RequestClientService();

        frame = new JFrame("VCRTS - Cloud Control Center");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(20, 20, 25));

        frame.add(createHeader(), BorderLayout.NORTH);
        showScreen(createHomePanel(service));
        refreshMonitor(null);
        refreshAdminJobs();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public JPanel createHomePanel(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel welcomeLabel = new JLabel("Welcome to VCRTS");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridy = 0;
        panel.add(welcomeLabel, gbc);

        JLabel subLabel = new JLabel("Beta Branch Testing Dashboard");
        subLabel.setForeground(Color.GRAY);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 18, 0);
        panel.add(subLabel, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        JButton btnOpenForm = new JButton("Open Submission Form");
        btnOpenForm.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnOpenForm.addActionListener(e -> showScreen(createSubmissionPanel()));
        gbc.gridy = 2;
        panel.add(btnOpenForm, gbc);

        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
        gbc.gridy = 3;
        panel.add(btnCalcTimes, gbc);

        JButton taskOwnerBtn = new JButton("Task Owner Portal");
        taskOwnerBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        taskOwnerBtn.addActionListener(e -> showScreen(createTaskOwnerScreen(service)));
        gbc.gridy = 4;
        panel.add(taskOwnerBtn, gbc);

        JButton vehicleOwnerBtn = new JButton("Vehicle Owner Portal");
        vehicleOwnerBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        vehicleOwnerBtn.addActionListener(e -> showScreen(createVehicleOwnerScreen(service)));
        gbc.gridy = 5;
        panel.add(vehicleOwnerBtn, gbc);

        JButton btnAdminScreen = new JButton("Open Admin Review");
        btnAdminScreen.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnAdminScreen.addActionListener(e -> showScreen(createAdminScreen(service)));
        gbc.gridy = 6;
        panel.add(btnAdminScreen, gbc);

        JTextArea introMessage = new JTextArea(
            "This branch is meant for testing the clearer project path.\n\n"
            + "Flow to test:\n"
            + "1. Start ServerMain.\n"
            + "2. Submit a request from one of the user screens.\n"
            + "3. Accept or reject it from the VC Controller popup.\n"
            + "4. Review approved client jobs and FIFO timing results from Admin Review.\n\n"
            + "The goal here is clarity, not final presentation polish."
        );
        introMessage.setEditable(false);
        introMessage.setLineWrap(true);
        introMessage.setWrapStyleWord(true);
        introMessage.setOpaque(false);
        introMessage.setForeground(Color.LIGHT_GRAY);
        introMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        introMessage.setBorder(null);

        gbc.gridy = 7;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(introMessage, gbc);

        return panel;
    }

    public JPanel createAdminScreen(CloudDataService service) {
        JPanel adminPanel = new JPanel(new BorderLayout(12, 12));
        adminPanel.setBackground(new Color(30, 30, 35));
        adminPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Admin Review Screen");
        titleLabel.setForeground(Color.CYAN);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        adminPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
        actionPanel.add(btnCalcTimes, gbc);

        gbc.gridy = 1;
        JButton refreshJobsBtn = new JButton("Refresh Saved Jobs");
        refreshJobsBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        refreshJobsBtn.addActionListener(e -> refreshAdminJobs());
        actionPanel.add(refreshJobsBtn, gbc);

        gbc.gridy = 2;
        JButton backBtn = new JButton("Back to Home");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        backBtn.addActionListener(e -> showScreen(createHomePanel(service)));
        actionPanel.add(backBtn, gbc);

        adminJobArea = createReadOnlyArea();
        JScrollPane jobScroll = new JScrollPane(adminJobArea);
        jobScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "Saved Accepted Requests",
            0, 0, null, Color.CYAN
        ));

        adminNotesArea = createReadOnlyArea();
        adminNotesArea.setText(
            "How this beta branch works:\n"
            + "- Requests are submitted from the client, task owner, or vehicle owner screens.\n"
            + "- ServerMain opens the controller approval popup.\n"
            + "- Accepted CLIENT requests are stored in jobs.txt.\n"
            + "- Accepted TASK_OWNER requests are stored in task_owner_requests.txt.\n"
            + "- Accepted VEHICLE_OWNER requests are stored in vehicle_owner_requests.txt.\n"
            + "- Calculate Completion Times reads the saved client jobs and shows FIFO results.\n"
            + "- This screen is here to make the testing path easier to follow."
        );
        JScrollPane notesScroll = new JScrollPane(adminNotesArea);
        notesScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "Admin Notes",
            0, 0, null, Color.CYAN
        ));

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(jobScroll);
        centerPanel.add(notesScroll);

        adminPanel.add(actionPanel, BorderLayout.WEST);
        adminPanel.add(centerPanel, BorderLayout.CENTER);

        refreshAdminJobs();
        return adminPanel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 15, 20));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("VCRTS CLOUD DASHBOARD");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            new LoginScreen(service);
        });
        header.add(logoutBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel createSubmissionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createWhiteLabel("Select Role:"), gbc);

        roleBox = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleBox.addActionListener(e -> adjustFields());
        gbc.gridx = 1;
        panel.add(roleBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        idLabel = createWhiteLabel("Owner ID:");
        panel.add(idLabel, gbc);
        idField = new JTextField();
        idField.setToolTipText("Enter the manual numeric ID for this request.");
        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        infoLabel = createWhiteLabel("Vehicle Info:");
        panel.add(infoLabel, gbc);
        infoField = new JTextField();
        infoField.setToolTipText("Describe the job or vehicle details for the controller.");
        gbc.gridx = 1;
        panel.add(infoField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        durLabel = createWhiteLabel("Residency (Hrs):");
        panel.add(durLabel, gbc);
        durField = new JTextField();
        durField.setToolTipText("Enter a whole number of hours, such as 2.");
        gbc.gridx = 1;
        panel.add(durField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        deadlineLabel = createWhiteLabel("Job Deadline (yyyy/MM/dd HH:mm:ss):");
        panel.add(deadlineLabel, gbc);
        deadlineField = new JTextField();
        deadlineField.setToolTipText("Example: 2026/04/07 21:30:00");
        gbc.gridx = 1;
        panel.add(deadlineField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JTextArea helperText = new JTextArea(
            "Request flow:\n"
            + "- CLIENT entries create saved jobs after server approval.\n"
            + "- OWNER entries send vehicle-related requests to the controller.\n"
            + "- TASK_OWNER and VEHICLE_OWNER requests are also saved after approval.\n"
            + "- ADMIN role does not submit records from this form."
        );
        helperText.setEditable(false);
        helperText.setLineWrap(true);
        helperText.setWrapStyleWord(true);
        helperText.setOpaque(false);
        helperText.setForeground(Color.LIGHT_GRAY);
        helperText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(helperText, gbc);

        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.addActionListener(e -> saveEntry());
        gbc.gridy = 6;
        gbc.insets = new Insets(30, 10, 10, 10);
        panel.add(submitBtn, gbc);

        JButton homeBtn = new JButton("Back to Home");
        homeBtn.addActionListener(e -> showScreen(createHomePanel(service)));
        gbc.gridy = 7;
        panel.add(homeBtn, gbc);

        gbc.gridy = 8;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        adjustFields();
        return panel;
    }

    private void showScreen(JPanel contentPanel) {
        frame.getContentPane().removeAll();
        frame.add(createHeader(), BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, createMonitorPanel());
        splitPane.setDividerLocation(430);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        frame.add(splitPane, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createTaskOwnerScreen(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Task Owner Portal");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(createWhiteLabel("Task Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Task Description:"), gbc);
        JTextField taskField = new JTextField();
        gbc.gridx = 1;
        panel.add(taskField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Target Vehicle ID:"), gbc);
        JTextField vehicleField = new JTextField();
        gbc.gridx = 1;
        panel.add(vehicleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Priority Level:"), gbc);
        JTextField priorityField = new JTextField();
        gbc.gridx = 1;
        panel.add(priorityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton submitBtn = new JButton("Submit to VC");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || taskField.getText().isBlank()
                || vehicleField.getText().isBlank() || priorityField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Task Owner fields.");
                return;
            }
            String entry = String.format(
                "[%s] ROLE:TASK_OWNER | ID:%s | TASK:%s | VEHICLE:%s | PRIORITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                taskField.getText().trim(),
                vehicleField.getText().trim(),
                priorityField.getText().trim()
            );
            sendRequestToServer(entry);
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    private JPanel createVehicleOwnerScreen(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Vehicle Owner Portal");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(createWhiteLabel("Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Vehicle ID:"), gbc);
        JTextField vehicleIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(vehicleIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Status Update:"), gbc);
        JTextField statusField = new JTextField();
        gbc.gridx = 1;
        panel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Availability:"), gbc);
        JTextField availabilityField = new JTextField();
        gbc.gridx = 1;
        panel.add(availabilityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton submitBtn = new JButton("Submit to VC");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || vehicleIdField.getText().isBlank()
                || statusField.getText().isBlank() || availabilityField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Vehicle Owner fields.");
                return;
            }
            String entry = String.format(
                "[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                vehicleIdField.getText().trim(),
                statusField.getText().trim(),
                availabilityField.getText().trim()
            );
            sendRequestToServer(entry);
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    private JButton createBackToHomeButton(CloudDataService service) {
        JButton backBtn = new JButton("Back to Home");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        backBtn.addActionListener(e -> showScreen(createHomePanel(service)));
        return backBtn;
    }

    public void keypressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveEntry();
        }
    }

    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 25));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        monitorArea = createReadOnlyArea();

        JScrollPane scrollPane = new JScrollPane(monitorArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "Live System Terminal",
            0, 0, null, Color.CYAN
        ));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JTextArea createReadOnlyArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.BLACK);
        area.setForeground(Color.GREEN);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return area;
    }

    private JLabel createWhiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    private void adjustFields() {
        if (roleBox == null || idLabel == null || infoLabel == null || durLabel == null || deadlineLabel == null) {
            return;
        }

        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
        frame.revalidate();
    }

    private void saveEntry() {
        String role = (String) roleBox.getSelectedItem();
        if ("ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(frame, "Admin does not submit records from this form. Use the Admin Review screen.");
            return;
        }

        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durField.getText().trim();
        String deadline = deadlineField.isVisible() ? deadlineField.getText().trim() : "N/A";

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter all required fields.");
            return;
        }

        if (!id.matches("\\d+")) {
            JOptionPane.showMessageDialog(frame, "ID must be numeric (digits only).");
            return;
        }

        final int duration;
        try {
            duration = Integer.parseInt(dur);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Duration must be a number.");
            return;
        }

        if ("CLIENT".equals(role) && deadline.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Client jobs must include a deadline.");
            return;
        }

        try {
            LocalDateTime arrivalTime = LocalDateTime.now();
            LocalDateTime deadlineTime = deadlineField.isVisible()
                ? LocalDateTime.parse(deadline, dtf)
                : null;
            String formattedDeadline = deadlineTime == null ? "N/A" : dtf.format(deadlineTime);
            String entry = String.format(
                "[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%d | DEADLINE:%s",
                dtf.format(arrivalTime),
                role,
                id,
                info,
                duration,
                formattedDeadline
            );

            refreshMonitor("Connecting to VC Controller server...");
            RequestClientService.RequestResult result = requestClientService.sendRequest(entry);
            refreshMonitor("Server response: " + result.getAck() + " - Pending approval...");

            if ("ACCEPTED".equals(result.getDecision())) {
                refreshMonitor("FINAL STATUS: ACCEPTED\n\nSaved entry:\n" + entry);
                clear();
                refreshAdminJobs();
            } else {
                refreshMonitor("FINAL STATUS: REJECTED\n\nRejected entry:\n" + entry);
                JOptionPane.showMessageDialog(frame, "Request rejected by VC Controller. Nothing was saved.");
            }
        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Deadline must use format yyyy/MM/dd HH:mm:ss.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection error: " + e.getMessage());
        }
    }

    private void calculateCompletionTimes() {
        try {
            List<JobCompletionRecord> records = controller.calculateCompletionTimes();
            if (records.isEmpty()) {
                refreshMonitor("No client jobs found.");
                refreshAdminJobs();
                return;
            }

            StringBuilder results = new StringBuilder();
            results.append("FIFO Completion Times\n---------------------\n");
            for (JobCompletionRecord record : records) {
                results.append(record.toDisplayString()).append("\n");
            }
            refreshMonitor(results.toString().trim());
            refreshAdminJobs();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error calculating completion times.");
        }
    }

    private void refreshMonitor(String resultSection) {
        if (monitorArea == null) {
            return;
        }

        try {
            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                display.append(line).append("\n");
            }

            if (resultSection != null && !resultSection.isBlank()) {
                display.append("\n\n").append(resultSection);
            }

            monitorArea.setText(display.toString());
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength());
        } catch (IOException ignored) {
            monitorArea.setText(resultSection == null ? "" : resultSection);
        }
    }

    private void clear() {
        idField.setText("");
        infoField.setText("");
        durField.setText("");
        deadlineField.setText("");
    }

    private void refreshAdminJobs() {
        if (adminJobArea == null) {
            return;
        }

        try {
            List<String> records = service.readAllSavedRequestSummaries();
            if (records.isEmpty()) {
                adminJobArea.setText("No accepted requests have been saved yet.");
                return;
            }

            adminJobArea.setText(String.join("\n", records));
            adminJobArea.setCaretPosition(0);
        } catch (IOException e) {
            adminJobArea.setText("Unable to read saved requests: " + e.getMessage());
        }
    }

    private void sendRequestToServer(String entry) {
        try {
            refreshMonitor("Connecting to VC Controller server...");
            RequestClientService.RequestResult result = requestClientService.sendRequest(entry);
            refreshMonitor("Server ACK: " + result.getAck() + " - Pending approval...");

            if ("ACCEPTED".equals(result.getDecision())) {
                refreshMonitor("FINAL STATUS: ACCEPTED\n\n" + entry);
                JOptionPane.showMessageDialog(frame, "Request accepted.");
                refreshAdminJobs();
            } else {
                refreshMonitor("FINAL STATUS: REJECTED\n\n" + entry);
                JOptionPane.showMessageDialog(frame, "Request rejected.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection error: " + e.getMessage());
        }
    }
}
