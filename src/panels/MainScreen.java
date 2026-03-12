package panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import models.Role;
import models.User;
import panels.main.MainHeaderPanel;
import panels.roles.AdminDashboardPanel;
import panels.roles.JobControllerPanel;
import panels.roles.JobSubmitterPanel;
import panels.roles.OwnerPanel;
import services.AuthService;
import services.CloudLogService;
import services.JobService;
import services.VehicleService;

public class MainScreen {
    private final JFrame frame;
    private final CloudLogService cloudLogService;
    private final JobService jobService;
    private final VehicleService vehicleService;
    private final User currentUser;
    private final AuthService authService;

    public MainScreen(User currentUser, AuthService authService, JobService jobService, VehicleService vehicleService) {
        this.currentUser = currentUser;
        this.authService = authService;
        this.cloudLogService = new CloudLogService(Paths.get("vcrts_log.txt"));
        this.jobService = jobService;
        this.vehicleService = vehicleService;

        frame = new JFrame("VCRTS - Cloud Management & Admin Console");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(18, 18, 18));

        JPanel root = new JPanel(new BorderLayout()) {
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
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        root.add(new MainHeaderPanel(currentUser, this::logout), BorderLayout.NORTH);
        root.add(createRolePanel(), BorderLayout.CENTER);

        frame.add(root, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Log out and return to login screen?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            frame.dispose();
            new LoginScreen(authService, jobService, vehicleService);
        }
    }

    private JPanel createRolePanel() {
        Role role = currentUser.getRole();
        if (role == Role.VEHICLE_OWNER) {
            return new OwnerPanel(currentUser, vehicleService, cloudLogService);
        }
        if (role == Role.JOB_SUBMITTER) {
            return new JobSubmitterPanel(currentUser, jobService, cloudLogService);
        }
        if (role == Role.JOB_CONTROLLER) {
            return new JobControllerPanel(currentUser, jobService, vehicleService, cloudLogService);
        }
        if (role == Role.SYSTEM_ADMIN) {
            return new AdminDashboardPanel(currentUser, authService, jobService, vehicleService, cloudLogService);
        }

        JPanel fallback = new JPanel(new BorderLayout());
        fallback.add(new JLabel("No panel configured for role: " + role), BorderLayout.CENTER);
        return fallback;
    }
}
