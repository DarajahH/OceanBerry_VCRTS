package views;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import services.CloudDataService;
import services.VCController;
import services.VCController.JobCompletionRecord;

public class VCRTSDashboard {

    private final JFrame frame;
    private final CloudDataService service;
    private final VCController controller;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // Form Components
    private JComboBox<String> roleBox;
    private JTextField idField, infoField, durField, deadlineField;
    private JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private JTextArea monitorArea;

    public VCRTSDashboard(CloudDataService service) {
        this.service = service;
        this.controller = new VCController(service);

        // 1. Setup Main Frame
        frame = new JFrame("VCRTS - Cloud Control Center");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(20, 20, 25));

        // 2. Add Header
        frame.add(createHeader(), BorderLayout.NORTH);

        // 3. Create Panels
        JPanel leftControlPanel = createControlPanel();
        JPanel rightMonitorPanel = createMonitorPanel();

        // 4. Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftControlPanel, rightMonitorPanel);
        splitPane.setDividerLocation(400); // Give the form a bit more room
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        
        frame.add(splitPane, BorderLayout.CENTER);

        // Initialize state
        adjustFields();
        refreshMonitor(null);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 15, 20));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("VCRTS CLOUD DASHBOARD");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        // Role Selection
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createWhiteLabel("Select Role:"), gbc);
        
        roleBox = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleBox.addActionListener(e -> adjustFields());
        gbc.gridx = 1; 
        panel.add(roleBox, gbc);

        // Input Fields
        gbc.gridx = 0; gbc.gridy = 1;
        idLabel = createWhiteLabel("Owner ID:");
        panel.add(idLabel, gbc);
        idField = new JTextField();
        gbc.gridx = 1; panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoLabel = createWhiteLabel("Vehicle Info:");
        panel.add(infoLabel, gbc);
        infoField = new JTextField();
        gbc.gridx = 1; panel.add(infoField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        durLabel = createWhiteLabel("Residency (Hrs):");
        panel.add(durLabel, gbc);
        durField = new JTextField();
        gbc.gridx = 1; panel.add(durField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        deadlineLabel = createWhiteLabel("Job Deadline:");
        panel.add(deadlineLabel, gbc);
        deadlineField = new JTextField();
        gbc.gridx = 1; panel.add(deadlineField, gbc);

        // Buttons
        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.addActionListener(e -> saveEntry());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 10, 10); // Extra top padding
        panel.add(submitBtn, gbc);

        JButton calcBtn = new JButton("Calculate Completion Time");
        calcBtn.addActionListener(e -> calculateCompletionTimes());
        gbc.gridy = 6; gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(calcBtn, gbc);

        // Spacer to push everything to the top
        gbc.gridy = 7; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 25));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        monitorArea = new JTextArea();
        monitorArea.setEditable(false);
        monitorArea.setBackground(Color.BLACK);
        monitorArea.setForeground(Color.GREEN);
        monitorArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(monitorArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY), 
            "Live System Terminal", 
            0, 0, null, Color.CYAN));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // --- LOGIC METHODS (Migrated from ConsolePanel) ---

    private JLabel createWhiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    private void adjustFields() {
        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
        frame.revalidate(); // Refreshes the UI so hidden fields don't leave weird spaces
    }

    private void saveEntry() {
        String role = (String) roleBox.getSelectedItem();
        if ("ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(frame, "Admin does not submit records. Use Calculate Completion Time.");
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

        if ("CLIENT".equals(role) && deadline.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Client jobs must include a deadline.");
            return;
        }

        String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%s | DEADLINE:%s",
            dtf.format(LocalDateTime.now()), role, id, info, dur, deadline);

        try {
            service.appendLog(entry);
            refreshMonitor("Saved entry:\n" + entry);
            clear();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "File Error");
        }
    }

    private void calculateCompletionTimes() {
        try {
            List<JobCompletionRecord> records = controller.calculateCompletionTimes();
            if (records.isEmpty()) {
                refreshMonitor("No client jobs found.");
                return;
            }

            StringBuilder results = new StringBuilder();
            results.append("FIFO Completion Times\n---------------------\n");
            for (JobCompletionRecord record : records) {
                results.append(record.toDisplayString()).append("\n");
            }
            refreshMonitor(results.toString().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error calculating completion times.");
        }
    }

    private void refreshMonitor(String resultSection) {
        try {
            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                display.append(line).append("\n");
            }

            if (resultSection != null && !resultSection.isBlank()) {
                display.append("\n\n").append(resultSection);
            }

            monitorArea.setText(display.toString());
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength()); // Auto-scroll to bottom
        } catch (IOException ignored) {}
    }

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }
}