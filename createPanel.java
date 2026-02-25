import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;

/**
 * VCRTS Cloud Admin Powerhouse
 * Student Note: This class handles the GUI and File I/O for Milestone 2.
 */
public class createPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "vcrts_log.txt";

    private JTextField idField, infoField, durationField, deadlineField;
    private JLabel idLabel, infoLabel, durLabel, deadlineLabel, adminInstruction;
    private JComboBox<String> roleSelector; 
    private JTextArea dashboardArea; 
    private JButton approveBtn, rejectBtn, submitBtn;
    private JFrame frame;
    private JPanel welcomeOverlay;

    public createPanel() {
        frame = new JFrame("VCRTS - Cloud Admin Powerhouse");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(null); 
        frame.getContentPane().setBackground(new Color(10, 10, 10));

        // --- 1. WELCOME OVERLAY ---
        // Student Note: This provides the "Welcome Screen" requirement.
        createWelcomeOverlay();

        // --- 2. MAIN DASHBOARD CONTENT ---
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
        mainContent.setBounds(0, 0, 1200, 800);

        // Header and Clock
        JLabel title = new JLabel("VCRTS CLOUD CONTROL CENTER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 32));
        title.setBounds(50, 45, 650, 40);
        mainContent.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(950, 45, 200, 40);
        mainContent.add(clock);

        // --- 3. INPUT FIELDS ---
        int lx = 60, fx = 250, sy = 130, sp = 55;

        addStyledLabel(mainContent, "Switch User Role:", lx, sy);
        roleSelector = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleSelector.setBounds(fx, sy, 240, 35);
        mainContent.add(roleSelector);

        adminInstruction = new JLabel("<html><i>Admin Mode: Decision power active.</i></html>");
        adminInstruction.setForeground(Color.CYAN);
        adminInstruction.setBounds(fx, sy + 35, 240, 20);
        adminInstruction.setVisible(false);
        mainContent.add(adminInstruction);

        idLabel = addStyledLabel(mainContent, "Owner ID:", lx, sy + sp);
        idField = new JTextField();
        idField.setBounds(fx, sy + sp, 240, 30);
        mainContent.add(idField);

        infoLabel = addStyledLabel(mainContent, "Vehicle Info:", lx, sy + (sp * 2));
        infoField = new JTextField();
        infoField.setBounds(fx, sy + (sp * 2), 240, 30);
        mainContent.add(infoField);

        durLabel = addStyledLabel(mainContent, "Residency (Hrs):", lx, sy + (sp * 3));
        durationField = new JTextField();
        durationField.setBounds(fx, sy + (sp * 3), 240, 30);
        // Student Note: DigitsOnlyFilter prevents invalid non-numeric input.
        ((AbstractDocument) durationField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(durationField);

        deadlineLabel = addStyledLabel(mainContent, "Job Deadline:", lx, sy + (sp * 4));
        deadlineField = new JTextField(); 
        deadlineField.setBounds(fx, sy + (sp * 4), 240, 30);
        deadlineField.setVisible(false); // Hidden for Owners
        ((AbstractDocument) deadlineField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(deadlineField);

        // --- 4. ACTION BUTTONS ---
        approveBtn = new JButton("APPROVE REQUEST");
        approveBtn.setBounds(fx, sy + (sp * 5), 240, 35);
        approveBtn.setBackground(new Color(52, 199, 89));
        approveBtn.setVisible(false);
        mainContent.add(approveBtn);

        rejectBtn = new JButton("REJECT REQUEST");
        rejectBtn.setBounds(fx, sy + (sp * 5) + 40, 240, 35);
        rejectBtn.setBackground(new Color(255, 59, 48));
        rejectBtn.setVisible(false);
        mainContent.add(rejectBtn);

        submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fx, sy + (sp * 6), 240, 45);
        mainContent.add(submitBtn);

        // --- 5. DATABASE MONITOR ---
        // Student Note: This displays the vcrts_log.txt content in real-time.
        addStyledLabel(mainContent, "Global Cloud Database Monitor:", 550, 100);
        dashboardArea = new JTextArea();
        dashboardArea.setEditable(false);
        dashboardArea.setBackground(new Color(5, 5, 5));
        dashboardArea.setForeground(new Color(50, 255, 100));
        dashboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(dashboardArea);
        scroll.setBounds(550, 130, 580, 550);
        mainContent.add(scroll);

        // Listeners
        roleSelector.addActionListener(e -> updateUI((String)roleSelector.getSelectedItem()));
        submitBtn.addActionListener(e -> handleAction("SUBMITTED"));
        approveBtn.addActionListener(e -> handleAction("APPROVED"));
        rejectBtn.addActionListener(e -> handleAction("REJECTED"));

        frame.add(welcomeOverlay);
        frame.add(mainContent);
        
        frame.setVisible(true);
        loadDatabase();
    }

    private void createWelcomeOverlay() {
        welcomeOverlay = new JPanel();
        welcomeOverlay.setBounds(0, 0, 1200, 800);
        welcomeOverlay.setBackground(new Color(15, 15, 15, 250));
        welcomeOverlay.setLayout(new GridBagLayout());
        
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(500, 300));
        card.setBackground(new Color(30, 30, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

        JLabel welcomeTitle = new JLabel("WELCOME TO VCRTS");
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeTitle.setFont(new Font("Helvetica Neue", Font.BOLD, 32));
        welcomeTitle.setForeground(Color.WHITE);

        // Student Note: Button updated with Black Text for clarity.
        JButton enterBtn = new JButton("ENTER CONSOLE");
        enterBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        enterBtn.setBackground(Color.WHITE);
        enterBtn.setForeground(Color.BLACK); // Set text to black
        enterBtn.setFocusPainted(false);
        enterBtn.setFont(new Font("Helvetica Neue", Font.BOLD, 14));
        enterBtn.addActionListener(e -> {
            welcomeOverlay.setVisible(false);
            frame.repaint();
        });

        card.add(Box.createVerticalGlue());
        card.add(welcomeTitle);
        card.add(Box.createVerticalStrut(40));
        card.add(enterBtn);
        card.add(Box.createVerticalGlue());

        welcomeOverlay.add(card);
    }

    private void updateUI(String role) {
        boolean isAdmin = role.equals("ADMIN");
        boolean isClient = role.equals("CLIENT");

        idLabel.setText(isClient ? "Client ID:" : (isAdmin ? "Target ID:" : "Owner ID:"));
        infoLabel.setText(isClient ? "Job Desc:" : (isAdmin ? "Review Info:" : "Vehicle Info:"));
        durLabel.setText(isClient ? "Job Dur (Hrs):" : "Time (Hrs):");
        
        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
        adminInstruction.setVisible(isAdmin);
        approveBtn.setVisible(isAdmin);
        rejectBtn.setVisible(isAdmin);
        submitBtn.setVisible(!isAdmin);
    }

    private void handleAction(String status) {
        String role = (String)roleSelector.getSelectedItem();
        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durationField.getText().trim();
        String deadline = "CLIENT".equals(role) ? deadlineField.getText().trim() : "N/A";

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter all information.");
            return;
        }

        String ts = LocalDateTime.now().format(TS_FMT);
        String entry = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s | STATUS: %s", 
                                     ts, role, id, info, dur, deadline, status);

        try {
            // Student Note: Files.writeString handles the File I/O for the database.
            Files.writeString(Paths.get(LOG_FILE), entry + System.lineSeparator(), 
                              StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            loadDatabase();
            clearInputs();
        } catch (IOException ex) { JOptionPane.showMessageDialog(frame, "Error saving to cloud."); }
    }

    private void loadDatabase() {
        try {
            Path path = Paths.get(LOG_FILE);
            if (Files.exists(path)) {
                dashboardArea.setText("");
                List<String> logs = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (String log : logs) dashboardArea.append(log + "\n");
            }
        } catch (IOException e) { dashboardArea.setText("Database Connection Error."); }
    }

    private void clearInputs() {
        idField.setText(""); infoField.setText(""); durationField.setText(""); deadlineField.setText("");
    }

    private JLabel addStyledLabel(JPanel panel, String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        label.setBounds(x, y, 180, 30);
        panel.add(label);
        return label;
    }

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