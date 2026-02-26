import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;

public class createPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "vcrts_log.txt";

    private JTextField idField, infoField, durationField, deadlineField;
    private JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private JComboBox<String> roleSelector; 
    private JTextArea dashboardArea; 
    private JButton approveBtn, rejectBtn, submitBtn;
    private JFrame frame;

    public createPanel() {
        // Initial setup
        frame = new JFrame("VCRTS - Cloud Management & Admin Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(18, 18, 18));

        // Glass UI Design
        JPanel mainContent = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 12));
                g2d.fillRoundRect(20, 20, getWidth()-40, getHeight()-40, 30, 30);
                g2d.dispose();
            }
        };
        mainContent.setOpaque(false);
        mainContent.setLayout(null);

        // UI Header
        JLabel title = new JLabel("VCRTS CLOUD CONTROL CENTER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 30));
        title.setBounds(50, 45, 600, 40);
        mainContent.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(900, 45, 200, 40);
        mainContent.add(clock);

        // Sidebar Configuration
        int lx = 60, fx = 250, sy = 130, sp = 55;

        addStyledLabel(mainContent, "Selected Role:", lx, sy);
        roleSelector = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleSelector.setBounds(fx, sy, 220, 35);
        mainContent.add(roleSelector);

        idLabel = addStyledLabel(mainContent, "User ID:", lx, sy + sp);
        idField = new JTextField();
        idField.setBounds(fx, sy + sp, 220, 30);
        mainContent.add(idField);

        infoLabel = addStyledLabel(mainContent, "Information:", lx, sy + (sp * 2));
        infoField = new JTextField();
        infoField.setBounds(fx, sy + (sp * 2), 220, 30);
        mainContent.add(infoField);

        durLabel = addStyledLabel(mainContent, "Duration (Hrs):", lx, sy + (sp * 3));
        durationField = new JTextField();
        durationField.setBounds(fx, sy + (sp * 3), 220, 30);
        ((AbstractDocument) durationField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(durationField);

        deadlineLabel = addStyledLabel(mainContent, "Deadline:", lx, sy + (sp * 4));
        deadlineField = new JTextField(); 
        deadlineField.setBounds(fx, sy + (sp * 4), 220, 30);
        deadlineField.setEnabled(false); 
        ((AbstractDocument) deadlineField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(deadlineField);

        // ADMIN POWERHOUSE BUTTONS (Approve/Reject)
        approveBtn = new JButton("APPROVE REQUEST");
        approveBtn.setBounds(fx, sy + (sp * 5), 220, 35);
        approveBtn.setBackground(new Color(52, 199, 89));
        approveBtn.setVisible(false); // Only for Admin
        mainContent.add(approveBtn);

        rejectBtn = new JButton("REJECT REQUEST");
        rejectBtn.setBounds(fx, sy + (sp * 5) + 40, 220, 35);
        rejectBtn.setBackground(new Color(255, 59, 48));
        rejectBtn.setVisible(false); // Only for Admin
        mainContent.add(rejectBtn);

        // Submit Button (For Owners/Clients)
        submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fx, sy + (sp * 6), 220, 45);
        mainContent.add(submitBtn);

        // DATABASE DASHBOARD
        addStyledLabel(mainContent, "Live Database Monitor:", 550, 100);
        dashboardArea = new JTextArea();
        dashboardArea.setEditable(false);
        dashboardArea.setBackground(new Color(10, 10, 10));
        dashboardArea.setForeground(new Color(0, 255, 100));
        dashboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(dashboardArea);
        scroll.setBounds(550, 130, 520, 500);
        mainContent.add(scroll);

        // Role Logic
        roleSelector.addActionListener(e -> updateUI((String)roleSelector.getSelectedItem()));

        // Actions
        submitBtn.addActionListener(e -> processEntry("SUBMITTED"));
        approveBtn.addActionListener(e -> processEntry("APPROVED"));
        rejectBtn.addActionListener(e -> processEntry("REJECTED"));

        frame.add(mainContent);
        frame.setVisible(true);

        loadDatabase(); // Synchronize with txt file
        showWelcomeMessage(); 
    }

    private void showWelcomeMessage() {
        JOptionPane.showMessageDialog(frame, 
            "Welcome to the VCRTS Cloud Management System.\nAccess levels: Owner, Client, and Admin Powerhouse.", 
            "System Login Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateUI(String role) {
        boolean isAdmin = role.equals("ADMIN");
        boolean isClient = role.equals("CLIENT");

        idLabel.setText(isClient ? "Client ID:" : (isAdmin ? "Admin ID:" : "Owner ID:"));
        infoLabel.setText(isClient ? "Job Desc:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Dur (Hrs):" : "Residency (Hrs):");
        
        deadlineField.setEnabled(isClient);
        if (!isClient) deadlineField.setText("");

        // Admin controls visibility
        approveBtn.setVisible(isAdmin);
        rejectBtn.setVisible(isAdmin);
        submitBtn.setVisible(!isAdmin);
    }

    private void processEntry(String status) {
        String role = (String)roleSelector.getSelectedItem();
        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durationField.getText().trim();
        String deadline = role.equals("CLIENT") ? deadlineField.getText().trim() : "N/A";

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Error: Fill all fields.");
            return;
        }

        String ts = LocalDateTime.now().format(TS_FMT);
        // Requirement Format: ROLE | ID | INFO | DUR | DEADLINE | STATUS
        String entry = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s | STATUS: %s", 
                                     ts, role, id, info, dur, deadline, status);

        try {
            Files.writeString(Paths.get(LOG_FILE), entry + System.lineSeparator(), 
                              StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            loadDatabase(); // Refresh visual log
            clearInputs();
        } catch (IOException ex) { JOptionPane.showMessageDialog(frame, "Database write error."); }
    }

    private void loadDatabase() {
        try {
            Path path = Paths.get(LOG_FILE);
            if (Files.exists(path)) {
                dashboardArea.setText("");
                List<String> logs = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (String log : logs) dashboardArea.append(log + "\n");
            }
        } catch (IOException e) { dashboardArea.setText("Unable to sync database."); }
    }

    private void clearInputs() {
        idField.setText(""); infoField.setText(""); durationField.setText(""); deadlineField.setText("");
    }

    private JLabel addStyledLabel(JPanel panel, String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(180, 180, 180));
        label.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        label.setBounds(x, y, 180, 30);
        panel.add(label);
        return label;
    }

    // Input Filter Class
    static class DigitsOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d+")) super.insertString(fb, offset, string, attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) super.replace(fb, offset, length, text, attrs);
        }
    }

    // Live Clock Component
    static class LiveClockPanel extends JPanel {
        public LiveClockPanel() {
            setOpaque(false);
            JLabel clockLabel = new JLabel();
            clockLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 18));
            clockLabel.setForeground(Color.CYAN);
            add(clockLabel);
            new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))).start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(createPanel::new);
    }
}