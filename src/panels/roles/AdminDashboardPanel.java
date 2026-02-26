package panels.roles;

import models.Job;
import models.Role;
import models.User;
import models.Vehicle;
import panels.DashboardPanel;
import services.AuthService;
import services.CloudLogService;
import services.JobService;
import services.VehicleService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

public class AdminDashboardPanel extends JPanel {
    private final User currentUser;
    private final AuthService authService;
    private final JobService jobService;
    private final VehicleService vehicleService;
    private final CloudLogService cloudLogService;

    private final DashboardPanel dashboardPanel;

    private final DefaultTableModel usersModel;
    private final JTable usersTable;
    private final JTextField usernameField;
    private final JTextField passwordField;
    private final JComboBox<Role> roleCombo;

    private final DefaultTableModel vehiclesModel;
    private final JTable vehiclesTable;

    private final DefaultTableModel jobsModel;
    private final JTable jobsTable;

    public AdminDashboardPanel(
            User currentUser,
            AuthService authService,
            JobService jobService,
            VehicleService vehicleService,
            CloudLogService cloudLogService) {
        this.currentUser = currentUser;
        this.authService = authService;
        this.jobService = jobService;
        this.vehicleService = vehicleService;
        this.cloudLogService = cloudLogService;
        this.dashboardPanel = new DashboardPanel();

        setLayout(new BorderLayout(8, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("System Admin Dashboard"));
        JButton refreshAllButton = new JButton("Refresh All");
        refreshAllButton.addActionListener(e -> refreshAll());
        topBar.add(refreshAllButton);
        add(topBar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        usersModel = new DefaultTableModel(new Object[]{"User ID", "Username", "Role"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(usersModel);
        usernameField = new JTextField(16);
        passwordField = new JTextField(16);
        roleCombo = new JComboBox<>(new Role[]{
                Role.VEHICLE_OWNER,
                Role.JOB_SUBMITTER,
                Role.JOB_CONTROLLER,
                Role.SYSTEM_ADMIN
        });
        tabs.addTab("Users", createUsersTab());

        vehiclesModel = new DefaultTableModel(
                new Object[]{"Vehicle ID", "Owner ID", "Vehicle Info", "Residency", "Total Earned ($)", "Jobs Worked", "Jobs In Progress"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        vehiclesTable = new JTable(vehiclesModel);
        tabs.addTab("Vehicles", createVehiclesTab());

        jobsModel = new DefaultTableModel(
                new Object[]{"Queue Rank", "Job ID", "Assigned Vehicle", "Submitter", "Description", "Deadline", "Cost", "Status", "Interrupts", "Failures", "Reports"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jobsTable = new JTable(jobsModel);
        tabs.addTab("Jobs", createJobsTab());

        tabs.addTab("Monitor", createMonitorTab());

        add(tabs, BorderLayout.CENTER);
        refreshAll();
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel form = new JPanel();
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        form.add(new JLabel("Role:"));
        form.add(roleCombo);

        JButton addButton = new JButton("Add User");
        JButton removeButton = new JButton("Remove User");
        JButton refreshButton = new JButton("Refresh Users");

        addButton.addActionListener(e -> addUser());
        removeButton.addActionListener(e -> removeSelectedUser());
        refreshButton.addActionListener(e -> refreshUsers());

        form.add(addButton);
        form.add(removeButton);
        form.add(refreshButton);

        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    private JPanel createVehiclesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(vehiclesTable), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Vehicles");
        refreshButton.addActionListener(e -> refreshVehicles());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refreshButton);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createJobsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Jobs");
        refreshButton.addActionListener(e -> refreshJobs());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refreshButton);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMonitorTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(dashboardPanel, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Monitor");
        refreshButton.addActionListener(e -> refreshDashboard());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(refreshButton);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void addUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        Role role = (Role) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            JOptionPane.showMessageDialog(this, "Username, password, and role are required.");
            return;
        }

        boolean added = authService.addUser(authService.nextUserId(), username, password, role);
        if (!added) {
            JOptionPane.showMessageDialog(this, "Could not add user (username may already exist).");
            return;
        }

        usernameField.setText("");
        passwordField.setText("");
        refreshUsers();
        JOptionPane.showMessageDialog(this, "User added.");
    }

    private void removeSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to remove.");
            return;
        }

        String username = String.valueOf(usersModel.getValueAt(selectedRow, 1));
        if (currentUser.getUsername().equalsIgnoreCase(username)) {
            JOptionPane.showMessageDialog(this, "You cannot remove the currently logged-in admin user.");
            return;
        }

        boolean removed = authService.removeUser(username);
        if (!removed) {
            JOptionPane.showMessageDialog(this, "Could not remove user.");
            return;
        }

        refreshUsers();
        JOptionPane.showMessageDialog(this, "User removed.");
    }

    private void refreshAll() {
        refreshUsers();
        refreshVehicles();
        refreshJobs();
        refreshDashboard();
    }

    private void refreshUsers() {
        usersModel.setRowCount(0);
        for (User user : authService.getUsersList()) {
            usersModel.addRow(new Object[]{
                    user.getUserId(),
                    user.getUsername(),
                    user.getRole().name()
            });
        }
    }

    private void refreshVehicles() {
        vehiclesModel.setRowCount(0);
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        for (Vehicle vehicle : vehicles) {
            vehiclesModel.addRow(new Object[]{
                    vehicle.getVehicleId(),
                    vehicle.getOwnerUserId(),
                    vehicle.getVehicleInfo(),
                    vehicle.getResidencyHours(),
                    vehicle.getTotalEarnings().toPlainString(),
                    vehicle.getJobsWorked(),
                    vehicle.getJobsInProgress()
            });
        }
    }

    private void refreshJobs() {
        jobsModel.setRowCount(0);
        List<Job> jobs = jobService.getQueueOrderedJobs();
        for (Job job : jobs) {
            jobsModel.addRow(new Object[]{
                    job.getPriorityRank(),
                    job.getJobId(),
                    emptyIfBlank(job.getAssignedVehicleId()),
                    job.getSubmitterUserId(),
                    job.getDescription(),
                    job.getDeadline(),
                    job.getCostForJob().toPlainString(),
                    job.getStatus().name(),
                    job.getInterrupts(),
                    job.getFailures(),
                    job.getReportLines().size()
            });
        }
    }

    private String emptyIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "(unassigned)";
        }
        return value.trim();
    }

    private void refreshDashboard() {
        try {
            dashboardPanel.setLogs(cloudLogService.readAll());
        } catch (IOException ex) {
            dashboardPanel.showError("Database Connection Error.");
        }
    }
}
