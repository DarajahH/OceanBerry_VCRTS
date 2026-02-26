package panels;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final JLabel totalValue;
    private final JLabel vehicleOwnerValue;
    private final JLabel submitterValue;
    private final JLabel controllerValue;
    private final JLabel adminValue;
    private final JTextArea logArea;

    public DashboardPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // FlatLaf client properties (safe no-op if FlatLaf isn't active).
        putClientProperty("JComponent.roundRect", true);
        putClientProperty("FlatLaf.style", "arc:16");

        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 8, 8));
        totalValue = createMetricCard(statsPanel, "Total Logs");
        vehicleOwnerValue = createMetricCard(statsPanel, "Vehicle Owner");
        submitterValue = createMetricCard(statsPanel, "Job Submitter");
        controllerValue = createMetricCard(statsPanel, "Job Controller");
        adminValue = createMetricCard(statsPanel, "System Admin");
        add(statsPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(5, 5, 5));
        logArea.setForeground(new Color(50, 255, 100));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Global Cloud Database Monitor"));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setLogs(List<String> logs) {
        int vehicleOwner = 0;
        int submitter = 0;
        int controller = 0;
        int admin = 0;

        logArea.setText("");
        for (String log : logs) {
            logArea.append(log);
            logArea.append("\n");

            if (log.contains("ROLE: VEHICLE_OWNER")) {
                vehicleOwner++;
            } else if (log.contains("ROLE: JOB_SUBMITTER")) {
                submitter++;
            } else if (log.contains("ROLE: JOB_CONTROLLER")) {
                controller++;
            } else if (log.contains("ROLE: SYSTEM_ADMIN")) {
                admin++;
            }
        }

        totalValue.setText(String.valueOf(logs.size()));
        vehicleOwnerValue.setText(String.valueOf(vehicleOwner));
        submitterValue.setText(String.valueOf(submitter));
        controllerValue.setText(String.valueOf(controller));
        adminValue.setText(String.valueOf(admin));
    }

    public void showError(String message) {
        logArea.setText(message);
    }

    private JLabel createMetricCard(JPanel container, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        card.putClientProperty("JComponent.roundRect", true);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Helvetica Neue", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(170, 170, 170));

        JLabel valueLabel = new JLabel("0", SwingConstants.CENTER);
        valueLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 20));
        valueLabel.setForeground(new Color(220, 220, 220));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        container.add(card);

        return valueLabel;
    }
}
