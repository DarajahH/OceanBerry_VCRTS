package views;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import services.CloudDataService;
import services.VCController;
import services.VCController.JobCompletionRecord;

public class ConsolePanel {
    private final JFrame frame;
    private final JTextField idField;
    private final JTextField infoField;
    private final JTextField durField;
    private final JTextField deadlineField;
    private final JLabel idLabel;
    private final JLabel infoLabel;
    private final JLabel durLabel;
    private final JLabel deadlineLabel;
    private final JComboBox<String> roleBox;
    private final JTextArea monitorArea;
    private final CloudDataService service;
    private final VCController controller;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public ConsolePanel() {
        this.service = new CloudDataService(
            Paths.get("vcrts_log.txt"),
            Paths.get("users.txt")
        );
        this.controller = new VCController(service);

        frame = new JFrame("VCRTS - Vehicular Cloud Console");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(20, 20, 25));

        addLabel("Select Role:", 50, 30);
        roleBox = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleBox.setBounds(200, 30, 200, 30);
        roleBox.addActionListener(e -> adjustFields());
        frame.add(roleBox);

        idLabel = addLabel("Owner ID:", 50, 100);
        idField = new JTextField();
        idField.setBounds(200, 100, 200, 30);
        frame.add(idField);

        infoLabel = addLabel("Vehicle Info:", 50, 150);
        infoField = new JTextField();
        infoField.setBounds(200, 150, 200, 30);
        frame.add(infoField);

        durLabel = addLabel("Residency (Hrs):", 50, 200);
        durField = new JTextField();
        durField.setBounds(200, 200, 200, 30);
        frame.add(durField);

        deadlineLabel = addLabel("Job Deadline:", 50, 250);
        deadlineField = new JTextField();
        deadlineField.setBounds(200, 250, 200, 30);
        frame.add(deadlineField);

        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.setBounds(50, 320, 350, 40);
        submitBtn.addActionListener(e -> saveEntry());
        frame.add(submitBtn);

        JButton calcBtn = new JButton("Calculate Completion Time");
        calcBtn.setBounds(50, 380, 350, 40);
        calcBtn.addActionListener(e -> calculateCompletionTimes());
        frame.add(calcBtn);

        monitorArea = new JTextArea();
        monitorArea.setEditable(false);
        monitorArea.setBackground(Color.BLACK);
        monitorArea.setForeground(Color.GREEN);
        JScrollPane scroll = new JScrollPane(monitorArea);
        scroll.setBounds(450, 30, 500, 580);
        frame.add(scroll);

        adjustFields();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refreshMonitor(null);
    }

    private void calculateCompletionTimes() {
        try {
            List<JobCompletionRecord> completionRecords = controller.calculateCompletionTimes();
            if (completionRecords.isEmpty()) {
                refreshMonitor("No client jobs found.");
                return;
            }

            StringBuilder results = new StringBuilder();
            results.append("FIFO Completion Times").append(System.lineSeparator());
            results.append("---------------------").append(System.lineSeparator());
            for (JobCompletionRecord record : completionRecords) {
                results.append(record.toDisplayString()).append(System.lineSeparator());
            }

            refreshMonitor(results.toString().trim());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "File Error while calculating completion times.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid duration found in the log file.");
        }
    }

    private void adjustFields() {
        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
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

        if (!id.matches("\\d+")) {
            JOptionPane.showMessageDialog(frame, "ID must be numeric (digits only).");
            return;
        }

        if (!dur.matches("\\d+")) {
            JOptionPane.showMessageDialog(frame, "Duration must be numeric (digits only).");
            return;
        }

        if ("CLIENT".equals(role) && deadline.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Client jobs must include a deadline.");
            return;
        }

        String entry = String.format(
            "[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%s | DEADLINE:%s",
            dtf.format(LocalDateTime.now()),
            role,
            id,
            info,
            dur,
            deadline
        );

        try {
            service.appendLog(entry);
            refreshMonitor("Saved entry:\n" + entry);
            clear();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "File Error");
        }
    }

    private void refreshMonitor(String resultSection) {
        try {
            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                display.append(line).append(System.lineSeparator());
            }

            if (display.length() == 0) {
                display.append("No records saved yet.");
            }

            if (resultSection != null && !resultSection.isBlank()) {
                display.append(System.lineSeparator()).append(System.lineSeparator()).append(resultSection);
            }

            monitorArea.setText(display.toString());
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength());
        } catch (IOException ignored) {
            monitorArea.setText("Unable to load records.");
        }
    }

    private void clear() {
        idField.setText("");
        infoField.setText("");
        durField.setText("");
        deadlineField.setText("");
    }

    private JLabel addLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setBounds(x, y, 150, 30);
        frame.add(label);
        return label;
    }
}
