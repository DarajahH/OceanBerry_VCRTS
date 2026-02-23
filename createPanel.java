import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.text.*;

public class CreatePanel { // 1) Class name PascalCase
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JTextField idField, infoField, durationField, deadlineField;
    private JToggleButton roleToggle;
    private JFrame frame;

    public CreatePanel() {
        frame = new JFrame("VCRTS - Cloud Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30));

        JPanel glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // 2) ensure background paints correctly
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int inset = 20;
                int w = Math.max(0, getWidth() - inset * 2);
                int h = Math.max(0, getHeight() - inset * 2);

                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(inset, inset, w, h, 30, 30);
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.drawRoundRect(inset, inset, Math.max(0, w - 1), Math.max(0, h - 1), 30, 30);
                g2d.dispose();
            }
        };

        glassPanel.setOpaque(false);
        glassPanel.setLayout(null);

        JLabel title = new JLabel("VCRTS MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 24));
        title.setBounds(50, 40, 400, 40);
        glassPanel.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(650, 40, 200, 40);
        glassPanel.add(clock);

        int labelX = 100, fieldX = 300, startY = 120, spacing = 50;

        addStyledLabel(glassPanel, "User Role:", labelX, startY);

        // 3) Toggle semantics: selected == CLIENT (consistent with label)
        roleToggle = new JToggleButton("OWNER");
        roleToggle.setBounds(fieldX, startY, 200, 35);
        roleToggle.setFocusPainted(false);
        roleToggle.setBackground(new Color(52, 199, 89));
        roleToggle.setForeground(Color.BLACK);
        roleToggle.setFont(new Font("Helvetica Neue", Font.BOLD, 14));
        glassPanel.add(roleToggle);

        addStyledLabel(glassPanel, "User/Owner ID:", labelX, startY + spacing);
        idField = new JTextField();
        idField.setBounds(fieldX, startY + spacing, 200, 30);
        glassPanel.add(idField);

        addStyledLabel(glassPanel, "Vehicle Info / Job Info:", labelX, startY + (spacing * 2));
        infoField = new JTextField();
        infoField.setBounds(fieldX, startY + (spacing * 2), 200, 30);
        glassPanel.add(infoField);

        addStyledLabel(glassPanel, "Duration (Hours):", labelX, startY + (spacing * 3));
        durationField = new JTextField();
        durationField.setBounds(fieldX, startY + (spacing * 3), 200, 30);

        // 4) Restrict duration to digits only (no “abc” even allowed)
        ((AbstractDocument) durationField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        glassPanel.add(durationField);

        addStyledLabel(glassPanel, "Deadline (if Client):", labelX, startY + (spacing * 4));
        deadlineField = new JTextField();
        deadlineField.setBounds(fieldX, startY + (spacing * 4), 200, 30);
        deadlineField.setEnabled(false);

        // 5) Restrict deadline to digits only (if you intend it to be numeric)
        ((AbstractDocument) deadlineField.getDocument()).setDocumentFilter(new DigitsOnlyFilter());
        glassPanel.add(deadlineField);

        roleToggle.addItemListener(e -> {
            boolean isClient = roleToggle.isSelected(); // selected => client
            if (isClient) {
                roleToggle.setText("CLIENT");
                roleToggle.setBackground(new Color(0, 122, 255));
                deadlineField.setEnabled(true);
            } else {
                roleToggle.setText("OWNER");
                roleToggle.setBackground(new Color(52, 199, 89));
                deadlineField.setEnabled(false);
                deadlineField.setText("");
            }
        });

        JButton submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fieldX, startY + (spacing * 5), 200, 40);
        submitBtn.setBackground(new Color(0, 122, 255));
        submitBtn.setForeground(Color.BLACK);
        submitBtn.setFocusPainted(false);

        // 6) Don’t implement ActionListener on the whole class; use a handler method
        submitBtn.addActionListener(evt -> handleSubmit());
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

    // 7) Extract submit logic (minimal change, but cleaner)
    private void handleSubmit() {
        boolean isClient = roleToggle.isSelected();
        String role = isClient ? "Client" : "Owner";

        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String durText = durationField.getText().trim();
        String deadlineText = deadlineField.getText().trim();

        if (id.isEmpty() || info.isEmpty() || durText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill all required fields.");
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Duration must be a number.");
            return;
        }
        // 8) Add basic bounds (reject 0/negative)
        if (duration <= 0 || duration > 168) {
            JOptionPane.showMessageDialog(frame, "Duration must be between 1 and 168 hours.");
            return;
        }

        String deadlineValue = "N/A";
        if (isClient) {
            if (deadlineText.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Client must enter a deadline.");
                return;
            }
            int deadline;
            try {
                deadline = Integer.parseInt(deadlineText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Deadline must be a number.");
                return;
            }
            if (deadline <= 0) {
                JOptionPane.showMessageDialog(frame, "Deadline must be a positive number.");
                return;
            }
            deadlineValue = deadlineText;
        }

        // 9) Use java.time for timestamp
        String timestamp = LocalDateTime.now().format(TS_FMT);

        // 10) Sanitize newlines to keep log format stable
        id = sanitizeOneLine(id);
        info = sanitizeOneLine(info);

        String record = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %d | DEADLINE: %s%n",
                timestamp, role, id, info, duration, deadlineValue);

        // 11) Use NIO Files w/ UTF-8 + stable path behavior
        Path logPath = Paths.get("vcrts_log.txt");
        try {
            Files.writeString(
                    logPath,
                    record,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
            JOptionPane.showMessageDialog(frame, "Transaction stored successfully!");

            idField.setText("");
            infoField.setText("");
            durationField.setText("");
            deadlineField.setText("");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving to file: " + ex.getMessage());
        }
    }

    private static String sanitizeOneLine(String s) {
        return s.replace("\r", " ").replace("\n", " ").trim();
    }

    // 12) Digits-only filter for numeric text fields
    static class DigitsOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    static class LiveClockPanel extends JPanel {
        private final JLabel clockLabel;

        public LiveClockPanel() {
            clockLabel = new JLabel();
            clockLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 18));
            clockLabel.setForeground(Color.CYAN);
            add(clockLabel);

            // 13) Use java.time for clock too
            new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
                    .start();
        }
    }

    public static void main(String[] args) {
        // 14) Build UI on EDT
        SwingUtilities.invokeLater(CreatePanel::new);
    }
}