package views;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import services.CloudDataService;

public class ConsolePanel {
    private final JFrame frame;
    private final JTextField idField, infoField, durField, deadlineField;
    private final JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private final JComboBox<String> roleBox;
    private final JTextArea monitorArea;
    private final CloudDataService service;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public ConsolePanel() {
        this.service = new CloudDataService(
            Paths.get("vcrts_log.txt"), 
            Paths.get("users.txt")
        );
        frame = new JFrame("VCRTS - Vehicular Cloud Console");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(20, 20, 25));

       // Role Selector (Requirement A)
        addLabel("Select Role:", 50, 30);
        roleBox = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleBox.setBounds(200, 30, 200, 30);
        roleBox.addActionListener(e -> adjustFields());
        frame.add(roleBox);

        // Input Fields (Requirement B)
        idLabel = addLabel("Owner ID:", 50, 100);
        idField = new JTextField(); idField.setBounds(200, 100, 200, 30);
        frame.add(idField);

        infoLabel = addLabel("Vehicle Info:", 50, 150);
        infoField = new JTextField(); infoField.setBounds(200, 150, 200, 30);
        frame.add(infoField);

        durLabel = addLabel("Residency (Hrs):", 50, 200);
        durField = new JTextField(); durField.setBounds(200, 200, 200, 30);
        frame.add(durField);

        deadlineLabel = addLabel("Job Deadline:", 50, 250);
        deadlineField = new JTextField(); deadlineField.setBounds(200, 250, 200, 30);
        deadlineLabel.setVisible(false); deadlineField.setVisible(false);
        frame.add(deadlineField);

        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.setBounds(50, 320, 350, 40);
        submitBtn.addActionListener(e -> saveEntry());
        frame.add(submitBtn);

        JButton calcBtn = new JButton("Calculate Completion Time");
        calcBtn.setBounds(50, 380, 350, 40);
        calcBtn.addActionListener(e -> calculateCompletionTimes());
        frame.add(calcBtn);



        // Monitor (Requirement C)
        monitorArea = new JTextArea();
        monitorArea.setEditable(false);
        monitorArea.setBackground(Color.BLACK);
        monitorArea.setForeground(Color.GREEN);
        JScrollPane scroll = new JScrollPane(monitorArea);
        scroll.setBounds(450, 30, 500, 580);
        frame.add(scroll);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refreshMonitor();
    }

    private Object calculateCompletionTimes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculateCompletionTimes'");
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
        String ts = dtf.format(LocalDateTime.now());
        String role = (String) roleBox.getSelectedItem();

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
    
        String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | TIME:%s | DEADLINE:%s", 
                                     ts, role, idField.getText(), infoField.getText(), 
                                     durField.getText(), deadlineField.isVisible() ? deadlineField.getText() : "N/A");
        try {
            service.appendLog(entry);
            refreshMonitor();
            clear();
        } catch (IOException e) { JOptionPane.showMessageDialog(frame, "File Error"); }
    }

    private void refreshMonitor() {
        try {
            monitorArea.setText("");
            for (String line : service.readAllLogs()) monitorArea.append(line + "\n");
        } catch (IOException ignored) {}
    }

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }

    private JLabel addLabel(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setBounds(x, y, 150, 30);
        frame.add(l);
        return l;
    }
}
