package panels;

import services.CloudLogService;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class createPanel {
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "vcrts_log.txt";

    private final JTextField idField, infoField, durationField, deadlineField;
    private final JLabel idLabel, infoLabel, durLabel, deadlineLabel, adminInstruction;
    private final JComboBox<String> roleSelector;
    private final JTextArea dashboardArea;
    private final JButton approveBtn, rejectBtn, submitBtn;
    private final JFrame frame;
    private final CloudLogService cloudLogService;

    public createPanel() {
        this.cloudLogService = new CloudLogService(Paths.get(LOG_FILE));
        frame = new JFrame("VCRTS - Cloud Management & Admin Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(18, 18, 18));

        JPanel mainContent = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 12));
                g2d.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 30, 30);
                g2d.dispose();
            }
        };
        mainContent.setOpaque(false);
        mainContent.setLayout(null);
        mainContent.setBounds(0, 0, 1150, 750);

        JLabel title = new JLabel("VCRTS CLOUD CONTROL CENTER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 32));
        title.setBounds(50, 45, 650, 40);
        mainContent.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(950, 45, 200, 40);
        mainContent.add(clock);

        int lx = 60;
        int fx = 250;
        int sy = 130;
        int sp = 55;

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
        ((AbstractDocument) durationField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(durationField);

        deadlineLabel = addStyledLabel(mainContent, "Job Deadline:", lx, sy + (sp * 4));
        deadlineField = new JTextField();
        deadlineField.setBounds(fx, sy + (sp * 4), 240, 30);
        deadlineField.setVisible(false);
        ((AbstractDocument) deadlineField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        mainContent.add(deadlineField);

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

        addStyledLabel(mainContent, "Global Cloud Database Monitor:", 550, 100);
        dashboardArea = new JTextArea();
        dashboardArea.setEditable(false);
        dashboardArea.setBackground(new Color(5, 5, 5));
        dashboardArea.setForeground(new Color(50, 255, 100));
        dashboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(dashboardArea);
        scroll.setBounds(550, 130, 580, 550);
        mainContent.add(scroll);

        roleSelector.addActionListener(e -> updateUI((String) roleSelector.getSelectedItem()));
        submitBtn.addActionListener(e -> handleAction("SUBMITTED"));
        approveBtn.addActionListener(e -> handleAction("APPROVED"));
        rejectBtn.addActionListener(e -> handleAction("REJECTED"));

        frame.add(mainContent);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        loadDatabase();
    }

    private void handleAction(String status) {
        String role = (String) roleSelector.getSelectedItem();
        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durationField.getText().trim();
        String deadline = "CLIENT".equals(role) ? deadlineField.getText().trim() : "N/A";

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter all information.");
            return;
        }

        String ts = LocalDateTime.now().format(TS_FMT);
        String entry = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s | STATUS: %s", ts, role, id, info, dur, deadline, status);

        try {
            cloudLogService.append(entry);
            loadDatabase();
            clearInputs();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving to cloud.");
        }
    }

    private void loadDatabase() {
        try {
            dashboardArea.setText("");
            List<String> logs = cloudLogService.readAll();
            for (String log : logs) {
                dashboardArea.append(log + "\n");
            }
        } catch (IOException ex) {
            dashboardArea.setText("Database Connection Error.");
        }
    }

    private void updateUI(String role) {
        boolean isAdmin = "ADMIN".equals(role);
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : (isAdmin ? "Target ID:" : "Owner ID:"));
        infoLabel.setText(isClient ? "Job Desc:" : (isAdmin ? "Review Info:" : "Vehicle Info:"));
        durLabel.setText(isClient ? "Job Dur (Hrs):" : "Residency (Hrs):");

        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
        adminInstruction.setVisible(isAdmin);
        approveBtn.setVisible(isAdmin);
        rejectBtn.setVisible(isAdmin);
        submitBtn.setVisible(!isAdmin);
    }

    private void clearInputs() {
        idField.setText("");
        infoField.setText("");
        durationField.setText("");
        deadlineField.setText("");
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
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
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
}
