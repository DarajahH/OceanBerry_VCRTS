package views;

import app.VcrtsTheme;
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
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableModel;
import models.job.Job;
import services.CloudDataService;
import services.VCController;
import services.VCController.JobCompletionRecord;


public class VCRTSDashboard {
    private static final String HOME_SCREEN = "HOME_SCREEN";
    private static final String FORM_SCREEN = "FORM_SCREEN";
    private static final String ADMIN_SCREEN = "ADMIN_SCREEN";
    private static final String TASK_OWNER_SCREEN = "TASK_OWNER_SCREEN";
    private static final String VEHICLE_OWNER_SCREEN = "VEHICLE_OWNER_SCREEN";
    private static final int FORM_RAIL_WIDTH = 760;

    private static final Color APP_BG = VcrtsTheme.CANVAS;
    private static final Color SHELL_BG = VcrtsTheme.SHELL;
    private static final Color SIDEBAR_BG = VcrtsTheme.SIDEBAR;
    private static final Color SURFACE_BG = VcrtsTheme.SURFACE;
    private static final Color SURFACE_BG_ALT = VcrtsTheme.SURFACE_ELEVATED;
    private static final Color FIELD_BG = VcrtsTheme.FIELD;
    private static final Color TERMINAL_BG = VcrtsTheme.LOG;
    private static final Color ACCENT = VcrtsTheme.ACCENT;
    private static final Color ACCENT_ACTIVE = VcrtsTheme.ACCENT_ACTIVE;
    private static final Color ACCENT_GHOST = VcrtsTheme.ACCENT_GHOST;
    private static final Color ACCENT_SOFT = VcrtsTheme.BORDER;
    private static final Color TEXT_PRIMARY = VcrtsTheme.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = VcrtsTheme.TEXT_SECONDARY;
    private static final Color TEXT_MUTED = VcrtsTheme.TEXT_MUTED;
    private static final Color SUCCESS = VcrtsTheme.SUCCESS;
    private static final Color WARNING = VcrtsTheme.WARNING;
    private static final Color DANGER = VcrtsTheme.DANGER;
    private static final Font TITLE_FONT = VcrtsTheme.TITLE_FONT;
    private static final Font SECTION_FONT = VcrtsTheme.SECTION_FONT;
    private static final Font BODY_FONT = VcrtsTheme.BODY_FONT;
    private static final Font LABEL_FONT = VcrtsTheme.LABEL_FONT;


    private final JFrame frame;
    private final CloudDataService service;
    private final VCController controller;
    private final String currentUserRole;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final CardLayout cardLayout;
    private final JPanel leftCardContainer;
    private final JPanel appShell;
    private final JPanel feedbackBar;
    private final JLabel feedbackLabel;
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private JLabel shellContextLabel;
    private Timer feedbackTimer;
    private String currentScreen = HOME_SCREEN;

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
    private Timer clientNotificationTimer;

    public VCRTSDashboard(CloudDataService service, String currentUserRole) {
       this.service = service;
        this.controller = new VCController(service);
        this.currentUserRole = currentUserRole == null ? "CLIENT" : currentUserRole.toUpperCase();

        // 1. Setup Main Frame
        frame = new JFrame("VCRTS - Cloud Control Center");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(APP_BG);
        cardLayout = new CardLayout();
        leftCardContainer = new JPanel(cardLayout);
        leftCardContainer.setOpaque(false);

        leftCardContainer.add(createHomePanel(service), HOME_SCREEN);
        leftCardContainer.add(createCombinedClientPanel(service), FORM_SCREEN);
        if (isAdminUser()) {
            leftCardContainer.add(createAdminScreen(service), ADMIN_SCREEN);
        }
        leftCardContainer.add(createTaskOwnerScreen(service), TASK_OWNER_SCREEN);
        leftCardContainer.add(createVehicleOwnerScreen(service), VEHICLE_OWNER_SCREEN);
        leftCardContainer.setBackground(SHELL_BG);

        feedbackLabel = new JLabel();
        feedbackLabel.setFont(BODY_FONT);
        feedbackLabel.setForeground(TEXT_PRIMARY);
        feedbackBar = new JPanel(new BorderLayout());
        feedbackBar.setVisible(false);

        appShell = new JPanel(new BorderLayout());
        appShell.setBackground(APP_BG);
        appShell.add(createHeader(), BorderLayout.NORTH);
        appShell.add(createShellBody(), BorderLayout.CENTER);
        frame.add(appShell, BorderLayout.CENTER);

        adjustFields();
        showScreen(HOME_SCREEN);
        refreshMonitor(null);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (isClientUser()) {
            refreshNotifications();
            displayUnreadNotificationsIfAny();
            startClientNotificationTimer();
        }
    }
    
    // --- PANEL CREATION METHODS ---

    /*
    The Home Panel is a simple welcome screen with buttons to navigate to the submission form and to calculate completion times. 
    It serves as the landing page after login, providing a clear starting point for users. 
    The design is clean and minimalistic, with a focus on usability and quick access to key features while we work further through development.
    -DH
    */

    /**
     * @param service
     * @return
     */
    public JPanel createHomePanel(CloudDataService service) {//DH
        JButton residencyButton = new JButton("View Residency Time");
        styleSecondaryButton(residencyButton);
        residencyButton.addActionListener(e -> showResidencyTimeOverview());

        JButton clientButton = new JButton("Submit New Transaction");
        stylePrimaryButton(clientButton);
        clientButton.setEnabled(isClientUser());
        if (isClientUser()) {
            clientButton.addActionListener(e -> showScreen(FORM_SCREEN));
        }

        JButton taskOwnerButton = new JButton("Task Owner Portal");
        styleSecondaryButton(taskOwnerButton);
        taskOwnerButton.setEnabled(isClientUser());
        if (isClientUser()) {
            taskOwnerButton.addActionListener(e -> showScreen(TASK_OWNER_SCREEN));
        }

        JButton adminButton = new JButton("Open Admin Review");
        stylePrimaryButton(adminButton);
        adminButton.setEnabled(isAdminUser());
        if (isAdminUser()) {
            adminButton.addActionListener(e -> showScreen(ADMIN_SCREEN));
        }

        JButton completionButton = new JButton("Calculate Completion Times");
        styleSecondaryButton(completionButton);
        completionButton.setEnabled(isAdminUser());
        if (isAdminUser()) {
            completionButton.addActionListener(e -> calculateCompletionTimes());
        }

        JButton ownerButton = new JButton("Vehicle Owner Portal");
        stylePrimaryButton(ownerButton);
        ownerButton.setEnabled(isOwnerUser());
        if (isOwnerUser()) {
            ownerButton.addActionListener(e -> showScreen(VEHICLE_OWNER_SCREEN));
        }

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10));
        grid.setOpaque(false);
        grid.add(createLaunchCard("Residency", null, createButtonStack(residencyButton)));
        if (isClientUser()) {
            grid.add(createLaunchCard("Client", null, createButtonStack(clientButton)));
            grid.add(createLaunchCard("Task Owner", null, createButtonStack(taskOwnerButton)));
        }
        if (isAdminUser()) {
            grid.add(createLaunchCard("Admin", null, createButtonStack(adminButton)));
            grid.add(createLaunchCard("Completion", null, createButtonStack(completionButton)));
        }
        if (isOwnerUser()) {
            grid.add(createLaunchCard("Owner", null, createButtonStack(ownerButton)));
        }

        JPanel profileCard = createMetaCard(
            "Session",
            service.getCurrentUsername() == null ? "Active role" : service.getCurrentUsername(),
            currentUserRole
        );

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(grid, BorderLayout.CENTER);
        body.add(profileCard, BorderLayout.SOUTH);

        return createDashboardPage(
            "",
            "Dashboard",
            "",
            body,
            null
        );
    }

    public JPanel createAdminScreen(CloudDataService service) { //DH
        // Create the structured table model
        String[] columns = {"Request ID", "Submitter", "Role", "Details"};
        pendingRequestsModel = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } // Read-only
        };
        
        pendingRequestsTable = new JTable(pendingRequestsModel);
        pendingRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(pendingRequestsTable);
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
        pendingScrollPane.setPreferredSize(new Dimension(0, 300));
        styleScrollPane(pendingScrollPane, null);

        adminRequestStatusLabel = new JLabel("Waiting for client request...");
        adminRequestStatusLabel.setForeground(TEXT_MUTED);
        adminRequestStatusLabel.setFont(BODY_FONT);

        JButton refreshBtn = new JButton("Refresh");
        styleSecondaryButton(refreshBtn);
        refreshBtn.addActionListener(e -> refreshPendingAdminRequest());
        JButton viewDetailsBtn = new JButton("View full details");
        styleSecondaryButton(viewDetailsBtn);
        viewDetailsBtn.addActionListener(e -> showSelectedPendingRequestDetails());

        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        stylePrimaryButton(btnCalcTimes);
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());

        JButton acceptBtn = new JButton("Accept");
        styleSuccessButton(acceptBtn);
        acceptBtn.addActionListener(e -> submitAdminDecision("ACCEPTED"));

        JButton rejectBtn = new JButton("Reject");
        styleDangerButton(rejectBtn);
        rejectBtn.addActionListener(e -> submitAdminDecision("REJECTED"));

        JPanel statusContent = new JPanel();
        statusContent.setOpaque(false);
        statusContent.setLayout(new BoxLayout(statusContent, BoxLayout.Y_AXIS));

        JPanel statusBadge = createStatusBadge("STATUS", adminRequestStatusLabel);
        statusBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusContent.add(statusBadge);
        statusContent.add(Box.createVerticalStrut(14));

        JPanel controls = createButtonStack(
            refreshBtn,
            viewDetailsBtn,
            btnCalcTimes,
            acceptBtn,
            rejectBtn
        );
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusContent.add(controls);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.add(createSurfaceCard(
            "Pending Requests",
            "",
            pendingScrollPane
        ), BorderLayout.CENTER);
        JPanel actionCard = createSurfaceCard(
            "Actions",
            "",
            statusContent
        );
        actionCard.setPreferredSize(new Dimension(280, 0));
        body.add(actionCard, BorderLayout.EAST);

        startAdminRefreshTimer();
        refreshPendingAdminRequest();

        return createDashboardPage(
            "",
            "Admin",
            "",
            body,
            null
        );
    }


    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SHELL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_SOFT),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("VCRTS");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Dialog", Font.BOLD, 17));
        brandPanel.add(title);

        shellContextLabel = new JLabel("Dashboard");
        shellContextLabel.setForeground(TEXT_MUTED);
        shellContextLabel.setFont(VcrtsTheme.META_FONT);
        brandPanel.add(shellContextLabel);

        header.add(brandPanel, BorderLayout.WEST);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(createHeaderBadge(currentUserRole));

        if (isClientUser()) {
            JButton notificationsBtn = new JButton("Notifications");
            styleSecondaryButton(notificationsBtn);
            notificationsBtn.setPreferredSize(new Dimension(110, 32));
            notificationsBtn.addActionListener(e -> showClientNotifications());
            rightHeader.add(notificationsBtn);

            notificationBadge = createNotificationBadge(0);
            rightHeader.add(notificationBadge);
        }

        JButton logoutBtn = new JButton("Logout");
        styleSecondaryButton(logoutBtn);
        logoutBtn.addActionListener(e -> {
            stopAdminRefreshTimer();
            stopClientNotificationTimer();
            frame.dispose();
            new LoginScreen(service);
        });
        rightHeader.add(logoutBtn);
        header.add(rightHeader, BorderLayout.EAST);

        refreshNotifications();

        return header;
    }

    private JPanel createShellBody() {
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(APP_BG);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        body.add(createSidebar(), BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout(0, 8));
        workspace.setOpaque(false);
        workspace.add(createFeedbackBar(), BorderLayout.NORTH);
        workspace.add(hasRightSidePanel() ? createWorkspaceLayout(leftCardContainer) : leftCardContainer, BorderLayout.CENTER);
        body.add(workspace, BorderLayout.CENTER);
        return body;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(12, 12, 12, 12)
        ));
        sidebar.setPreferredSize(new Dimension(196, 0));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("WORKSPACE");
        label.setForeground(TEXT_MUTED);
        label.setFont(VcrtsTheme.META_FONT);
        top.add(label);
        top.add(Box.createVerticalStrut(8));

        top.add(createSidebarButton(HOME_SCREEN, "Dashboard"));
        if (isClientUser()) {
            top.add(Box.createVerticalStrut(6));
            top.add(createSidebarButton(FORM_SCREEN, "Client"));
            top.add(Box.createVerticalStrut(6));
            top.add(createSidebarButton(TASK_OWNER_SCREEN, "Task Owner"));
        }
        if (isOwnerUser()) {
            top.add(Box.createVerticalStrut(6));
            top.add(createSidebarButton(VEHICLE_OWNER_SCREEN, "Owner"));
        }
        if (isAdminUser()) {
            top.add(Box.createVerticalStrut(6));
            top.add(createSidebarButton(ADMIN_SCREEN, "Admin"));
        }

        sidebar.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));

        JLabel utilities = new JLabel("UTILITIES");
        utilities.setForeground(TEXT_MUTED);
        utilities.setFont(VcrtsTheme.META_FONT);
        middle.add(utilities);
        middle.add(Box.createVerticalStrut(8));

        JButton residencyButton = new JButton("Residency");
        styleSecondaryButton(residencyButton);
        residencyButton.addActionListener(e -> showResidencyTimeOverview());
        middle.add(residencyButton);

        if (canViewVcrtsLogs()) {
            middle.add(Box.createVerticalStrut(8));
            JButton logsButton = new JButton("Logs");
            styleSecondaryButton(logsButton);
            logsButton.addActionListener(e -> showSystemLogDialog());
            middle.add(logsButton);
        }

        if (isAdminUser()) {
            middle.add(Box.createVerticalStrut(8));
            JButton completionButton = new JButton("Completion");
            styleSecondaryButton(completionButton);
            completionButton.addActionListener(e -> calculateCompletionTimes());
            middle.add(completionButton);
        }

        sidebar.add(middle, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel(service.getCurrentUsername() == null ? "Session" : service.getCurrentUsername());
        userLabel.setForeground(TEXT_PRIMARY);
        userLabel.setFont(LABEL_FONT);
        footer.add(userLabel);
        footer.add(Box.createVerticalStrut(4));

        JLabel roleLabel = new JLabel(currentUserRole);
        roleLabel.setForeground(TEXT_MUTED);
        roleLabel.setFont(VcrtsTheme.META_FONT);
        footer.add(roleLabel);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createFeedbackBar() {
        feedbackBar.setOpaque(true);
        feedbackBar.setBackground(SURFACE_BG_ALT);
        feedbackBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(6, 10, 6, 10)
        ));
        feedbackBar.add(feedbackLabel, BorderLayout.CENTER);
        return feedbackBar;
    }

    private JButton createSidebarButton(String key, String label) {
        JButton button = new JButton(label);
        styleNavButton(button);
        button.addActionListener(e -> showScreen(key));
        navButtons.put(key, button);
        return button;
    }

    private void updateNavigationState() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton button = entry.getValue();
            boolean active = entry.getKey().equals(currentScreen);
            button.setBackground(active ? ACCENT_GHOST : SIDEBAR_BG);
            button.setForeground(active ? TEXT_PRIMARY : TEXT_SECONDARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? ACCENT : ACCENT_SOFT),
                new EmptyBorder(10, 12, 10, 12)
            ));
        }
    }

    private void updateHeaderContext() {
        if (shellContextLabel != null) {
            shellContextLabel.setText(screenTitle(currentScreen));
        }
    }

// 1. The Main Tabbed Container
    private JPanel createCombinedClientPanel(CloudDataService service) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setFont(new Font("Dialog", Font.BOLD, 13));
        tabbedPane.setBackground(SURFACE_BG);
        tabbedPane.setForeground(TEXT_PRIMARY);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Job Request", createJobSubmissionTab());
        tabbedPane.addTab("Vehicle Registration", createVehicleSubmissionTab(service));

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 18));
        body.setOpaque(false);
        body.add(createSurfaceCard(
            "Submission",
            "",
            tabbedPane
        ));

        return createDashboardPage(
            "",
            "Client",
            "",
            body,
            null
        );
    }

    // 2. The Job Submission Form (Tab 1)
    private JPanel createJobSubmissionTab() {
        JPanel formPanel = createFormStack();

        roleBox = new JComboBox<>(new String[]{"CLIENT"});
        roleBox.setEnabled(false);
        roleBox.addActionListener(e -> adjustFields());
        roleBox.setVisible(false);

        idLabel = createWhiteLabel("Job");
        idField = new JTextField();
        styleTextField(idField);

        infoLabel = createWhiteLabel("Description");
        infoField = new JTextField();
        styleTextField(infoField);

        durLabel = createWhiteLabel("Hours");
        durField = new JTextField();
        styleTextField(durField);

        deadlineLabel = createWhiteLabel("Deadline");
        deadlineField = new JTextField();
        deadlineField.setToolTipText("YYYY/MM/DD HH:MM:SS");
        styleTextField(deadlineField);
        formPanel.add(createInlineFormRow(
            createFormFieldRow(idLabel, idField),
            createFormFieldRow(durLabel, durField)
        ));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createFormFieldRow(infoLabel, infoField));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createFormFieldRow(deadlineLabel, deadlineField));
        formPanel.add(Box.createVerticalStrut(16));

        JButton submitBtn = new JButton("Submit Transaction");
        stylePrimaryButton(submitBtn);
        submitBtn.addActionListener(e -> saveEntry());
        formPanel.add(createFullWidthButton(submitBtn));

        return wrapFormPanel(formPanel);
    }

    // 3. The Vehicle Registration Form (Tab 2)
    private JPanel createVehicleSubmissionTab(CloudDataService service) {
        JPanel formPanel = createFormStack();

        JLabel ownerLabel = createWhiteLabel("Owner");
        JTextField ownerIdField = new JTextField();
        styleTextField(ownerIdField);

        JLabel vehicleLabel = createWhiteLabel("Vehicle");
        JTextField vehicleInfoField = new JTextField();
        styleTextField(vehicleInfoField);

        JLabel residencyLabel = createWhiteLabel("Residency Hours");
        JTextField residencyField = new JTextField();
        styleTextField(residencyField);

        JLabel statusLabel = createWhiteLabel("Status");
        JComboBox<String> statusBox = new JComboBox<>(
        new String[]{"IDLE", "IN_SERVICE", "ASSIGNED", "UNAVAILABLE"});
        styleComboBox(statusBox);

        JLabel availabilityLabel = createWhiteLabel("Availability");
        String[] availOptions = {"YES", "NO"};
        JComboBox<String> availabilityBox = new JComboBox<>(availOptions);
        styleComboBox(availabilityBox);
        formPanel.add(createInlineFormRow(
            createFormFieldRow(ownerLabel, ownerIdField),
            createFormFieldRow(residencyLabel, residencyField)
        ));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createFormFieldRow(vehicleLabel, vehicleInfoField));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInlineFormRow(
            createFormFieldRow(statusLabel, statusBox),
            createFormFieldRow(availabilityLabel, availabilityBox)
        ));
        formPanel.add(Box.createVerticalStrut(16));

        JButton submitBtn = new JButton("Submit Vehicle to VC");
        stylePrimaryButton(submitBtn);
        submitBtn.addActionListener(e -> {
            String ownerId = ownerIdField.getText().trim();
            String vehicleInfo = vehicleInfoField.getText().trim();
            String residency = residencyField.getText().trim();
            String status = (String) statusBox.getSelectedItem();
            String isAvailable = (String) availabilityBox.getSelectedItem();

            if (ownerId.isEmpty() || vehicleInfo.isEmpty() || residency.isEmpty() || status.isEmpty()) {
                showFeedback("Please complete all vehicle fields.", DANGER, 4200);
                return;
            }

            if (!ownerId.matches("\\d+")) {
                showFeedback("Owner ID must be numeric.", WARNING, 4200);
                return;
            }

            int residencyHours;
            try {
                residencyHours = Integer.parseInt(residency);
            } catch (NumberFormatException ex) {
                showFeedback("Residency hours must be a number.", WARNING, 4200);
                return;
            }

            String entry = String.format(
                "[%s] ROLE:VEHICLE_OWNER | ID:%s | INFO:%s | RESIDENCY:%d | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerId,
                vehicleInfo,
                residencyHours,
                status,
                isAvailable
            );

            try {
                refreshMonitor("Connecting to VC Controller server...");

                Socket socket = new Socket("localhost", 9806);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                outputStream.writeUTF(entry);
                outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : "");

                String ack = inputStream.readUTF();
                String requestId = inputStream.readUTF();
                refreshMonitor("Server response: " + ack + " (Request ID: " + requestId + ") - Pending approval...");
                try {
                    service.addNotification(service.getCurrentUsername(), "Server ACK received (Request ID: " + requestId + ") — vehicle submission pending approval.");
                    refreshNotifications();
                } catch (IOException ignored) {}

                showFeedback("Vehicle submission sent for admin review.", SUCCESS, 4200);

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
                showFeedback("Cannot connect to VC Controller server.", DANGER, 5000);
            } catch (IOException ex) {
                showFeedback("Connection error: " + ex.getMessage(), DANGER, 5000);
            }
        });
        formPanel.add(createFullWidthButton(submitBtn));

        return wrapFormPanel(formPanel);
    }


//Show Screen method calls Panels - DH

    public void showScreen(String screenId) {//DH
        currentScreen = screenId;
        cardLayout.show(leftCardContainer, screenId);
        updateNavigationState();
        updateHeaderContext();
        if (isAdminUser() && ADMIN_SCREEN.equals(screenId)) {
            startAdminRefreshTimer();
        } else {
            stopAdminRefreshTimer();
        }
        refreshMonitor(null);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createTaskOwnerScreen(CloudDataService service) {//DH
        JPanel formPanel = createFormStack();

        JLabel ownerLabel = createWhiteLabel("Task Owner");
        JTextField ownerIdField = new JTextField();
        ownerIdField.setText(service.getCurrentUsername() == null ? "" : service.getCurrentUsername());
        ownerIdField.setEditable(false);
        styleTextField(ownerIdField);

        JLabel taskLabel = createWhiteLabel("Task");
        JTextField taskIdField = new JTextField();
        styleTextField(taskIdField);

        JLabel descriptionLabel = createWhiteLabel("Description");
        JTextArea taskField = new JTextArea(3, 20);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        styleTextArea(taskField);
        JScrollPane taskScroll = new JScrollPane(taskField);
        styleScrollPane(taskScroll, null);
        taskScroll.setPreferredSize(new Dimension(0, 96));

        JLabel hoursLabel = createWhiteLabel("Hours");
        Integer[] durationOptions = new Integer[24];
        for (int i = 0; i < durationOptions.length; i++) {
            durationOptions[i] = i + 1;
        }
        JComboBox<Integer> durationField = new JComboBox<>(durationOptions);
        durationField.setSelectedItem(1);
        styleComboBox(durationField);

        JLabel targetVehicleLabel = createWhiteLabel("Target Vehicle");
        JComboBox<VehicleChoice> vehicleField = new JComboBox<>(loadVehicleChoices(service));
        styleComboBox(vehicleField);

        JLabel deadlineTextLabel = createWhiteLabel("Deadline");
        JTextField taskDeadlineField = new JTextField();
        taskDeadlineField.setToolTipText("YYYY/MM/DD HH:MM:SS");
        styleTextField(taskDeadlineField);
        formPanel.add(createInlineFormRow(
            createFormFieldRow(ownerLabel, ownerIdField),
            createFormFieldRow(hoursLabel, durationField)
        ));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createFormFieldRow(taskLabel, taskIdField));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createFormFieldRow(descriptionLabel, taskScroll));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInlineFormRow(
            createFormFieldRow(targetVehicleLabel, vehicleField),
            createFormFieldRow(deadlineTextLabel, taskDeadlineField)
        ));
        formPanel.add(Box.createVerticalStrut(16));

        JButton submitBtn = new JButton("Submit to VC");
        stylePrimaryButton(submitBtn);
        submitBtn.addActionListener(e -> {
            VehicleChoice selectedVehicle = (VehicleChoice) vehicleField.getSelectedItem();
            Integer selectedDuration = (Integer) durationField.getSelectedItem();
            if (ownerIdField.getText().isBlank() || taskIdField.getText().isBlank() || taskField.getText().trim().isBlank()
                    || selectedDuration == null
                    || selectedVehicle == null || selectedVehicle.getVehicleId().isBlank()
                    || taskDeadlineField.getText().isBlank()) {
                showFeedback("Please complete all task owner fields.", DANGER, 4200);
                return;
            }
            String deadlineText = taskDeadlineField.getText().trim();
            LocalDateTime deadlineTime;
            try {
                deadlineTime = LocalDateTime.parse(deadlineText, dtf);
            } catch (java.time.format.DateTimeParseException ex) {
                showFeedback("Deadline must use format YYYY/MM/DD HH:MM:SS.", WARNING, 4200);
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
            refreshMonitor("Task Owner submission queued for admin review:\n" + entry);
            sendToVCController(entry, ownerIdField.getText().trim());
        });
        formPanel.add(createFullWidthButton(submitBtn));

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 18));
        body.setOpaque(false);
        body.add(createSurfaceCard(
            "Assignment",
            "",
            wrapFormPanel(formPanel)
        ));

        return createDashboardPage(
            "",
            "Task Owner",
            "",
            body,
            null
        );
    }

    private JPanel createVehicleOwnerScreen(CloudDataService service) {//DH
        JPanel formPanel = createFormStack();

        JLabel ownerLabel = createWhiteLabel("Owner");
        JTextField ownerIdField = new JTextField();
        ownerIdField.setText(service.getCurrentUsername() == null ? "" : service.getCurrentUsername());
        ownerIdField.setEditable(false);
        styleTextField(ownerIdField);

        JLabel vehicleLabel = createWhiteLabel("Vehicle");
        JTextField vehicleIdField = new JTextField();
        styleTextField(vehicleIdField);

        JLabel statusLabel = createWhiteLabel("Status");
        JComboBox<String> statusField = new JComboBox<>(new String[] {"Usable", "In Use", "Maintenance"});
        styleComboBox(statusField);

        JLabel availabilityLabel = createWhiteLabel("Availability");
        JComboBox<String> availabilityField = new JComboBox<>(new String[] {"open", "closed"});
        styleComboBox(availabilityField);
        formPanel.add(createInlineFormRow(
            createFormFieldRow(ownerLabel, ownerIdField),
            createFormFieldRow(vehicleLabel, vehicleIdField)
        ));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(createInlineFormRow(
            createFormFieldRow(statusLabel, statusField),
            createFormFieldRow(availabilityLabel, availabilityField)
        ));
        formPanel.add(Box.createVerticalStrut(16));

        JButton submitBtn = new JButton("Submit to VC");
        stylePrimaryButton(submitBtn);
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || vehicleIdField.getText().isBlank()) {
                showFeedback("Please complete all vehicle owner fields.", DANGER, 4200);
                return;
            }
            String entry = String.format("[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                vehicleIdField.getText().trim(),
                String.valueOf(statusField.getSelectedItem()),
                String.valueOf(availabilityField.getSelectedItem()));
            refreshMonitor("Vehicle Owner update queued for admin review:\n" + entry);
            sendToVCController(entry, ownerIdField.getText().trim());
        });
        formPanel.add(createFullWidthButton(submitBtn));

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 18));
        body.setOpaque(false);
        body.add(createSurfaceCard(
            "Vehicle Status",
            "",
            wrapFormPanel(formPanel)
        ));

        return createDashboardPage(
            "",
            "Owner",
            "",
            body,
            null
        );
    }

    //VK- DH
    public void keypressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveEntry();
        }
    }

    private JComponent createWorkspaceLayout(JComponent mainContent) {
        JComponent scrollableMain = createMainViewport(mainContent);
        if (isClientUser()) {
            return createStackedWorkspace(scrollableMain, createClientJobsPanel());
        }

        if (isOwnerUser()) {
            return createStackedWorkspace(scrollableMain, createOwnerVehiclePanel());
        }

        return createStackedWorkspace(scrollableMain);
    }

    private JComponent createStackedWorkspace(JComponent mainContent, JComponent... supportPanels) {
        JPanel workspace = new JPanel(new BorderLayout(0, 8));
        workspace.setOpaque(false);
        workspace.add(mainContent, BorderLayout.CENTER);

        if (supportPanels != null && supportPanels.length > 0) {
            JPanel supportStack = new JPanel();
            supportStack.setOpaque(false);
            supportStack.setLayout(new BoxLayout(supportStack, BoxLayout.Y_AXIS));
            for (int i = 0; i < supportPanels.length; i++) {
                supportPanels[i].setAlignmentX(Component.LEFT_ALIGNMENT);
                supportPanels[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, supportPanels[i].getPreferredSize().height));
                supportStack.add(supportPanels[i]);
                if (i < supportPanels.length - 1) {
                    supportStack.add(Box.createVerticalStrut(8));
                }
            }
            workspace.add(supportStack, BorderLayout.SOUTH);
        }

        return workspace;
    }

    private JComponent createMainViewport(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_SOFT));
        scrollPane.getViewport().setBackground(SHELL_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createDashboardPage(String eyebrow, String title, String subtitle, JComponent body, JComponent footer) {
        JPanel page = new JPanel(new BorderLayout(0, 10));
        page.setBackground(SHELL_BG);
        page.setBorder(createCardBorder());
        if ((title != null && !title.isBlank()) || (subtitle != null && !subtitle.isBlank()) || (eyebrow != null && !eyebrow.isBlank())) {
            page.add(createPageIntroCard(eyebrow, title, subtitle), BorderLayout.NORTH);
        }
        page.add(body, BorderLayout.CENTER);
        if (footer != null) {
            page.add(footer, BorderLayout.SOUTH);
        }
        return page;
    }

    private JPanel createPageIntroCard(String eyebrow, String title, String subtitle) {
        JPanel intro = new JPanel();
        intro.setBackground(SHELL_BG);
        intro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_SOFT),
            new EmptyBorder(0, 2, 8, 2)
        ));
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));

        if (eyebrow != null && !eyebrow.isBlank()) {
            JLabel eyebrowLabel = new JLabel(eyebrow);
            eyebrowLabel.setForeground(TEXT_MUTED);
            eyebrowLabel.setFont(new Font("Dialog", Font.BOLD, 11));
            intro.add(eyebrowLabel);
            intro.add(Box.createVerticalStrut(4));
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        intro.add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            intro.add(Box.createVerticalStrut(4));

            JLabel subtitleLabel = new JLabel("<html><div style='width:640px;'>" + subtitle + "</div></html>");
            subtitleLabel.setForeground(TEXT_SECONDARY);
            subtitleLabel.setFont(BODY_FONT);
            intro.add(subtitleLabel);
        }
        return intro;
    }

    private JPanel createSurfaceCard(String title, String subtitle, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(SURFACE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(10, 10, 10, 10)
        ));

        if ((title != null && !title.isBlank()) || (subtitle != null && !subtitle.isBlank())) {
            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

            if (title != null && !title.isBlank()) {
                JLabel titleLabel = new JLabel(title);
                titleLabel.setForeground(TEXT_PRIMARY);
                titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
                header.add(titleLabel);
            }

            if (subtitle != null && !subtitle.isBlank()) {
                if (title != null && !title.isBlank()) {
                    header.add(Box.createVerticalStrut(4));
                }
                JLabel subtitleLabel = new JLabel("<html><div style='width:620px;'>" + subtitle + "</div></html>");
                subtitleLabel.setForeground(TEXT_SECONDARY);
                subtitleLabel.setFont(BODY_FONT);
                header.add(subtitleLabel);
            }

            card.add(header, BorderLayout.NORTH);
        }

        if (content != null) {
            card.add(content, BorderLayout.CENTER);
        }

        return card;
    }

    private JPanel createActionCard(String eyebrow, String title, String description, JComponent content) {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel eyebrowLabel = new JLabel(eyebrow);
        eyebrowLabel.setForeground(ACCENT);
        eyebrowLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        text.add(eyebrowLabel);
        text.add(Box.createVerticalStrut(6));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));

        JLabel descriptionLabel = new JLabel("<html><div style='width:280px;'>" + description + "</div></html>");
        descriptionLabel.setForeground(TEXT_MUTED);
        descriptionLabel.setFont(BODY_FONT);
        text.add(descriptionLabel);

        body.add(text, BorderLayout.CENTER);
        if (content != null) {
            body.add(content, BorderLayout.SOUTH);
        }

        return createSurfaceCard(null, null, body);
    }

    private JPanel createLaunchCard(String title, String subtitle, JComponent content) {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        body.add(titleLabel, BorderLayout.NORTH);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setForeground(TEXT_MUTED);
            subtitleLabel.setFont(BODY_FONT);
            body.add(subtitleLabel, BorderLayout.CENTER);
        }

        if (content != null) {
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            footer.add(content, BorderLayout.CENTER);
            body.add(footer, BorderLayout.SOUTH);
        }

        return createSurfaceCard(null, null, body);
    }

    private JPanel createMetaCard(String title, String primary, String secondary) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(SURFACE_BG_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setFont(VcrtsTheme.META_FONT);
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JLabel primaryLabel = new JLabel(primary);
        primaryLabel.setForeground(TEXT_PRIMARY);
        primaryLabel.setFont(LABEL_FONT);
        stack.add(primaryLabel);

        JLabel secondaryLabel = new JLabel(secondary);
        secondaryLabel.setForeground(TEXT_SECONDARY);
        secondaryLabel.setFont(BODY_FONT);
        stack.add(Box.createVerticalStrut(4));
        stack.add(secondaryLabel);
        card.add(stack, BorderLayout.CENTER);
        return card;
    }

    private JPanel createButtonStack(AbstractButton... buttons) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            buttonPanel.add(buttons[i]);
            if (i < buttons.length - 1) {
                buttonPanel.add(Box.createVerticalStrut(10));
            }
        }
        return buttonPanel;
    }

    private String screenTitle(String screenId) {
        if (FORM_SCREEN.equals(screenId)) {
            return "Client";
        }
        if (TASK_OWNER_SCREEN.equals(screenId)) {
            return "Task Owner";
        }
        if (VEHICLE_OWNER_SCREEN.equals(screenId)) {
            return "Owner";
        }
        if (ADMIN_SCREEN.equals(screenId)) {
            return "Admin";
        }
        return "Dashboard";
    }

    private JPanel createFooterRow(JButton button) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(button);
        return footer;
    }

    private JPanel createStatusBadge(String title, JLabel valueLabel) {
        JPanel badge = new JPanel(new BorderLayout(0, 6));
        badge.setBackground(SURFACE_BG_ALT);
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(ACCENT);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        badge.add(titleLabel, BorderLayout.NORTH);
        badge.add(valueLabel, BorderLayout.CENTER);
        return badge;
    }

    private void showFeedback(String message, Color tone, int hideAfterMs) {
        if (feedbackTimer != null) {
            feedbackTimer.stop();
        }
        feedbackBar.setBackground(tone);
        feedbackLabel.setText(message);
        feedbackBar.setVisible(true);
        frame.revalidate();
        frame.repaint();
        if (hideAfterMs > 0) {
            feedbackTimer = new Timer(hideAfterMs, e -> clearFeedback());
            feedbackTimer.setRepeats(false);
            feedbackTimer.start();
        }
    }

    private void clearFeedback() {
        feedbackBar.setVisible(false);
        feedbackLabel.setText("");
        frame.revalidate();
        frame.repaint();
    }

    private void showStyledContentDialog(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(SURFACE_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT_PRIMARY);
        heading.setFont(SECTION_FONT);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel, title, JOptionPane.PLAIN_MESSAGE);
    }

    private void showSystemLogDialog() {
        JScrollPane scrollPane = new JScrollPane(getMonitorArea());
        styleScrollPane(scrollPane, null);
        scrollPane.setPreferredSize(new Dimension(760, 280));
        refreshMonitor(null);
        showStyledContentDialog("System Log", scrollPane);
    }

    private JPanel createMonitorPanel() {
        JPanel terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(getMonitorArea());
        styleScrollPane(scrollPane, null);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        terminalPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel panel = createSurfaceCard(
            "System Log",
            "",
            terminalPanel
        );
        panel.setPreferredSize(new Dimension(0, 130));
        return panel;
    }

    private JTextArea getMonitorArea() {
        if (monitorArea == null) {
            monitorArea = new JTextArea();
            monitorArea.setEditable(false);
            monitorArea.setBackground(TERMINAL_BG);
            monitorArea.setForeground(TEXT_SECONDARY);
            monitorArea.setFont(VcrtsTheme.MONO_FONT);
            monitorArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        }
        return monitorArea;
    }

    // --- LOGIC METHODS (Migrated from ConsolePanel) ---

    private JLabel createWhiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUTED);
        label.setFont(LABEL_FONT);
        return label;
    }

    private JPanel createFormStack() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel createFormFieldRow(JLabel label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel createInlineFormRow(JComponent left, JComponent right) {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(left);
        row.add(right);
        return row;
    }

    private JPanel createFullWidthButton(AbstractButton button) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(button, BorderLayout.CENTER);
        return row;
    }

    private JPanel wrapFormPanel(JComponent formPanel) {
        JPanel rail = new JPanel();
        rail.setOpaque(false);
        rail.setLayout(new BoxLayout(rail, BoxLayout.X_AXIS));

        JPanel constrained = new JPanel(new BorderLayout());
        constrained.setOpaque(false);
        constrained.setPreferredSize(new Dimension(FORM_RAIL_WIDTH, formPanel.getPreferredSize().height));
        constrained.setMaximumSize(new Dimension(FORM_RAIL_WIDTH, Integer.MAX_VALUE));
        constrained.add(formPanel, BorderLayout.NORTH);

        rail.add(constrained);
        rail.add(Box.createHorizontalGlue());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(rail, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createHomeSummaryCard() {
        JPanel card = new JPanel(new GridLayout(0, 1, 0, 6));
        card.setBackground(SURFACE_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel headline = new JLabel("Shared workspace");
        headline.setForeground(TEXT_PRIMARY);
        headline.setFont(new Font("Dialog", Font.BOLD, 14));
        card.add(headline);

        JLabel body = new JLabel("<html><div style='width:420px;'>"
            + "Jobs, vehicles, approvals, and system state in one place."
            + "</div></html>");
        body.setForeground(TEXT_MUTED);
        body.setFont(BODY_FONT);
        card.add(body);
        return card;
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(10, 10, 10, 10)
        );
    }

    private void stylePrimaryButton(AbstractButton button) {
        styleButton(button, ACCENT, TEXT_PRIMARY);
    }

    private void styleSecondaryButton(AbstractButton button) {
        styleButton(button, SURFACE_BG, TEXT_PRIMARY);
    }

    private void styleSuccessButton(AbstractButton button) {
        styleButton(button, new Color(23, 94, 59), TEXT_PRIMARY);
    }

    private void styleDangerButton(AbstractButton button) {
        styleButton(button, new Color(127, 29, 29), TEXT_PRIMARY);
    }

    private void styleNavButton(AbstractButton button) {
        button.setBackground(SIDEBAR_BG);
        button.setForeground(TEXT_SECONDARY);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(LABEL_FONT);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(8, 10, 8, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleButton(AbstractButton button, Color background, Color foreground) {
        button.setFont(LABEL_FONT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(6, 10, 6, 10));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(background.equals(SURFACE_BG) ? ACCENT_SOFT : background.darker()),
            new EmptyBorder(7, 10, 7, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JComponent.minimumWidth", 120);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setPreferredSize(new Dimension(144, 30));

        Color hoverBackground = background.equals(SURFACE_BG)
            ? new Color(31, 36, 43)
            : new Color(
                Math.min(background.getRed() + 8, 255),
                Math.min(background.getGreen() + 8, 255),
                Math.min(background.getBlue() + 8, 255)
            );
        Color borderColor = background.equals(SURFACE_BG) ? ACCENT_SOFT : background.darker();
        Color hoverBorderColor = background.equals(SURFACE_BG) ? ACCENT : hoverBackground.darker();

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isEnabled()) {
                    return;
                }
                button.setBackground(hoverBackground);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(hoverBorderColor),
                    new EmptyBorder(7, 10, 7, 10)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor),
                    new EmptyBorder(7, 10, 7, 10)
                ));
            }
        });
    }

    private void styleTextField(JTextField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(BODY_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void styleTextArea(JTextArea area) {
        area.setBackground(FIELD_BG);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setFont(BODY_FONT);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(FIELD_BG);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFont(BODY_FONT);
        comboBox.setBorder(BorderFactory.createLineBorder(ACCENT_SOFT));
        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, 34));
    }

    private void styleTable(JTable table) {
        table.setBackground(SURFACE_BG_ALT);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(ACCENT_SOFT);
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setRowHeight(28);
        table.setFont(BODY_FONT);
        table.getTableHeader().setBackground(SURFACE_BG);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(LABEL_FONT);
        table.setBorder(BorderFactory.createEmptyBorder());
    }

    private void styleScrollPane(JScrollPane scrollPane, String title) {
        scrollPane.getViewport().setBackground(SURFACE_BG_ALT);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_SOFT));
    }

    private JLabel createNotificationBadge(int count) {
        JLabel badge = new JLabel(String.valueOf(count));
        badge.setOpaque(true);
        badge.setBackground(DANGER);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Dialog", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setVisible(count > 0);
        return badge;
    }

    private JLabel createHeaderBadge(String text) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(SURFACE_BG);
        badge.setForeground(TEXT_MUTED);
        badge.setFont(new Font("Dialog", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_SOFT),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return badge;
    }

    private void refreshNotifications() {
        if (!isClientUser() || notificationBadge == null) {
            return;
        }
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
            showFeedback("Notifications are available for clients only.", WARNING, 3000);
            return;
        }
        try {
            List<String> unread = service.getUnreadNotifications(service.getCurrentUsername());
            if (unread == null || unread.isEmpty()) {
                showFeedback("No unread notifications.", SURFACE_BG_ALT, 2600);
                if (notificationBadge != null) {
                    notificationBadge.setVisible(false);
                }
                return;
            }

            StringBuilder message = new StringBuilder();
            for (String note : unread) {
                message.append("- ").append(note).append("\n\n");
            }

            JTextArea area = new JTextArea(message.toString());
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBackground(TERMINAL_BG);
            area.setForeground(TEXT_PRIMARY);
            area.setFont(BODY_FONT);

            JScrollPane scroll = new JScrollPane(area);
            styleScrollPane(scroll, null);
            scroll.setPreferredSize(new Dimension(520, 320));

            showStyledContentDialog("Unread Notifications", scroll);
            service.markNotificationsRead(service.getCurrentUsername());
            refreshNotifications();
        } catch (IOException e) {
            showFeedback("Unable to load notifications.", DANGER, 3200);
        }
    }

    private void displayUnreadNotificationsIfAny() {
        if (!isClientUser()) {
            return;
        }
        try {
            List<String> unread = service.getUnreadNotifications(service.getCurrentUsername());
            if (unread != null && !unread.isEmpty()) {
                showFeedback(unread.get(0), ACCENT_GHOST, 5000);
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
                if (vehicleId.isBlank()) {
                    continue;
                }

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

    private void showResidencyTimeOverview() {
        try {
            if (isAdminUser()) {
                showAdminResidencyOverview();
                return;
            }

            String currentUsername = service.getCurrentUsername();
            if (currentUsername == null || currentUsername.isBlank()) {
                showFeedback("No active user session found.", WARNING, 3200);
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

            showFeedback("No residency view available for this role.", WARNING, 3200);
        } catch (Exception e) {
            showFeedback("Unable to load residency time overview.", DANGER, 3600);
        }
    }

    private void showAdminResidencyOverview() throws IOException {
        List<JobCompletionRecord> records = controller.calculateCompletionTimes();
        if (records.isEmpty()) {
            showFeedback("No client jobs found.", WARNING, 3200);
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
            if (!matchesSubmitter(record, currentUsername)) {
                continue;
            }
            rows.add(new Object[] {
                safeValue(record.get("JOB_ID")),
                safeValue(record.get("DESCRIPTION")),
                safeValue(record.get("DURATION")),
                safeValue(record.get("DEADLINE")),
                safeValue(record.get("STATUS"))
            });
        }

        if (rows.isEmpty()) {
            showFeedback("No residency records found for your account.", WARNING, 3200);
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
        for (Map<String, String> record : service.readAllVehicles()) {
            if (!currentUsername.equals(safeValue(record.get("OWNER_ID")))) {
                continue;
            }
            rows.add(new Object[] {
                safeValue(record.get("VEHICLE_ID")),
                safeValue(record.get("RESIDENCY_HOURS")),
                "N/A",
                safeValue(record.get("STATUS"))
            });
        }

        if (rows.isEmpty()) {
            showFeedback("No vehicle residency records found for your account.", WARNING, 3200);
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

        styleTable(table);
        styleScrollPane(scrollPane, null);
        showStyledContentDialog(title, scrollPane);
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

    private boolean matchesSubmitter(Map<String, String> record, String currentUsername) {
        String submitter = record.get("SUBMITTER");
        if (submitter != null && !submitter.isBlank() && currentUsername.equals(submitter)) {
            return true;
        }
        return currentUsername.equals(record.get("ID"));
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

    private void adjustFields() {
        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Job ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        if (deadlineLabel != null) deadlineLabel.setVisible(isClient);
        if (deadlineField != null) deadlineField.setVisible(isClient);
        frame.revalidate(); // Refreshes the UI so hidden fields don't leave weird spaces
    }

    private void saveEntry() {
        String role = (String) roleBox.getSelectedItem();
        if ("ADMIN".equals(role)) {
            showFeedback("Admin does not submit records. Use Calculate Completion Time.", WARNING, 3800);
            return;
        }

        //Form input collected cleanly- TC
        String id = idField.getText().trim();
        String info = infoField.getText().trim();
        String dur = durField.getText().trim();
        String deadline = deadlineField.isVisible() ? deadlineField.getText().trim() : "N/A";
        int duration;


        if (id.isEmpty() || info.isEmpty() || dur.isEmpty()) {
            showFeedback("Please enter all required fields.", DANGER, 3800);
            return;
        }

        try {
            duration = Integer.parseInt(dur);
        } catch (NumberFormatException e) {
            showFeedback("Duration must be a number.", WARNING, 3800);
            return;
        }
/*
        try {
            LocalDateTime arrivalTime = LocalDateTime.now();
            String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%d",
                dtf.format(arrivalTime), role, id, info, duration);

        if ("CLIENT".equals(role) && deadline.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Client jobs must include a deadline.");
            return;
        }*/

        try {
            LocalDateTime arrivalTime = LocalDateTime.now();
            LocalDateTime deadlineTime = deadlineField.isVisible()
                ? LocalDateTime.parse(deadline, dtf)
                : null;
            String formattedDeadline = deadlineTime == null ? "N/A" : dtf.format(deadlineTime);
            String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%d | DEADLINE:%s",
                dtf.format(arrivalTime), role, id, info, duration, formattedDeadline);

            // Send this the to VC Controller server over socket
            refreshMonitor("Connecting to VC Controller server...");

            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            outputStream.writeUTF(entry);
            outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : "");

            String ack = inputStream.readUTF();
            String requestId = inputStream.readUTF();
            refreshMonitor("Server response: " + ack + " (Request ID: " + requestId + ") - Pending approval...");
            try {
                service.addNotification(service.getCurrentUsername(), "Server ACK received (Request ID: " + requestId + ") — request pending approval.");
                refreshNotifications();
            } catch (IOException ignored) {}

            showFeedback("Transaction submitted for admin review.", SUCCESS, 4200);
            clear();

            // Keep socket open on a background thread until admin decides, then close
            new Thread(() -> {
                try {
                    inputStream.readUTF();
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                } catch (IOException ignored) {}
            }).start();

        } catch (java.time.format.DateTimeParseException e) {
            showFeedback("Deadline must use format yyyy/MM/dd HH:mm:ss.", WARNING, 4200);
        } catch (java.net.ConnectException e) {
            showFeedback("Cannot connect to VC Controller server. Make sure the server is running first.", DANGER, 5000);
        } catch (IllegalArgumentException e) {
            showFeedback(e.getMessage(), WARNING, 4200);
        } catch (IOException e) {
            showFeedback("Connection error: " + e.getMessage(), DANGER, 5000);
        }
    }

    private void calculateCompletionTimes() {
        try {
            List<JobCompletionRecord> records = controller.calculateCompletionTimes();
            if (records.isEmpty()) {
                String msg = "No client jobs found.";
                if (canViewVcrtsLogs()) {
                    refreshMonitor(msg);
                } else {
                    showFeedback(msg, WARNING, 3200);
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
                textArea.setBackground(TERMINAL_BG);
                textArea.setForeground(TEXT_PRIMARY);
                textArea.setFont(VcrtsTheme.MONO_FONT);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 300));
                styleScrollPane(scrollPane, null);
                showStyledContentDialog("Completion Times", scrollPane);
            }
            refreshMonitor(results.toString().trim());
        } catch (HeadlessException | IOException e) {
            showFeedback("Error calculating completion times.", DANGER, 3600);
        }
    }

    private void sendToVCController(String entry, String id) {
        try {
            refreshMonitor("Connecting to VC Controller server...");
            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            // Send data and username
            outputStream.writeUTF(entry);
            outputStream.writeUTF(service.getCurrentUsername() != null ? service.getCurrentUsername() : id);

            // Wait for ACK
            String ack = inputStream.readUTF();
            String requestId = inputStream.readUTF();
            refreshMonitor("Server response: " + ack + " (Request ID: " + requestId + ") - Pending approval...");
            try {
                service.addNotification(service.getCurrentUsername(), "Server ACK received (Request ID: " + requestId + ") — submission pending approval.");
                refreshNotifications();
            } catch (IOException ignored) {}

            showFeedback("Submission sent for admin review.", SUCCESS, 4200);

            // Background thread to wait for final decision (ACCEPTED/REJECTED)
            new Thread(() -> {
                try {
                    String finalDecision = inputStream.readUTF();
                    refreshMonitor("Final Admin Decision: " + finalDecision);
                    socket.close();
                } catch (IOException ignored) {}
            }).start();

        } catch (IOException ex) {
            showFeedback("Server error: Make sure ServerMain is running.", DANGER, 5000);
        }
    }

//User Validation Functions -DH

    private boolean isAdminUser() {
        return "ADMIN".equals(currentUserRole);
    }

    private boolean isOwnerUser() {
        return "OWNER".equals(currentUserRole);
    }

    private boolean isClientUser() {
        return "CLIENT".equals(currentUserRole);
    }

    private boolean hasRightSidePanel() {
        return isOwnerUser() || isClientUser() || isAdminUser();
    }

    private boolean canViewVcrtsLogs() {
        return isOwnerUser() || isAdminUser();
    }

    private void refreshMonitor(String resultSection) {
        getMonitorArea();
        try {
            StringBuilder display = new StringBuilder();
            for (String line : service.readAllLogs()) {
                display.append(line).append("\n");
            }

            if (resultSection != null && !resultSection.isBlank()) {
                display.append("\n\n").append(resultSection);
            }

            monitorArea.setText(display.toString());
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength()); // Auto-scroll to bottom
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
            return createStackedWorkspace(clientPanel, activityPanel);
        }

        if (!isOwnerUser()) {
            return createMonitorPanel();
        }

        JPanel ownerPanel = createOwnerVehiclePanel();
        refreshOwnerVehiclePanel();
        return createStackedWorkspace(ownerPanel, createMonitorPanel());
    }

    private JPanel createOwnerVehiclePanel() {
        String[] columns = {"Vehicle", "Activity", "Availability", "Updates", "Last Update"};
        ownerVehicleModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ownerVehicleTable = new JTable(ownerVehicleModel);
        styleTable(ownerVehicleTable);
        ownerVehicleTable.setFillsViewportHeight(true);
        ownerVehicleTable.setRowHeight(26);
        ownerVehicleTable.setAutoCreateRowSorter(true);
        ownerVehicleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumn vehicleCol = ownerVehicleTable.getColumnModel().getColumn(0);
        TableColumn activityCol = ownerVehicleTable.getColumnModel().getColumn(1);
        TableColumn availabilityCol = ownerVehicleTable.getColumnModel().getColumn(2);
        TableColumn updatesCol = ownerVehicleTable.getColumnModel().getColumn(3);
        TableColumn lastUpdateCol = ownerVehicleTable.getColumnModel().getColumn(4);
        vehicleCol.setPreferredWidth(180);
        activityCol.setPreferredWidth(200);
        availabilityCol.setPreferredWidth(140);
        updatesCol.setPreferredWidth(90);
        lastUpdateCol.setPreferredWidth(210);

        JScrollPane scrollPane = new JScrollPane(ownerVehicleTable);
        styleScrollPane(scrollPane, null);
        scrollPane.setPreferredSize(new Dimension(0, 190));

        JPanel panel = createSurfaceCard("Vehicle Roster", "", scrollPane);
        panel.setPreferredSize(new Dimension(0, 230));
        return panel;
    }

    private JPanel createClientJobsPanel() {
        String[] columns = {"Job ID", "Description", "Duration", "Deadline", "Status"};
        clientJobModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clientJobTable = new JTable(clientJobModel);
        styleTable(clientJobTable);
        clientJobTable.setFillsViewportHeight(true);
        clientJobTable.setRowHeight(26);
        clientJobTable.setAutoCreateRowSorter(true);
        clientJobTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumn jobIdCol = clientJobTable.getColumnModel().getColumn(0);
        TableColumn descriptionCol = clientJobTable.getColumnModel().getColumn(1);
        TableColumn durationCol = clientJobTable.getColumnModel().getColumn(2);
        TableColumn deadlineCol = clientJobTable.getColumnModel().getColumn(3);
        TableColumn statusCol = clientJobTable.getColumnModel().getColumn(4);
        jobIdCol.setPreferredWidth(120);
        descriptionCol.setPreferredWidth(320);
        durationCol.setPreferredWidth(100);
        deadlineCol.setPreferredWidth(210);
        statusCol.setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(clientJobTable);
        styleScrollPane(scrollPane, null);
        scrollPane.setPreferredSize(new Dimension(0, 185));

        JPanel panel = createSurfaceCard("My Jobs", "", scrollPane);
        panel.setPreferredSize(new Dimension(0, 225));
        return panel;
    }

    private JPanel createClientActivityPanel() {
        clientActivityArea = new JTextArea();
        clientActivityArea.setEditable(false);
        clientActivityArea.setBackground(TERMINAL_BG);
        clientActivityArea.setForeground(TEXT_SECONDARY);
        clientActivityArea.setFont(VcrtsTheme.MONO_FONT);

        JScrollPane scrollPane = new JScrollPane(clientActivityArea);
        styleScrollPane(scrollPane, null);
        scrollPane.setPreferredSize(new Dimension(0, 140));

        JPanel panel = createSurfaceCard("Activity", "", scrollPane);
        panel.setPreferredSize(new Dimension(0, 180));
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
            for (Map<String, String> record : service.readAllVehicles()) {
                if (!currentUsername.equals(safeValue(record.get("OWNER_ID")))) {
                    continue;
                }

                String vehicleId = safeValue(record.get("VEHICLE_ID"));
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
                if (!matchesSubmitter(record, currentUsername)) {
                    continue;
                }
                clientJobModel.addRow(new Object[] {
                    safeValue(record.get("JOB_ID")),
                    safeValue(record.get("DESCRIPTION")),
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
            for (Map<String, String> record : service.readClientJobRecords()) {
                if (!matchesSubmitter(record, currentUsername)) {
                    continue;
                }
                display.append("Job ID: ")
                    .append(safeValue(record.get("JOB_ID")))
                    .append(" | Description: ")
                    .append(safeValue(record.get("DESCRIPTION")))
                    .append(" | Duration: ")
                    .append(safeValue(record.get("DURATION")))
                    .append(" | Deadline: ")
                    .append(safeValue(record.get("DEADLINE")))
                    .append(" | Status: ")
                    .append(safeValue(record.get("STATUS")))
                    .append("\n");
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

    private void startClientNotificationTimer() {
        stopClientNotificationTimer();
        clientNotificationTimer = new Timer(2000, e -> refreshNotifications());
        clientNotificationTimer.start();
    }

    private void stopClientNotificationTimer() {
        if (clientNotificationTimer != null) {
            clientNotificationTimer.stop();
            clientNotificationTimer = null;
        }
    }

    private void showSelectedPendingRequestDetails() {
        if (pendingRequestsModel == null || pendingRequestsTable == null) return;
        int row = pendingRequestsTable.getSelectedRow();
        if (row < 0) {
            showFeedback("Select a row in the table first.", WARNING, 3000);
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
        area.setBackground(TERMINAL_BG);
        area.setForeground(TEXT_PRIMARY);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(720, 480));
        styleScrollPane(sp, null);
        showStyledContentDialog("Request Details", sp);
    }

    private void refreshPendingAdminRequest() {
        if (pendingRequestsModel == null || pendingRequestsTable == null || adminRequestStatusLabel == null) return;

        try {
            List<Map<String, String>> requests = service.readAllPendingRequests();
            
            // Save selected row ID to avoid resetting selection on every timer tick
            int selectedRow = pendingRequestsTable.getSelectedRow();
            String selectedId = selectedRow >= 0 ? (String) pendingRequestsModel.getValueAt(selectedRow, 0) : null;

            pendingRequestsModel.setRowCount(0); // Clear table

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
                
                // Parse role dynamically so the admin can see it in its own column
                Map<String, String> parsed = service.parseLogEntry(entry);
                String role = parsed.getOrDefault("ROLE", "UNKNOWN");
                
                pendingRequestsModel.addRow(new Object[]{id, submitter, role, entry});
                
                if (id != null && id.equals(selectedId)) {
                    newSelectedIndex = i;
                }
            }
            
            // Restore selection
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
            showFeedback("Please select a request from the table first.", WARNING, 3200);
            return;
        }

        // Grab data from the selected row
        String requestId = (String) pendingRequestsModel.getValueAt(selectedRow, 0);
        String submitter = (String) pendingRequestsModel.getValueAt(selectedRow, 1);
        String entry = (String) pendingRequestsModel.getValueAt(selectedRow, 3);

        try {
            service.writeAdminDecision(requestId, decision);

            if (submitter != null && !submitter.isBlank()) {
                String notifMsg = "Your submission was " + decision + ":\n" + entry;
                service.addNotification(submitter, notifMsg);
            }

            adminRequestStatusLabel.setText("Last response sent: " + decision);
            refreshMonitor("Admin decision sent for request:\n" + entry + "\nSTATUS: " + decision);
            showFeedback("Decision sent: " + decision + ".", "ACCEPTED".equals(decision) ? SUCCESS : WARNING, 3600);
            
            refreshPendingAdminRequest(); // Refresh the table immediately to clear the accepted/rejected row
        } catch (IOException e) {
            showFeedback("Unable to send admin decision.", DANGER, 3600);
        }
    }

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }

}
