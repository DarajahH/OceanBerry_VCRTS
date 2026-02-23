import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

public class createPanel implements ActionListener {
    // Fields for data collection
    private JTextField idField, infoField, durationField, deadlineField;
    private JToggleButton roleToggle;
    private JFrame frame;

    public createPanel() {

        // Main Frame Setup [cite: 1, 22]
        frame = new JFrame("VCRTS - Cloud Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30));
        
        // Glass Panel Container
        JPanel glassPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 30, 30);
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.drawRoundRect(20, 20, getWidth() - 41, getHeight() - 41, 30, 30);
                g2d.dispose();
            }
        };

        glassPanel.setOpaque(false);
        glassPanel.setLayout(null);

        // Header with Live Clock
        JLabel title = new JLabel("VCRTS MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 24));
        title.setBounds(50, 40, 400, 40);
        glassPanel.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(650, 40, 200, 40);
        glassPanel.add(clock);

        // Input Fields
        int labelX = 100, fieldX = 300, startY = 120, spacing = 50;

        // Tongle for Role Selection (Owner vs Client) [cite: 15, 33]
        addStyledLabel(glassPanel, "User Role:", labelX, startY);

        roleToggle = new JToggleButton("OWNER");
        roleToggle.setBounds(fieldX, startY, 200, 35);
        roleToggle.setFocusPainted(false);
        roleToggle.setBackground(new Color(52, 199, 89)); // Green
        roleToggle.setForeground(Color.BLACK);
        roleToggle.setFont(new Font("Helvetica Neue", Font.BOLD, 14));
        glassPanel.add(roleToggle);

        // ID field (common for both roles)
        addStyledLabel(glassPanel, "User/Owner ID:", labelX, startY + spacing);
        idField = new JTextField();
        idField.setBounds(fieldX, startY + spacing, 200, 30);
        glassPanel.add(idField);

        // For Owner: Vehicle Info | For Client: Job Info
        addStyledLabel(glassPanel, "Vehicle Info / Job Info:", labelX, startY + (spacing * 2));
        infoField = new JTextField();
        infoField.setBounds(fieldX, startY + (spacing * 2), 200, 30);
        glassPanel.add(infoField);

        // Duration field (common for both roles)
        addStyledLabel(glassPanel, "Duration (Hours):", labelX, startY + (spacing * 3));
        durationField = new JTextField();
        durationField.setBounds(fieldX, startY + (spacing * 3), 200, 30);
        glassPanel.add(durationField);

        // deadline field (only for clients)
        addStyledLabel(glassPanel, "Deadline (if Client):", labelX, startY + (spacing * 4));
        deadlineField = new JTextField();
        deadlineField.setBounds(fieldX, startY + (spacing * 4), 200, 30);
        deadlineField.setEnabled(false); // Disabled by default
        glassPanel.add(deadlineField);

        // Toggle behavior for role selection
        roleToggle.addItemListener(e -> {
            if (roleToggle.isSelected()) {
                roleToggle.setText("CLIENT");
                roleToggle.setBackground(new Color(0, 122, 255)); // Apple Blue
                deadlineField.setEnabled(true);
            } else {
                roleToggle.setText("OWNER");
                roleToggle.setBackground(new Color(52, 199, 89));
                deadlineField.setEnabled(false);
                deadlineField.setText("");
            }
        });

        // Submit Button 
        JButton submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fieldX, startY + (spacing * 5), 200, 40);
        submitBtn.setBackground(new Color(0, 122, 255));
        submitBtn.setForeground(Color.BLACK);
        submitBtn.setFocusPainted(false);
        submitBtn.addActionListener(this);
        glassPanel.add(submitBtn);

        frame.add(glassPanel);
        frame.setVisible(true);
    }

    private void addStyledLabel(JPanel panel, String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        label.setBounds(x, y, 200, 30);
        panel.add(label);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String role = roleToggle.isSelected() ? "Client" : "Owner";

        if (idField.getText().trim().isEmpty() ||
            infoField.getText().trim().isEmpty() ||
            durationField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(frame, "Please fill all required fields.");
            return;
        }

        // Validate numeric duration
        try {
            Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Duration must be a number.");
            return;
        }

        if (role.equals("Client")) {
            if (deadlineField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Client must enter a deadline.");
                return;
            }
            try {
                Integer.parseInt(deadlineField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Deadline must be a number.");
                return;
            }
        }

        // Data Formatting for File [cite: 7, 64]
        String deadlineValue = role.equals("Owner") ? "N/A" : deadlineField.getText().trim();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String record = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s\n",
                timestamp, role,
                idField.getText().trim(),
                infoField.getText().trim(),
                durationField.getText().trim(),
                deadlineValue);

        try (FileWriter fw = new FileWriter("vcrts_log.txt", true)) {
            fw.write(record);
            JOptionPane.showMessageDialog(frame, "Transaction stored successfully!");

            idField.setText("");
            infoField.setText("");
            durationField.setText("");
            deadlineField.setText("");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving to file.");
        }
    }
 
    // Integrated Clock Component [cite: 42, 72]
    static class LiveClockPanel extends JPanel {
        private JLabel clockLabel;
        public LiveClockPanel() {
            clockLabel = new JLabel();
            clockLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 18));
            clockLabel.setForeground(Color.CYAN);
            add(clockLabel);
            new Timer(1000, e ->
                clockLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()))
            ).start();
        }
    }

    public static void main(String[] args) {
        new createPanel();
    }
}