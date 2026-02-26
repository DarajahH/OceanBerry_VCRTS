package panels;

import services.CloudLogService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.io.IOException;

public class DashboardScreen {
    private final JFrame frame;
    private final DashboardPanel dashboardPanel;
    private final CloudLogService cloudLogService;

    public DashboardScreen(CloudLogService cloudLogService) {
        this.cloudLogService = cloudLogService;
        this.frame = new JFrame("VCRTS Dashboard");
        this.dashboardPanel = new DashboardPanel();

        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(840, 620);
        frame.setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        javax.swing.JLabel title = new javax.swing.JLabel("Cloud Dashboard", SwingConstants.LEFT);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(refreshButton, BorderLayout.EAST);

        frame.add(topBar, BorderLayout.NORTH);
        frame.add(dashboardPanel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        refresh();
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
        frame.toFront();
    }

    public void refresh() {
        try {
            dashboardPanel.setLogs(cloudLogService.readAll());
        } catch (IOException ex) {
            dashboardPanel.showError("Database Connection Error.");
        }
    }
}
