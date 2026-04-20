package views;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JTable pendingRequestsTable;
    private javax.swing.table.DefaultTableModel pendingRequestsModel;
    private JLabel adminRequestStatusLabel;
    private Timer adminRefreshTimer;
    //private Timer notificationTimer;

    public VCRTSDashboard(CloudDataService service, String currentUserRole) {
       this.service = service;
        this.controller = new VCController(service);
        this.currentUserRole = currentUserRole == null ? "CLIENT" : currentUserRole.toUpperCase();

        // 1. Setup Main Frame
        frame = new JFrame("VCRTS - Cloud Control Center");
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(20, 20, 25));

        // 2. Add Header
        frame.add(createHeader(), BorderLayout.NORTH);

        // 3. Setup the CardLayout for the left panel -DH
        cardLayout = new CardLayout();
        leftCardContainer = new JPanel(cardLayout);
        
        // Home panel is the default view, form panel is for submissions, and we can add more as needed
        leftCardContainer.add(createHomePanel(service), "HOME_SCREEN");
        leftCardContainer.add(createJobSubmissionTab(), "FORM_SCREEN");
        if (isAdminUser()) {//Only an admin may open this panel
            leftCardContainer.add(createAdminScreen(service), "ADMIN_SCREEN");
        }
        leftCardContainer.add(createTaskOwnerScreen(service), "TASK_OWNER_SCREEN");
        leftCardContainer.add(createVehicleOwnerScreen(service), "VEHICLE_OWNER_SCREEN");
        leftCardContainer.setBackground(new Color(30, 30, 35));

        // 4. Layout: split with monitor for owner/admin, content-only for client
        if (canViewVcrtsLogs()) {
            JPanel rightMonitorPanel = createMonitorPanel();
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCardContainer, rightMonitorPanel);
            splitPane.setDividerLocation(400);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            frame.add(splitPane, BorderLayout.CENTER);
        } else {
            frame.add(leftCardContainer, BorderLayout.CENTER);
        }

        // Initialize state:
        adjustFields();
        if (canViewVcrtsLogs()) {
            refreshMonitor(null);
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (isClientUser()) {
          //  showUnreadNotifications();
            //startNotificationTimer();
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
            JButton btnCalcTimes = new JButton("Calculate Completion Times");
            btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
            gbc.gridy = nextRow++;
            panel.add(btnCalcTimes, gbc);
        }
      if (isClientUser()) {
            JButton btnOpenForm = new JButton("Open Client Portal");
            btnOpenForm.setFont(new Font("SansSerif", Font.BOLD, 14));
            
            // UPDATE THIS LINE to call the new panel:
            btnOpenForm.addActionListener(e -> showScreen(createCombinedClientPanel(service)));
            
            gbc.gridy = nextRow++;
            gbc.insets = new Insets(20, 0, 10, 0);
            panel.add(btnOpenForm, gbc);
        }

        if (isOwnerUser()) {//DH
            JButton taskOwnerBtn = new JButton("Task Owner Portal");
            taskOwnerBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            taskOwnerBtn.addActionListener(e -> showScreen(createTaskOwnerScreen(service)));
            gbc.gridy = nextRow++;
            gbc.insets = new Insets(10, 0, 10, 0);
            panel.add(taskOwnerBtn, gbc);

            JButton vehicleOwnerBtn = new JButton("Vehicle Owner Portal");
            vehicleOwnerBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            vehicleOwnerBtn.addActionListener(e -> showScreen(createVehicleOwnerScreen(service)));
            gbc.gridy = nextRow++;
            panel.add(vehicleOwnerBtn, gbc);
        }

        if (isAdminUser()) {//DH
            JButton btnAdminScreen = new JButton("Go to Admin Screen");
            btnAdminScreen.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnAdminScreen.addActionListener(e -> showScreen(createAdminScreen(service)));
            gbc.gridy = nextRow++;
            panel.add(btnAdminScreen, gbc);
        }

        /*JTextArea introMessage = new JTextArea(
            "VCRTS lets users submit jobs, store job data in files, and calculate FIFO completion times.\n\n" +
            "How to proceed:\n" +
            "1. Click \"Submit New Transaction\"\n" +
            "2. Enter Job ID, description, duration, and deadline\n" +
            "3. Submit the entry\n" +
            "4. Click \"Calculate Completion Times\" to view results"
        );
        introMessage.setEditable(false);
        introMessage.setLineWrap(true);
        introMessage.setWrapStyleWord(true);
        introMessage.setOpaque(false);
        introMessage.setForeground(Color.LIGHT_GRAY);
        introMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        introMessage.setBorder(null);

        gbc.gridy = 7;
        gbc.weighty = 0.3;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(introMessage, gbc);
*/
        return panel;
        
    }

    public JPanel createAdminScreen(CloudDataService service) { //DH
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
        titleLabel.setForeground(Color.black);
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
            public boolean isCellEditable(int row, int column) { return false; } // Read-only
        };
        
        pendingRequestsTable = new JTable(pendingRequestsModel);
        pendingRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingRequestsTable.setBackground(Color.BLACK);
        pendingRequestsTable.setForeground(Color.GREEN);
        pendingRequestsTable.setGridColor(Color.DARK_GRAY);

        JScrollPane pendingScrollPane = new JScrollPane(pendingRequestsTable);
        pendingScrollPane.setPreferredSize(new Dimension(500, 150));
        adminPanel.add(pendingScrollPane, gbc);

        // Reset gbc for the buttons below
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCalcTimes.setBackground(new Color(70, 130, 180));
        btnCalcTimes.setForeground(Color.RED);
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());
        adminPanel.add(btnCalcTimes, gbc);

        gbc.gridy = 4;
        JButton acceptBtn = new JButton("Accept");
        acceptBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        acceptBtn.addActionListener(e -> submitAdminDecision("ACCEPTED"));
        adminPanel.add(acceptBtn, gbc);

        gbc.gridy = 5;
        JButton rejectBtn = new JButton("Reject");
        rejectBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        rejectBtn.addActionListener(e -> submitAdminDecision("REJECTED"));
        adminPanel.add(rejectBtn, gbc);

        gbc.gridy = 6;
        JButton backBtn = new JButton("Back to Home");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        backBtn.addActionListener(e -> {
            stopAdminRefreshTimer();
            showScreen(createHomePanel(service));
        });
        adminPanel.add(backBtn, gbc);

        startAdminRefreshTimer();
        refreshPendingAdminRequest();

        return adminPanel;
    }


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

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
          //  stopNotificationTimer();
            stopAdminRefreshTimer();
            frame.dispose();
            new LoginScreen(service);
        });
        header.add(logoutBtn, BorderLayout.EAST);

        return header;
    }

// 1. The Main Tabbed Container
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

    // 2. The Job Submission Form (Tab 1)
    private JPanel createJobSubmissionTab() { 
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createWhiteLabel("Select Role:"), gbc);

        roleBox = new JComboBox<>(new String[]{"CLIENT"});
        roleBox.setEnabled(false);
        roleBox.addActionListener(e -> adjustFields());
        gbc.gridx = 1; panel.add(roleBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        idLabel = createWhiteLabel("Client ID:");
        panel.add(idLabel, gbc);
        idField = new JTextField();
        gbc.gridx = 1; panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoLabel = createWhiteLabel("Job Description:");
        panel.add(infoLabel, gbc);
        infoField = new JTextField();
        gbc.gridx = 1; panel.add(infoField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        durLabel = createWhiteLabel("Duration (Hrs):");
        panel.add(durLabel, gbc);
        durField = new JTextField("e.g., '2' for 2 hours");
        gbc.gridx = 1; panel.add(durField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        deadlineLabel = createWhiteLabel("Job Deadline (YYYY/MM/DD HH:MM:SS):");
        panel.add(deadlineLabel, gbc);
        deadlineField = new JTextField();
        gbc.gridx = 1; panel.add(deadlineField, gbc);

        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.addActionListener(e -> saveEntry());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 10, 10); 
        panel.add(submitBtn, gbc);

        gbc.gridy = 6; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    // 3. The Vehicle Registration Form (Tab 2)
    private JPanel createVehicleSubmissionTab(CloudDataService service) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        gbc.gridwidth = 1;
        panel.add(createWhiteLabel("Owner ID:"), gbc);
        JTextField ownerIdField = new JTextField();
        gbc.gridx = 1; panel.add(ownerIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createWhiteLabel("Vehicle ID:"), gbc);
        JTextField vehicleIdField = new JTextField();
        gbc.gridx = 1; panel.add(vehicleIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createWhiteLabel("Status Update:"), gbc);
        JTextField statusField = new JTextField();
        gbc.gridx = 1; panel.add(statusField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createWhiteLabel("Availability:"), gbc);
        JTextField availabilityField = new JTextField();
        gbc.gridx = 1; panel.add(availabilityField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton submitBtn = new JButton("Submit Vehicle to VC");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || vehicleIdField.getText().isBlank() || statusField.getText().isBlank() || availabilityField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Vehicle Owner fields.");
                return;
            }
            String entry = String.format("[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | STATUS:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                vehicleIdField.getText().trim(),
                statusField.getText().trim(),
                availabilityField.getText().trim());
            try {
                service.appendLog(entry);
                refreshMonitor("Vehicle Owner update submitted to VC:\n" + entry);
                JOptionPane.showMessageDialog(frame, "Vehicle Owner submission sent.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Unable to submit Vehicle Owner update.");
            }
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 5; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

//Show Screen method calls Panels - DH

    public void showScreen(JPanel contentPanel) {//DH
        frame.getContentPane().removeAll();
        frame.add(createHeader(), BorderLayout.NORTH);
        if (canViewVcrtsLogs()) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, createMonitorPanel());
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

    private JPanel createTaskOwnerScreen(CloudDataService service) {//DH
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
        gbc.gridx = 1;
        panel.add(ownerIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createWhiteLabel("Task Description:"), gbc);
        JTextField taskField = new JTextField();
        gbc.gridx = 1;
        panel.add(taskField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(createWhiteLabel("Target Vehicle ID:"), gbc);
        JTextField vehicleField = new JTextField();
        gbc.gridx = 1;
        panel.add(vehicleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(createWhiteLabel("Deadline (YYYY/MM/DD HH:MM:SS):"), gbc);
        JTextField taskDeadlineField = new JTextField();
        gbc.gridx = 1;
        panel.add(taskDeadlineField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton submitBtn = new JButton("Submit to VC");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || taskField.getText().isBlank()
                    || vehicleField.getText().isBlank()
                    || taskDeadlineField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Task Owner fields.");
                return;
            }
            String deadlineText = taskDeadlineField.getText().trim();
            try {
                LocalDateTime.parse(deadlineText, dtf);
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(frame, "Deadline must use format YYYY/MM/DD HH:MM:SS.");
                return;
            }
            String entry = String.format("[%s] ROLE:TASK_OWNER | ID:%s | TASK:%s | VEHICLE:%s | DEADLINE:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                taskField.getText().trim(),
                vehicleField.getText().trim(),
                deadlineText);
            try {
                service.appendLog(entry);
                refreshMonitor("Task Owner submitted to VC:\n" + entry);
                JOptionPane.showMessageDialog(frame, "Task Owner request submitted.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Unable to submit Task Owner request.");
            }
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 7;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    private JPanel createVehicleOwnerScreen(CloudDataService service) {//DH
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
        panel.add(createWhiteLabel("Availability:"), gbc);
        JTextField availabilityField = new JTextField();
        gbc.gridx = 1;
        panel.add(availabilityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton submitBtn = new JButton("Submit to VC");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> {
            if (ownerIdField.getText().isBlank() || vehicleIdField.getText().isBlank() ||  availabilityField.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Please complete all Vehicle Owner fields.");
                return;
            }
            String entry = String.format("[%s] ROLE:VEHICLE_OWNER | ID:%s | VEHICLE:%s | AVAILABILITY:%s",
                dtf.format(LocalDateTime.now()),
                ownerIdField.getText().trim(),
                vehicleIdField.getText().trim(),
                availabilityField.getText().trim());
            try {
                service.appendLog(entry);
                refreshMonitor("Vehicle Owner update submitted to VC:\n" + entry);
                JOptionPane.showMessageDialog(frame, "Vehicle Owner submission sent.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Unable to submit Vehicle Owner update.");
            }
        });
        panel.add(submitBtn, gbc);

        gbc.gridy = 6;
        panel.add(createBackToHomeButton(service), gbc);
        return panel;
    }

    private JButton createBackToHomeButton(CloudDataService service) {//DH
        JButton backBtn = new JButton("Back to Home");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        backBtn.addActionListener(e -> showScreen(createHomePanel(service)));
        return backBtn;
    }

    //VK- DH
    public void keypressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveEntry();
        }
    }

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

    // --- LOGIC METHODS (Migrated from ConsolePanel) ---

    private JLabel createWhiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    private void adjustFields() {
        String role = (String) roleBox.getSelectedItem();
        boolean isClient = "CLIENT".equals(role);

        idLabel.setText(isClient ? "Client ID:" : "Owner ID:");
        infoLabel.setText(isClient ? "Job Description:" : "Vehicle Info:");
        durLabel.setText(isClient ? "Job Duration (Hrs):" : "Residency (Hrs):");

        if (deadlineLabel != null) deadlineLabel.setVisible(isClient);
        if (deadlineField != null) deadlineField.setVisible(isClient);
        frame.revalidate(); // Refreshes the UI so hidden fields don't leave weird spaces
    }

    private void saveEntry() {
        String role = (String) roleBox.getSelectedItem();
        if ("ADMIN".equals(role)) {
            JOptionPane.showMessageDialog(frame, "Admin does not submit records. Use Calculate Completion Time.");
            return;
        }

        //Form input collected cleanly- TC
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
            refreshMonitor("Server response: " + ack + " - Pending approval...");

            JOptionPane.showMessageDialog(frame,
                "Your transaction has been submitted and is pending admin approval.\n"
                + "You will be notified when a decision is made.",
                "Transaction Submitted", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, "Deadline must use format yyyy/MM/dd HH:mm:ss.");
        } catch (java.net.ConnectException e) {
            JOptionPane.showMessageDialog(frame, "Cannot connect to VC Controller server.\nMake sure the server is running first.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection error: " + e.getMessage());
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

    private boolean canViewVcrtsLogs() {
        return isOwnerUser() || isAdminUser();
    }

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
            monitorArea.setCaretPosition(monitorArea.getDocument().getLength()); // Auto-scroll to bottom
        } catch (IOException ignored) {}
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

   private void refreshPendingAdminRequest() {
        if (pendingRequestsModel == null || adminRequestStatusLabel == null) return;

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
                
                if (id.equals(selectedId)) {
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
            JOptionPane.showMessageDialog(frame, "Please select a request from the table first.");
            return;
        }

        // Grab data from the selected row
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
            
            refreshPendingAdminRequest(); // Refresh the table immediately to clear the accepted/rejected row
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to send admin decision.");
        }
    }

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }

}
