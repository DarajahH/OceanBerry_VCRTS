package views;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import models.job.Job;
import services.CloudDataService;
import services.VCController;
import services.VCController.JobCompletionRecord;

public class VCRTSDashboard {

    private final JFrame frame;
    private final CloudDataService service;
    private final VCController controller;
    private final String currentUserRole;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final CardLayout cardLayout;
    private final JPanel leftCardContainer;

    // Form Components
    private JComboBox<String> roleBox;
    private JTextField idField, infoField, durField, deadlineField;
    private JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private JTextArea monitorArea;
    private JTable ownerVehicleTable;
    private DefaultTableModel ownerVehicleModel;
    private JTable clientJobTable;
    private DefaultTableModel clientJobModel;
    private JTextArea clientActivityArea;
    private JTable pendingRequestsTable;
    private javax.swing.table.DefaultTableModel pendingRequestsModel;
    private JLabel adminRequestStatusLabel;
    private JLabel notificationBadge;
    private Timer adminRefreshTimer;

    /** Constructor: Initializes the main dashboard frame and sets up the UI components based on user role. -DH */
    public VCRTSDashboard(CloudDataService service, String currentUserRole) {
        this.service = service;
        this.controller = new VCController(service);
        this.currentUserRole = currentUserRole == null ? "CLIENT" : currentUserRole.toUpperCase();

        // 1. Setup Main Frame - Added emoji to title for fun :) -DH
        frame = new JFrame("VCRTS - Cloud Control Center 🫐");
        
        
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/blueraspberry.png")));
        
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(20, 20, 25));

        // 2. Add Header
        frame.add(createHeader(), BorderLayout.NORTH);

        // 3. Setup the CardLayout for the left panel
        cardLayout = new CardLayout();
        leftCardContainer = new JPanel(cardLayout);
        
        // Home panel is the default view, form panel is for submissions, and we can add more as needed
        leftCardContainer.add(createHomePanel(service), "HOME_SCREEN");
        leftCardContainer.add(createCombinedClientPanel(service), "FORM_SCREEN");
        if (isAdminUser()) {
            leftCardContainer.add(createAdminScreen(service), "ADMIN_SCREEN");
        }
        leftCardContainer.add(createTaskOwnerScreen(service), "TASK_OWNER_SCREEN");
        leftCardContainer.add(createVehicleOwnerScreen(service), "VEHICLE_OWNER_SCREEN");
        leftCardContainer.setBackground(new Color(30, 30, 35));

        // 4. Layout: split the secondary panel for roles that need it
        if (hasRightSidePanel()) {
            JComponent rightSidePanel = createRightSidePanel();
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCardContainer, rightSidePanel);
            splitPane.setDividerLocation(400);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            frame.add(splitPane, BorderLayout.CENTER);
        } else {
            frame.add(leftCardContainer, BorderLayout.CENTER);
        }

        // Initialize state:
        adjustFields();
        if (hasRightSidePanel()) {
            refreshMonitor(null);
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (isClientUser()) {
            refreshNotifications();
            displayUnreadNotificationsIfAny();
        }
    }
    
    // --- PANEL CREATION METHODS ---

    /** * Creates the Home Panel. This is a simple welcome screen with buttons to navigate to the submission 
     * form and to calculate completion times. It serves as the landing page after login. -DH 
     */
    public JPanel createHomePanel(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel welcomeLabel = new JLabel("Welcome to VCRTS");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        gbc.gridy = 0;
        panel.add(welcomeLabel, gbc);

        JLabel subLabel = new JLabel("VCRTS HOME");
        subLabel.setForeground(Color.GRAY);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        gbc.gridy = 1;
        gbc.weighty = 0.1;
        panel.add(subLabel, gbc);

        gbc.weighty = 0.1;
        gbc.insets = new Insets(10, 0, 20, 0);

        int nextRow = 3;
        if (isOwnerUser()) {
            JButton btnCalcTimes = createStyledButton("Calculate Completion Times");
            btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
            gbc.gridy = nextRow++;
            panel.add(btnCalcTimes, gbc);
        }

        if (isClientUser()) {
            JButton btnOpenForm = createStyledButton("Submit New Transaction");
            btnOpenForm.addActionListener(e -> showScreen(createCombinedClientPanel(service)));
            gbc.gridy = nextRow++;
            gbc.insets = new Insets(20, 0, 10, 0);
            panel.add(btnOpenForm, gbc);
        }

        JButton btnResidencyView = createStyledButton("View Residency Time");
        btnResidencyView.addActionListener(e -> showResidencyTimeOverview());
        gbc.gridy = nextRow++;
        gbc.insets = new Insets(10, 0, 10, 0);
        panel.add(btnResidencyView, gbc);

        if (isClientUser()) {
            JButton taskOwnerBtn = createStyledButton("Task Owner Portal");
            taskOwnerBtn.addActionListener(e -> showScreen(createTaskOwnerScreen(service)));
            gbc.gridy = nextRow++;
            gbc.insets = new Insets(10, 0, 10, 0);
            panel.add(taskOwnerBtn, gbc);
        }

        if (isOwnerUser()) {
            JButton vehicleOwnerBtn = createStyledButton("Vehicle Owner Portal");
            vehicleOwnerBtn.addActionListener(e -> showScreen(createVehicleOwnerScreen(service)));
            gbc.gridy = nextRow++;
            gbc.insets = new Insets(10, 0, 10, 0);
            panel.add(vehicleOwnerBtn, gbc);
        }

        if (isAdminUser()) {
            JButton btnAdminScreen = createStyledButton("Go to Admin Screen");
            btnAdminScreen.addActionListener(e -> showScreen(createAdminScreen(service)));
            gbc.gridy = nextRow++;
            panel.add(btnAdminScreen, gbc);
        }

        return panel;
    }

    /** Creates the Admin Screen to view and manage pending client requests. -DH */
    public JPanel createAdminScreen(CloudDataService service) {
        JPanel adminPanel = new JPanel(new GridBagLayout());
        adminPanel.setBackground(new Color(30, 30, 35));
        adminPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel titleLabel = new JLabel("Admin Screen");
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        adminPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        adminRequestStatusLabel = new JLabel("Waiting for client request...");
        adminRequestStatusLabel.setForeground(Color.WHITE);
        adminRequestStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        adminPanel.add(adminRequestStatusLabel, gbc);

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH; // Allow table to expand
        gbc.weighty = 0.5;

        // Create the structured table model
        String[] columns = {"Request ID", "Submitter", "Role", "Details"};
        pendingRequestsModel = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        pendingRequestsTable = new JTable(pendingRequestsModel);
        pendingRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingRequestsTable.setBackground(Color.BLACK);
        pendingRequestsTable.setForeground(Color.GREEN);
        pendingRequestsTable.setGridColor(Color.DARK_GRAY);
        pendingRequestsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        pendingRequestsTable.setRowHeight(22);
        TableColumn col0 = pendingRequestsTable.getColumnModel().getColumn(0);
        TableColumn col1 = pendingRequestsTable.getColumnModel().getColumn(1);
        TableColumn col2 = pendingRequestsTable.getColumnModel().getColumn(2);
        TableColumn col3 = pendingRequestsTable.getColumnModel().getColumn(3);
        col0.setPreferredWidth(280);
        col1.setPreferredWidth(120);
        col2.setPreferredWidth(90);
        col3.setPreferredWidth(520);
        pendingRequestsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedPendingRequestDetails();
                }
            }
        });

        JScrollPane pendingScrollPane = new JScrollPane(pendingRequestsTable);
        pendingScrollPane.setPreferredSize(new Dimension(920, 260));
        adminPanel.add(pendingScrollPane, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel adminActionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        adminActionRow.setBackground(new Color(30, 30, 35));
        JButton refreshBtn = createStyledButton("Refresh");
        refreshBtn.addActionListener(e -> refreshPendingAdminRequest());
        JButton viewDetailsBtn = createStyledButton("View full details");
        viewDetailsBtn.addActionListener(e -> showSelectedPendingRequestDetails());
        adminActionRow.add(refreshBtn);
        adminActionRow.add(viewDetailsBtn);
        adminPanel.add(adminActionRow, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton btnCalcTimes = createStyledButton("Calculate Completion Times");
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
        adminPanel.add(btnCalcTimes, gbc);

        gbc.gridy = 5;
        JButton acceptBtn = createStyledButton("Accept");
        acceptBtn.addActionListener(e -> submitAdminDecision("ACCEPTED"));
        adminPanel.add(acceptBtn, gbc);

        gbc.gridy = 6;
        JButton rejectBtn = createStyledButton("Reject");
        rejectBtn.addActionListener(e -> submitAdminDecision("REJECTED"));
        adminPanel.add(rejectBtn, gbc);

        gbc.gridy = 7;
        JButton backBtn = createStyledButton("Back to Home");
        backBtn.addActionListener(e -> {
            stopAdminRefreshTimer();
            showScreen(createHomePanel(service));
        });
        adminPanel.add(backBtn, gbc);

        startAdminRefreshTimer();
        refreshPendingAdminRequest();

        return adminPanel;
    }

    /** Creates the top header bar with branding, role info, and logout/notification controls. -DH */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 15, 20));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("VCRTS CLOUD DASHBOARD");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JLabel roleLabel = new JLabel("Role: " + currentUserRole);
        roleLabel.setForeground(Color.LIGHT_GRAY);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(roleLabel, BorderLayout.CENTER);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);

        JButton notificationsBtn = createStyledButton("Notifications");
        notificationsBtn.setPreferredSize(new Dimension(140, 36));
        notificationsBtn.addActionListener(e -> showClientNotifications());
        rightHeader.add(notificationsBtn);

        notificationBadge = createNotificationBadge(0);
        rightHeader.add(notificationBadge);

        JButton logoutBtn = createStyledButton("Logout");
        logoutBtn.addActionListener(e -> {
            stopAdminRefreshTimer();
            frame.dispose();
            new LoginScreen(service);
        });
        rightHeader.add(logoutBtn);

        header.add(rightHeader, BorderLayout.EAST);

        refreshNotifications();
        return header;
    }

    /** Creates the unified client portal containing tabs for Job Submission and Vehicle Registration. -DH */
    private JPanel createCombinedClientPanel(CloudDataService service) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 30, 35));

        JLabel title = new JLabel("Unified Client Portal", SwingConstants.CENTER);
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(20, 10, 10, 10));
        mainPanel.add(title, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));
        // Add the two separate screens as tabs
        tabbedPane.addTab("Submit Job Request", createJobSubmissionTab());
        tabbedPane.addTab("Register Vehicle", createVehicleSubmissionTab(service));
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    // Shared Back Button at the bottom
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(30, 30, 35));
        bottomPanel.add(createBackToHomeButton(service));
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /** Creates the Job Submission Tab layout and form fields. -DH */
    private JPanel createJobSubmissionTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createWhiteLabel("Select Role:"), gbc);

        roleBox = new JComboBox<>(new String[]{"CLIENT"});
        roleBox.setEnabled(false);
        roleBox.addActionListener(e -> adjustFields());
        gbc.gridx = 1;
        panel.add(roleBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        idLabel = createWhiteLabel("Client ID:");
        panel.add(idLabel, gbc);
        idField = new JTextField();
        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        infoLabel = createWhiteLabel("Job Description:");
        panel.add(infoLabel, gbc);
        infoField = new JTextField();
        gbc.gridx = 1;
        panel.add(infoField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        durLabel = createWhiteLabel("Duration (Hrs):");
        panel.add(durLabel, gbc);
        durField = new JTextField("e.g., '2' for 2 hours");
        gbc.gridx = 1;
        panel.add(durField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        deadlineLabel = createWhiteLabel("Job Deadline (YYYY/MM/DD HH:MM:SS):");
        panel.add(deadlineLabel, gbc);
        deadlineField = new JTextField();
        gbc.gridx = 1;
        panel.add(deadlineField, gbc);

        JButton submitBtn = createStyledButton("Submit Transaction");
        submitBtn.addActionListener(e -> saveEntry());
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 10, 10);
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    /** Creates the Vehicle Registration Tab layout and form fields. -DH */
    private JPanel createVehicleSubmissionTab(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        panel.add(createWhiteLabel("Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createWhiteLabel("Vehicle Info:"), gbc);
        JTextField vehicleInfoField = new JTextField();
        gbc.gridx = 1;
        panel.add(vehicleInfoField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Residency Hours (Hrs):"), gbc);
        JTextField residencyField = new JTextField("e.g., '4' for 4 hours");
        gbc.gridx = 1;
        panel.add(residencyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Vehicle Status:"), gbc);
        JComboBox<String> statusBox = new JComboBox<>(
        new String[]{"IDLE", "IN_SERVICE", "ASSIGNED", "UNAVAILABLE"});
        gbc.gridx = 1;
        panel.add(statusBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Availability:"), gbc);
        String[] availOptions = {"YES", "NO"};
        JComboBox<String> availabilityBox = new JComboBox<>(availOptions);
        gbc.gridx = 1;
        panel.add(availabilityBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton submitBtn = createStyledButton("Submit Vehicle to VC");
        submitBtn.addActionListener(e -> {
            String ownerId = ownerIdField.getText().trim();
            String vehicleInfo = vehicleInfoField.getText().trim();
            String residency = residencyField.getText().trim();
            String status = (String) statusBox.getSelectedItem();
            String isAvailable = (String) availabilityBox.getSelectedItem();

            if (ownerId.isEmpty() || vehicleInfo.isEmpty() || residency.isEmpty() || status.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please complete all vehicle fields.");
                return;
            }

            if (!ownerId.matches("\\d+")) {
                JOptionPane.showMessageDialog(frame, "Owner ID must be numeric.");
                return;
            }

            int residencyHours;
            try {
                residencyHours = Integer.parseInt(residency);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Residency hours must be a number.");
                return;
            }

            String entry = String.format(
                "[%s] ROLE:VEHICLE_OWNER | ID:%s | INFO:%s | RESIDENCY:%d | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()), ownerId, vehicleInfo, residencyHours, status, isAvailable
            );

            try {
                refreshMonitor("Connecting to VC Controller server...");
                Socket socket = new Socket("localhost", 9806);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                outputStream.writeUTF(entry);
                outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : "");

                String ack = inputStream.readUTF();
                refreshMonitor("Server response: " + ack + " - Pending approval...");
                try {
                    service.addNotification(service.getCurrentUsername(), "Server ACK received: " + ack + " — vehicle submission pending approval.");
                    refreshNotifications();
                } catch (IOException ignored) {}

                JOptionPane.showMessageDialog(frame, "Vehicle submission has been sent and is pending admin approval.", "Vehicle Submitted", JOptionPane.INFORMATION_MESSAGE);

                ownerIdField.setText("");
                vehicleInfoField.setText("");
                residencyField.setText("");
                statusBox.setSelectedIndex(0);
                availabilityBox.setSelectedIndex(0);

                new Thread(() -> {
                    try {
                        inputStream.readUTF();
                        inputStream.close();
                        outputStream.close();
                        socket.close();
                    } catch (IOException ignored) {}
                }).start();

            } catch (java.net.ConnectException ex) {
                JOptionPane.showMessageDialog(frame, "Cannot connect to VC Controller server.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Connection error: " + ex.getMessage());
            }
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    /** Swaps the main visible container to the requested panel. -DH */
    public void showScreen(JPanel contentPanel) {
        frame.getContentPane().removeAll();
        frame.add(createHeader(), BorderLayout.NORTH);
        if (hasRightSidePanel()) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, createRightSidePanel());
            splitPane.setDividerLocation(400);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            frame.add(splitPane, BorderLayout.CENTER);
        } else {
            frame.add(contentPanel, BorderLayout.CENTER);
        }
        frame.revalidate();
        frame.repaint();
    }
    
    /** Creates the Task Owner Screen layout and form submission logic. -DH */
    private JPanel createTaskOwnerScreen(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Task Owner Portal");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(createWhiteLabel("Task Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        ownerIdField.setText(service.getCurrentUsername() == null ? "" : service.getCurrentUsername());
        ownerIdField.setEditable(false);
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Task ID (Job ID):"), gbc);
        JTextField taskIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(taskIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Description:"), gbc);
        JTextArea taskField = new JTextArea(4, 20);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        JScrollPane taskScroll = new JScrollPane(taskField);
        gbc.gridx = 1;
        panel.add(taskScroll, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Duration (hours):"), gbc);
        Integer[] durationOptions = new Integer[24];
        for (int i = 0; i < durationOptions.length; i++) {
            durationOptions[i] = i + 1;
        }
        JComboBox<Integer> durationField = new JComboBox<>(durationOptions);
        durationField.setSelectedItem(1);
        gbc.gridx = 1;
        panel.add(durationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(createWhiteLabel("Target Vehicle ID:"), gbc);
        JComboBox<VehicleChoice> vehicleField = new JComboBox<>(loadVehicleChoices(service));
        gbc.gridx = 1;
        panel.add(vehicleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(createWhiteLabel("Deadline (YYYY/MM/DD HH:MM:SS):"), gbc);
        JTextField taskDeadlineField = new JTextField();
        gbc.gridx = 1;
        panel.add(taskDeadlineField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        JButton submitBtn = createStyledButton("Submit to VC");
        submitBtn.addActionListener(e -> {
            VehicleChoice selectedVehicle = (VehicleChoice) vehicleField.getSelectedItem();
            Integer selectedDuration = (Integer) durationField.getSelectedItem();
            if (ownerIdField.getText().isBlank() || taskIdField.getText().isBlank() || taskField.getText().trim().isBlank()
                    || selectedDuration == null
                    || selectedVehicle == null || selectedVehicle.getVehicleId().isBlank()
                    || taskDeadlineField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Task Owner fields.");
                return;
            }
            String deadlineText = taskDeadlineField.getText().trim();
            LocalDateTime deadlineTime;
            try {
                deadlineTime = LocalDateTime.parse(deadlineText, dtf);
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(frame, "Deadline must use format YYYY/MM/DD HH:MM:SS.");
                return;
            }
            String entry = String.format("[%s] ROLE:TASK_OWNER | ID:%s | TASK_ID:%s | DESCRIPTION:%s | DURATION:%d | VEHICLE:%s | DEADLINE:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                taskIdField.getText().trim(),
                taskField.getText().trim(),
                selectedDuration,
                selectedVehicle.getVehicleId(),
                deadlineText);
            try {
                Job job = Job.createJob(
                    taskIdField.getText().trim(),
                    taskField.getText().trim(),
                    selectedDuration,
                    LocalDateTime.now(),
                    deadlineTime,
                    selectedVehicle.getVehicleId()
                );
                service.appendJobAndLog(job, entry);
                refreshMonitor("Task Owner submitted to VC:\n" + entry);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Unable to submit Task Owner request.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            }
            sendToVCController(entry, ownerIdField.getText().trim());
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 8;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    /** Creates the Vehicle Owner portal screen for updating vehicle status/availability. -DH */
    private JPanel createVehicleOwnerScreen(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Vehicle Owner Portal");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(createWhiteLabel("Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        ownerIdField.setText(service.getCurrentUsername() == null ? "" : service.getCurrentUsername());
        ownerIdField.setEditable(false);
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Vehicle ID:"), gbc);
        JTextField vehicleIdField = new JTextField();
        gbc.gridx = 1;
        panel.add(vehicleIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Status Update:"), gbc);
        JComboBox<String> statusField = new JComboBox<>(new String[] {"Usable", "In Use", "Maintenance"});
        gbc.gridx = 1;
        panel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Availability:"), gbc);
        JComboBox<String> availabilityField = new JComboBox<>(new String[] {"open", "closed"});
        gbc.gridx = 1;
        panel.add(availabilityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton submitBtn = createStyledButton("Submit to VC");
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || vehicleIdField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Vehicle Owner fields.");
                return;
            }
            String entry = String.format("[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                vehicleIdField.getText().trim(),
                String.valueOf(statusField.getSelectedItem()),
                String.valueOf(availabilityField.getSelectedItem()));
            try {
                service.appendLog(entry);
                refreshMonitor("Vehicle Owner update submitted to VC:\n" + entry);
            } catch (IOException ex) {
                System.err.println("Local log failed, but attempting server send: " + ex.getMessage());
            }
            sendToVCController(entry, ownerIdField.getText().trim());
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    /** Instantiates and returns a uniform Back to Home button. -DH */
    private JButton createBackToHomeButton(CloudDataService service) {
        JButton backBtn = createStyledButton("Back to Home");
        backBtn.addActionListener(e -> showScreen(createHomePanel(service)));
        return backBtn;
    }

    public void keypressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveEntry();
        }
    }

    /** Creates the live monitor/terminal view for the right-hand side panel. -DH */
    private JPanel createMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 25));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        monitorArea = new JTextArea();
        monitorArea.setEditable(false);
        monitorArea.setBackground(Color.BLACK);
        monitorArea.setForeground(Color.GREEN);
        monitorArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(monitorArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY), 
            "Live System Terminal", 
            0, 0, null, Color.CYAN));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // --- LOGIC METHODS ---

    private JLabel createWhiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    /** Unified styled button generator handling padding, colors, and hover listeners. -DH */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBackground(new Color(60, 60, 65));
        button.setForeground(Color.WHITE); 
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 80, 180), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(70, 130, 180));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 60, 65));
            }
        });
        
        return button;
    }

    private JLabel createNotificationBadge(int count) {
        JLabel badge = new JLabel(String.valueOf(count));
        badge.setOpaque(true);
        badge.setBackground(new Color(220, 20, 60));
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setVisible(count > 0);
        return badge;
    }

    private void refreshNotifications() {
        if (!isClientUser() || notificationBadge == null) return;
        try {
            List<String> unread = service.getUnreadNotifications(service.getCurrentUsername());
            int count = unread == null ? 0 : unread.size();
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(count > 0);
        } catch (IOException ignored) {
            notificationBadge.setText("0");
            notificationBadge.setVisible(false);
        }
    }

    private void showClientNotifications() {
        if (!isClientUser()) {
            JOptionPane.showMessageDialog(frame, "Notifications are available for clients only.");
            return;
        }
        try {
            List<String> unread = service.getUnreadNotifications(service.getCurrentUsername());
            if (unread == null || unread.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No unread notifications.");
                notificationBadge.setVisible(false);
                return;
            }
            StringBuilder message = new StringBuilder();
            for (String note : unread) {
                message.append("• ").append(note).append("\n\n");
            }
            JTextArea area = new JTextArea(message.toString());
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font("SansSerif", Font.PLAIN, 13));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(500, 320));
            JOptionPane.showMessageDialog(frame, scroll, "Unread Notifications", JOptionPane.INFORMATION_MESSAGE);
            service.markNotificationsRead(service.getCurrentUsername());
            refreshNotifications();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to load notifications.");
        }
    }

    private void displayUnreadNotificationsIfAny() {
        if (!isClientUser()) return;
        try {
            List<String> unread = service.getUnreadNotifications(service.getCurrentUsername());
            if (unread != null && !unread.isEmpty()) {
                String first = unread.get(0);
                JOptionPane.showMessageDialog(frame, first, "New Notification", JOptionPane.INFORMATION_MESSAGE);
                refreshNotifications();
            }
        } catch (IOException ignored) {}
    }

    private VehicleChoice[] loadVehicleChoices(CloudDataService service) {
        try {
            List<Map<String, String>> vehicles = service.readAllVehicles();
            List<VehicleChoice> choices = new ArrayList<>();
            for (Map<String, String> vehicle : vehicles) {
                String vehicleId = safeValue(vehicle.get("VEHICLE_ID"));
                if (vehicleId.isBlank()) continue;

                String vehicleInfo = safeValue(vehicle.get("VEHICLE_INFO"));
                String label = "Vehicle " + vehicleId;
                if (!vehicleInfo.isBlank() && !"N/A".equals(vehicleInfo)) {
                    label += " - " + vehicleInfo;
                }
                choices.add(new VehicleChoice(vehicleId, label));
            }

            if (choices.isEmpty()) {
                choices.add(new VehicleChoice("", "No vehicles available"));
            }
            return choices.toArray(new VehicleChoice[0]);
        } catch (IOException e) {
            return new VehicleChoice[] { new VehicleChoice("", "No vehicles available") };
        }
    }

    /** Routes the view to the correct residency time overview based on the current user role. -DH */
    private void showResidencyTimeOverview() {
        try {
            if (isAdminUser()) {
                showAdminResidencyOverview();
                return;
            }

            String currentUsername = service.getCurrentUsername();
            if (currentUsername == null || currentUsername.isBlank()) {
                JOptionPane.showMessageDialog(frame, "No active user session found.");
                return;
            }

            if (isClientUser()) {
                showClientResidencyOverview(currentUsername);
                return;
            }

            if (isOwnerUser()) {
                showOwnerResidencyOverview(currentUsername);
                return;
            }

            JOptionPane.showMessageDialog(frame, "No residency view available for this role.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Unable to load residency time overview.");
        }
    }

    private void showAdminResidencyOverview() throws IOException {
        List<JobCompletionRecord> records = controller.calculateCompletionTimes();
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No client jobs found.");
            return;
        }

        String[] columns = {"Job ID", "Description", "Residency Time (hrs)", "Deadline", "Completion Time"};
        List<Object[]> rows = new ArrayList<>();
        for (JobCompletionRecord record : records) {
            rows.add(new Object[] {
                record.getJobId(),
                record.getInfo(),
                record.getResidencyTimeHours(),
                record.getDeadline(),
                record.getCompletionTime()
            });
        }
        showResidencyDialog("Residency Time Overview", columns, rows);
    }

    private void showClientResidencyOverview(String currentUsername) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (Map<String, String> record : service.readClientJobRecords()) {
            if (!currentUsername.equals(record.get("ID"))) {
                continue;
            }
            rows.add(new Object[] {
                safeValue(record.get("ID")),
                safeValue(record.get("INFO")),
                safeValue(record.get("DURATION")),
                safeValue(record.get("DEADLINE")),
                safeValue(record.get("STATUS"))
            });
        }

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No residency records found for your account.");
            return;
        }

        showResidencyDialog(
            "My Residency Time",
            new String[] {"Job ID", "Description", "Residency Time (hrs)", "Deadline", "Status"},
            rows
        );
    }

    private void showOwnerResidencyOverview(String currentUsername) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (String line : service.readAllLogs()) {
            Map<String, String> record = service.parseLogEntry(line);
            String role = normalizeRole(record.get("ROLE"));
            if (!"VEHICLE_OWNER".equals(role)) {
                continue;
            }
            if (!currentUsername.equals(record.get("ID"))) {
                continue;
            }

            rows.add(new Object[] {
                safeValue(record.get("VEHICLE")),
                residencyValue(record),
                safeValue(record.get("DEADLINE")),
                safeValue(record.get("STATUS"))
            });
        }

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No vehicle residency records found for your account.");
            return;
        }

        showResidencyDialog(
            "My Vehicle Residency",
            new String[] {"Vehicle", "Residency Time (hrs)", "Deadline", "Status"},
            rows
        );
    }

    private void showResidencyDialog(String title, String[] columns, List<Object[]> rows) {
        JTable table = new JTable(new javax.swing.table.DefaultTableModel(rows.toArray(new Object[0][]), columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(900, 320));

        JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String residencyValue(Map<String, String> record) {
        String value = record.get("DURATION");
        if (value == null || value.isBlank()) {
            value = record.get("DUR");
        }
        return safeValue(value);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().replace(' ', '_').toUpperCase();
    }

    private static final class VehicleChoice {
        private final String vehicleId;
        private final String label;

        private VehicleChoice(String vehicleId, String label) {
            this.vehicleId = vehicleId;
            this.label = label;
        }

        private String getVehicleId() {
            return vehicleId;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class VehicleSnapshot {
        private final String vehicleId;
        private String status = "N/A";
        private String availability = "N/A";
        private int updateCount = 0;
        private String lastTimestamp = "N/A";

        private VehicleSnapshot(String vehicleId) {
            this.vehicleId = vehicleId;
        }

        private void update(Map<String, String> record) {
            updateCount++;
            status = safeValueStatic(record.get("STATUS"));
            availability = safeValueStatic(record.get("AVAILABILITY"));
            lastTimestamp = safeValueStatic(record.get("TIMESTAMP"));
        }

        private static String safeValueStatic(String value) {
            return value == null || value.isBlank() ? "N/A" : value;
        }
    }

    /** Dynamically adjusts the text entry UI labels depending on the user's selected form role. -DH */
    private void adjustFields() {
        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        if (deadlineLabel != null) deadlineLabel.setVisible(isClient);
        if (deadlineField != null) deadlineField.setVisible(isClient);
        frame.revalidate(); 
    }

    /** Formats data from text inputs into standard strings and passes them to the controller via Socket. -DH */
    private void saveEntry() {
        String role = (String) roleBox.getSelectedItem();
        if ("ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(frame, "Admin does not submit records. Use Calculate Completion Time.");
            return;
        }

        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durField.getText().trim();
        String deadline = deadlineField.isVisible() ? deadlineField.getText().trim() : "N/A";
        int duration;

        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter all required fields.");
            return;
        }

        if (!id.matches("\\d+")) {
            JOptionPane.showMessageDialog(frame, "ID must be numeric (digits only).");
            return;
        }

        try {
            duration = Integer.parseInt(dur);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Duration must be a number.");
            return;
        }
        
        try {
            LocalDateTime arrivalTime = LocalDateTime.now();
            LocalDateTime deadlineTime = deadlineField.isVisible() ? LocalDateTime.parse(deadline, dtf) : null;
            String formattedDeadline = deadlineTime == null ? "N/A" : dtf.format(deadlineTime);
            String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%d | DEADLINE:%s",
                dtf.format(arrivalTime), role, id, info, duration, formattedDeadline);

            refreshMonitor("Connecting to VC Controller server...");

            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            outputStream.writeUTF(entry);
            outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : "");

            String ack = inputStream.readUTF();
            refreshMonitor("Server response: " + ack + " - Pending approval...");
            try {
                service.addNotification(service.getCurrentUsername(), "Server ACK received: " + ack + " — request pending approval.");
                refreshNotifications();
            } catch (IOException ignored) {}

            JOptionPane.showMessageDialog(frame,
                "Your transaction has been submitted and is pending admin approval.\n"
                + "You will be notified when a decision is made.",
                "Transaction Submitted", JOptionPane.INFORMATION_MESSAGE);
            clear();

            new Thread(() -> {
                try {
                    inputStream.readUTF();
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                } catch (IOException ignored) {}
            }).start();

        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame, "Deadline must use format yyyy/MM/dd HH:mm:ss.");
        } catch (java.net.ConnectException e) {
            JOptionPane.showMessageDialog(frame, "Cannot connect to VC Controller server.\nMake sure the server is running first.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection error: " + e.getMessage());
        }
    }

    /** Triggers the controller to calculate First In First Out processing and formats the records. -DH */
    private void calculateCompletionTimes() {
        try {
            List<JobCompletionRecord> records = controller.calculateCompletionTimes();
            if (records.isEmpty()) {
                String msg = "No client jobs found.";
                if (canViewVcrtsLogs()) {
                    refreshMonitor(msg);
                } else {
                    JOptionPane.showMessageDialog(frame, msg);
                }
                return;
            }

            StringBuilder results = new StringBuilder();
            results.append("FIFO Completion Times\n---------------------\n");
            for (JobCompletionRecord record : records) {
                results.append(record.toDisplayString()).append("\n");
            }

            if (canViewVcrtsLogs()) {
                refreshMonitor(results.toString().trim());
            } else {
                JTextArea textArea = new JTextArea(results.toString().trim());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 300));
                JOptionPane.showMessageDialog(frame, scrollPane, "Completion Times", JOptionPane.INFORMATION_MESSAGE);
            }
            refreshMonitor(results.toString().trim());
        } catch (HeadlessException | IOException e) {
            JOptionPane.showMessageDialog(frame, "Error calculating completion times.");
        }
    }

    /** Opens a socket to dispatch log entries directly to the server framework. -DH */
    private void sendToVCController(String entry, String id) {
        try {
            refreshMonitor("Connecting to VC Controller server...");
            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            outputStream.writeUTF(entry);
            outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : id);

            String ack = inputStream.readUTF();
            refreshMonitor("Server response: " + ack + " - Pending approval...");
            try {
                service.addNotification(service.getCurrentUsername(), "Server ACK received: " + ack + " — submission pending approval.");
                refreshNotifications();
            } catch (IOException ignored) {}

            JOptionPane.showMessageDialog(frame,
                "Submission sent! Pending admin approval.\nYou will be notified of the decision.",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            new Thread(() -> {
                try {
                    String finalDecision = inputStream.readUTF();
                    refreshMonitor("Final Admin Decision: " + finalDecision);
                    socket.close();
                } catch (IOException ignored) {}
            }).start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Server Error: Make sure ServerMain is running.");
        }
    }

    // User Validation Functions
    private boolean isAdminUser() { return "ADMIN".equals(currentUserRole); }
    private boolean isOwnerUser() { return "OWNER".equals(currentUserRole); }
    private boolean isClientUser() { return "CLIENT".equals(currentUserRole); }
    private boolean hasRightSidePanel() { return isOwnerUser() || isClientUser() || isAdminUser(); }
    private boolean canViewVcrtsLogs() { return isOwnerUser() || isAdminUser(); }

    private void refreshMonitor(String resultSection) {
        if (monitorArea == null) return;
        try {
            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                display.append(line).append("\n");
            }

            if (resultSection != null && !resultSection.isBlank()) {
                display.append("\n\n").append(resultSection);
            }

            monitorArea.setText(display.toString());
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength()); 
            refreshOwnerVehiclePanel();
            refreshClientJobsPanel();
            refreshClientActivityPanel();
        } catch (IOException ignored) {}
    }

    private JComponent createRightSidePanel() {
        if (isClientUser()) {
            JPanel clientPanel = createClientJobsPanel();
            JPanel activityPanel = createClientActivityPanel();
            refreshClientJobsPanel();
            refreshClientActivityPanel();
            JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                clientPanel,
                activityPanel
            );
            splitPane.setDividerLocation(220);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            return splitPane;
        }

        if (!isOwnerUser()) {
            return createMonitorPanel();
        }

        JPanel ownerPanel = createOwnerVehiclePanel();
        refreshOwnerVehiclePanel();
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            ownerPanel,
            createMonitorPanel()
        );
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        return splitPane;
    }

    private JPanel createOwnerVehiclePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columns = {"Vehicle", "Activity", "Availability", "Updates", "Last Update"};
        ownerVehicleModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ownerVehicleTable = new JTable(ownerVehicleModel);
        ownerVehicleTable.setFillsViewportHeight(true);
        ownerVehicleTable.setRowHeight(26);
        ownerVehicleTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(ownerVehicleTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "Vehicle Panel",
            0, 0, null, Color.CYAN));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createClientJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columns = {"Job ID", "Description", "Duration", "Deadline", "Status"};
        clientJobModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clientJobTable = new JTable(clientJobModel);
        clientJobTable.setFillsViewportHeight(true);
        clientJobTable.setRowHeight(26);
        clientJobTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(clientJobTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "My Jobs",
            0, 0, null, Color.CYAN));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createClientActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        clientActivityArea = new JTextArea();
        clientActivityArea.setEditable(false);
        clientActivityArea.setBackground(Color.BLACK);
        clientActivityArea.setForeground(Color.GREEN);
        clientActivityArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(clientActivityArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            "My Activity",
            0, 0, null, Color.CYAN));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void refreshOwnerVehiclePanel() {
        if (ownerVehicleModel == null || !isOwnerUser()) {
            return;
        }

        try {
            String currentUsername = service.getCurrentUsername();
            if (currentUsername == null || currentUsername.isBlank()) {
                ownerVehicleModel.setRowCount(0);
                return;
            }

            Map<String, VehicleSnapshot> vehicles = new LinkedHashMap<>();
            for (String line : service.readAllLogs()) {
                Map<String, String> record = service.parseLogEntry(line);
                if (!"VEHICLE_OWNER".equals(normalizeRole(record.get("ROLE")))) {
                    continue;
                }
                if (!currentUsername.equals(record.get("ID"))) {
                    continue;
                }

                String vehicleId = safeValue(record.get("VEHICLE"));
                VehicleSnapshot snapshot = vehicles.get(vehicleId);
                if (snapshot == null) {
                    snapshot = new VehicleSnapshot(vehicleId);
                    vehicles.put(vehicleId, snapshot);
                }
                snapshot.update(record);
            }

            ownerVehicleModel.setRowCount(0);
            for (VehicleSnapshot vehicle : vehicles.values()) {
                ownerVehicleModel.addRow(new Object[] {
                    vehicle.vehicleId,
                    vehicle.status,
                    vehicle.availability,
                    vehicle.updateCount,
                    vehicle.lastTimestamp
                });
            }
        } catch (IOException ignored) {
            if (ownerVehicleModel != null) {
                ownerVehicleModel.setRowCount(0);
            }
        }
    }

    private void refreshClientJobsPanel() {
        if (clientJobModel == null || !isClientUser()) {
            return;
        }

        try {
            String currentUsername = service.getCurrentUsername();
            if (currentUsername == null || currentUsername.isBlank()) {
                clientJobModel.setRowCount(0);
                return;
            }

            clientJobModel.setRowCount(0);
            for (Map<String, String> record : service.readClientJobRecords()) {
                if (!currentUsername.equals(record.get("ID"))) {
                    continue;
                }
                clientJobModel.addRow(new Object[] {
                    safeValue(record.get("ID")),
                    safeValue(record.get("INFO")),
                    safeValue(record.get("DURATION")),
                    safeValue(record.get("DEADLINE")),
                    safeValue(record.get("STATUS"))
                });
            }
        } catch (IOException ignored) {
            if (clientJobModel != null) {
                clientJobModel.setRowCount(0);
            }
        }
    }

    private void refreshClientActivityPanel() {
        if (clientActivityArea == null || !isClientUser()) {
            return;
        }

        try {
            String currentUsername = service.getCurrentUsername();
            if (currentUsername == null || currentUsername.isBlank()) {
                clientActivityArea.setText("No active client session.");
                return;
            }

            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                Map<String, String> record = service.parseLogEntry(line);
                if (!"CLIENT".equals(normalizeRole(record.get("ROLE")))) {
                    continue;
                }
                if (!currentUsername.equals(record.get("ID"))) {
                    continue;
                }
                display.append(line).append("\n");
            }

            if (display.length() == 0) {
                clientActivityArea.setText("No recent activity found.");
                return;
            }

            clientActivityArea.setText(display.toString().trim());
            clientActivityArea.setCaretPosition(clientActivityArea.getDocument().getLength());
        } catch (IOException ignored) {
            clientActivityArea.setText("Unable to load client activity.");
        }
    }

    private void startAdminRefreshTimer() {
        stopAdminRefreshTimer();
        adminRefreshTimer = new Timer(1000, e -> refreshPendingAdminRequest());
        adminRefreshTimer.start();
    }

    private void stopAdminRefreshTimer() {
        if (adminRefreshTimer != null) {
            adminRefreshTimer.stop();
            adminRefreshTimer = null;
        }
    }

    private void showSelectedPendingRequestDetails() {
        if (pendingRequestsModel == null || pendingRequestsTable == null) return;
        int row = pendingRequestsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a row in the table first.");
            return;
        }
        String id = String.valueOf(pendingRequestsModel.getValueAt(row, 0));
        String submitter = String.valueOf(pendingRequestsModel.getValueAt(row, 1));
        String role = String.valueOf(pendingRequestsModel.getValueAt(row, 2));
        String entry = String.valueOf(pendingRequestsModel.getValueAt(row, 3));
        String full = "Request ID: " + id + "\nSubmitter: " + submitter + "\nRole: " + role + "\n\nFull entry:\n" + entry;
        JTextArea area = new JTextArea(full);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(720, 480));
        JOptionPane.showMessageDialog(frame, sp, "Request details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshPendingAdminRequest() {
        if (pendingRequestsModel == null || pendingRequestsTable == null || adminRequestStatusLabel == null) return;

        try {
            List<Map<String, String>> requests = service.readAllPendingRequests();
            
            int selectedRow = pendingRequestsTable.getSelectedRow();
            String selectedId = selectedRow >= 0 ? (String) pendingRequestsModel.getValueAt(selectedRow, 0) : null;

            pendingRequestsModel.setRowCount(0);

            if (requests.isEmpty()) {
                adminRequestStatusLabel.setText("Waiting for client requests...");
                return;
            }

            adminRequestStatusLabel.setText("Pending client requests: " + requests.size());

            int newSelectedIndex = -1;
            for (int i = 0; i < requests.size(); i++) {
                Map<String, String> req = requests.get(i);
                String id = req.get("REQUEST_ID");
                String submitter = req.get("SUBMITTER");
                String entry = req.get("ENTRY");
                
                Map<String, String> parsed = service.parseLogEntry(entry);
                String role = parsed.getOrDefault("ROLE", "UNKNOWN");
                
                pendingRequestsModel.addRow(new Object[]{id, submitter, role, entry});
                
                if (id != null && id.equals(selectedId)) {
                    newSelectedIndex = i;
                }
            }
            
            if (newSelectedIndex >= 0) {
                pendingRequestsTable.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
            }
            
        } catch (IOException e) {
            adminRequestStatusLabel.setText("Unable to load pending requests");
        }
    }

    private void submitAdminDecision(String decision) {
        int selectedRow = pendingRequestsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(frame, "Please select a request from the table first.");
            return;
        }

        String requestId = (String) pendingRequestsModel.getValueAt(selectedRow, 0);
        String submitter = (String) pendingRequestsModel.getValueAt(selectedRow, 1);
        String entry = (String) pendingRequestsModel.getValueAt(selectedRow, 3);

        try {
            service.writeAdminDecision(requestId, decision);

            if (submitter != null && !submitter.isBlank()) {
                String notifMsg = "Your job request was " + decision + ":\n" + entry;
                service.addNotification(submitter, notifMsg);
            }

            adminRequestStatusLabel.setText("Last response sent: " + decision);
            refreshMonitor("Admin decision sent for request:\n" + entry + "\nSTATUS: " + decision);
            
            refreshPendingAdminRequest(); 
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to send admin decision.");
        }
    }

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }
}