
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

public class createPanel implements ActionListener {
    // Fields for data collection [cite: 10-16]
    private static JTextField idField, infoField, durationField, deadlineField;
    private static JComboBox<String> roleSelector;
    private static JFrame frame;

    public static void main(String[] args) {
        frame = new JFrame("VCRTS - Cloud Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30)); // Dark Apple-style background

        // Glass Panel Container
        JPanel glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30)); // Frosted Glass effect 
                g2d.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 30, 30);
                g2d.setColor(new Color(255, 255, 255, 60)); 
                g2d.drawRoundRect(20, 20, getWidth() - 41, getHeight() - 41, 30, 30);
                g2d.dispose();
            }
        };
        glassPanel.setOpaque(false);
        glassPanel.setLayout(null); // Absolute positioning for precise Apple-style UI

        // Header with Live Clock [cite: 41, 42]
        JLabel title = new JLabel("VCRTS MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 24));
        title.setBounds(50, 40, 400, 40);
        glassPanel.add(title);

        LiveClockPanel clock = new LiveClockPanel();
        clock.setBounds(650, 40, 200, 40);
        clock.setOpaque(false);
        glassPanel.add(clock);

        // Input Fields [cite: 8-16]
        int labelX = 100, fieldX = 300, startY = 120, spacing = 50;

        addStyledLabel(glassPanel, "User Role:", labelX, startY);
        roleSelector = new JComboBox<>(new String[]{"Owner", "Client"});
        roleSelector.setBounds(fieldX, startY, 200, 30);
        glassPanel.add(roleSelector);

        addStyledLabel(glassPanel, "User/Owner ID:", labelX, startY + spacing);
        idField = new JTextField();
        idField.setBounds(fieldX, startY + spacing, 200, 30);
        glassPanel.add(idField);

        addStyledLabel(glassPanel, "Vehicle Info / Job Info:", labelX, startY + (spacing * 2));
        infoField = new JTextField();
        infoField.setBounds(fieldX, startY + (spacing * 2), 200, 30);
        glassPanel.add(infoField);

        addStyledLabel(glassPanel, "Duration (Usage/Job):", labelX, startY + (spacing * 3));
        durationField = new JTextField();
        durationField.setBounds(fieldX, startY + (spacing * 3), 200, 30);
        glassPanel.add(durationField);

        addStyledLabel(glassPanel, "Deadline (if Client):", labelX, startY + (spacing * 4));
        deadlineField = new JTextField();
        deadlineField.setBounds(fieldX, startY + (spacing * 4), 200, 30);
        glassPanel.add(deadlineField);

        // Submit Button 
        JButton submitBtn = new JButton("Submit to Cloud");
        submitBtn.setBounds(fieldX, startY + (spacing * 5), 200, 40);
        submitBtn.setBackground(new Color(0, 122, 255)); // Apple Blue
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.addActionListener(new createPanel());
        glassPanel.add(submitBtn);

        frame.add(glassPanel);
        frame.setVisible(true);
    }

    private static void addStyledLabel(JPanel panel, String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        label.setBounds(x, y, 200, 30);
        panel.add(label);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String role = (String) roleSelector.getSelectedItem();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        
        // Data Formatting for File [cite: 7, 64]
        String record = String.format("[%s] ROLE: %s | ID: %s | INFO: %s | DUR: %s | DEADLINE: %s\n",
                timestamp, role, idField.getText(), infoField.getText(), 
                durationField.getText(), deadlineField.getText());

        try (FileWriter fw = new FileWriter("vcrts_log.txt", true)) {
            fw.write(record);
            JOptionPane.showMessageDialog(frame, "Transaction stored successfully!");
            // Clear fields for multiple entries 
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
            new Timer(1000, e -> clockLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()))).start();
        }
    }
}