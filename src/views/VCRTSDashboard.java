package views;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import models.job.Job;
import services.CloudDataService;
import services.VCController;
import services.VCController.JobCompletionRecord;

public class VCRTSDashboard {

    private final JFrame frame;
    private final CloudDataService service;
    private final VCController controller;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final CardLayout cardLayout;
    private final JPanel leftCardContainer;

    // Form Components
    private JComboBox<String> roleBox;
    private JTextField idField, infoField, durField, deadlineField;
    private JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private JTextArea monitorArea;

    public VCRTSDashboard(CloudDataService service) {
       this.service = service;
        this.controller = new VCController(service);

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
        leftCardContainer.add(createSubmissionPanel(), "FORM_SCREEN");
        leftCardContainer.add(new JPanel(), "PLACEHOLDER"); // Placeholder for future panels like AdminScreen, Analytics, etc.
        leftCardContainer.setBackground(new Color(30, 30, 35));


        // 4. Create the Right Panel (Monitor)
        JPanel rightMonitorPanel = createMonitorPanel();

        // 5. Split Pane (Holds the Card Container on the left, Monitor on the right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCardContainer, rightMonitorPanel);
        splitPane.setDividerLocation(400); 
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        
        frame.add(splitPane, BorderLayout.CENTER);

        // Initialize state:
        adjustFields();
        refreshMonitor(null);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    // --- PANEL CREATION METHODS ---

    /*
    The Home Panel is a simple welcome screen with buttons to navigate to the submission form and to calculate completion times. 
    It serves as the landing page after login, providing a clear starting point for users. 
    The design is clean and minimalistic, with a focus on usability and quick access to key features while we work further through development.
    -DH
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

        JLabel subLabel = new JLabel("VCRTS HOME DASHBOARD");
        subLabel.setForeground(Color.GRAY);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        gbc.gridy = 1;
        gbc.weighty = 0.1;
        panel.add(subLabel, gbc);

        // Submission Button now brings users to the form screen instead of directly moving into submission Panel.

        JButton btnOpenForm = new JButton("Submit New Transaction");
        btnOpenForm.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnOpenForm.addActionListener(e -> {
            frame.getContentPane().removeAll();
            frame.add(createHeader(), BorderLayout.NORTH);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSubmissionPanel(), createMonitorPanel());
            splitPane.setDividerLocation(400);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            frame.add(splitPane, BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();
        });

        gbc.gridy = 2;
        gbc.weighty = 0.2;
        gbc.insets = new Insets(20, 0, 10, 0);
        panel.add(btnOpenForm, gbc);

/* Calculate Completion Times Button was left in for User efficiency 
 This will trigger the logic to calculate and display completion times based on existing job records. -DH
*/
        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());

        gbc.gridy = 3;
        gbc.weighty = 0.1;
        gbc.insets = new Insets(10, 0, 20, 0);
        panel.add(btnCalcTimes, gbc);

        JTextArea introMessage = new JTextArea(
            "VCRTS lets users submit jobs, store job data in files, and calculate FIFO completion times.\n\n"
            + "How to proceed:\n"
            + "1. Click \"Submit New Transaction\"\n"
            + "2. Enter Job ID, description, duration, and deadline\n"
            + "3. Submit the entry\n"
            + "4. Click \"Calculate Completion Times\" to view results"
        );
        introMessage.setEditable(false);
        introMessage.setLineWrap(true);
        introMessage.setWrapStyleWord(true);
        introMessage.setOpaque(false);
        introMessage.setForeground(Color.LIGHT_GRAY);
        introMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        introMessage.setBorder(null);

        gbc.gridy = 4;
        gbc.weighty = 0.3;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(introMessage, gbc);

        return panel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 15, 20));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("VCRTS CLOUD DASHBOARD");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            new LoginScreen(service);
        });
        header.add(logoutBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel createSubmissionPanel() { 
        
    //Renamed from createFormPanel to better reflect its purpose as the main interaction point for users 
    // to submit new transactions and jobs. -DH
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        // Role Selection
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createWhiteLabel("Select Role:"), gbc);
        
        roleBox = new JComboBox<>(new String[]{"OWNER", "CLIENT", "ADMIN"});
        roleBox.addActionListener(e -> adjustFields());
        gbc.gridx = 1; 
        panel.add(roleBox, gbc);

        // Input Fields
        gbc.gridx = 0; gbc.gridy = 1;
        idLabel = createWhiteLabel("Owner ID:");
        panel.add(idLabel, gbc);
        idField = new JTextField();
        gbc.gridx = 1; panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoLabel = createWhiteLabel("Vehicle Info:");
        panel.add(infoLabel, gbc);
        infoField = new JTextField();
        gbc.gridx = 1; panel.add(infoField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        durLabel = createWhiteLabel("Residency (Hrs):");
        panel.add(durLabel, gbc);
        durField = new JTextField("Please enter in the expected duration in hours. For example, '2' for 2 hours.");
        gbc.gridx = 1; panel.add(durField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        deadlineLabel = createWhiteLabel("Job Deadline (YYYY-MM-DD HR:MM:SS):");
        panel.add(deadlineLabel, gbc);
        deadlineField = new JTextField();
        gbc.gridx = 1; panel.add(deadlineField, gbc);

        // Buttons
        JButton submitBtn = new JButton("Submit Transaction");
        submitBtn.addActionListener(e -> saveEntry());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 10, 10); // Extra top padding
        panel.add(submitBtn, gbc);

        JButton calcBtn = new JButton("Calculate Completion Time");
        calcBtn.addActionListener(e -> calculateCompletionTimes());
        gbc.gridy = 6; gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(calcBtn, gbc);

        /*  Home Button to return to the main dashboard/home screen 
            without needing to log out and back in. -DH
        */
        JButton homeBtn = new JButton("Back to Home");
        homeBtn.addActionListener(e -> { 
            frame.getContentPane().removeAll();
            frame.add(createHeader(), BorderLayout.NORTH);
            frame.add(createHomePanel(service), BorderLayout.CENTER);
            frame.revalidate();
            frame.repaint();
        });
        gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(homeBtn, gbc);


        // Spacer to push everything to the top
        gbc.gridy = 7; gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

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

        deadlineLabel.setVisible(isClient);
        deadlineField.setVisible(isClient);
        frame.revalidate(); // Refreshes the UI so hidden fields don't leave weird spaces
    }

    private boolean requestApproval(String entry) {
        //: Step 1: ACK (Acknowledgment)
        JOptionPane.showMessageDialog(
            frame,
            "ACK: Request received by VC contoller. \n\nPending approval.",
            "Resquest Acknowledged",
            JOptionPane.INFORMATION_MESSAGE
        );

        //Step 2: VC Controller decision 
        String[] options = {"Accept", "Reject"};

        int decision = JOptionPane.showOptionDialog(
            frame,
            "VC Controller Review:\n\n" + entry + "\n\nAccept or Reject this request?",
            "VC Controller Decision",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        return decision == 0; // Accept = true
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

        if ("CLIENT".equals(role) && deadline.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Client jobs must include a deadline.");
            return;
        }

        try {
            LocalDateTime arrivalTime = LocalDateTime.now();
            LocalDateTime deadlineTime = deadlineField.isVisible()
                ? LocalDateTime.parse(deadline, dtf)
                : null;
            String formattedDeadline = deadlineTime == null ? "N/A" : dtf.format(deadlineTime);
            String entry = String.format("[%s] ROLE:%s | ID:%s | INFO:%s | DURATION:%d | DEADLINE:%s",
                dtf.format(arrivalTime), role, id, info, duration, formattedDeadline);

            // Send this fucker to VC Controller server over socket
            refreshMonitor("Connecting to VC Controller server...");

            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            // Client sends the entry to server
            outputStream.writeUTF(entry);

            // Client should read acknowledge from server
            String ack = inputStream.readUTF();
            refreshMonitor("Server response: " + ack + " - Pending approval...");

            // Client reads the final decision from server(accept or nah)
            String decision = inputStream.readUTF();

            if ("ACCEPTED".equals(decision)) {
                refreshMonitor("FINAL STATUS: ACCEPTED\n\nSaved entry:\n" + entry);
                clear();
            } else {
                refreshMonitor("FINAL STATUS: REJECTED\n\nRejected entry:\n" + entry);
                JOptionPane.showMessageDialog(frame, "Request rejected by VC Controller. Nothing was saved.");
            }

            // Connection close
            inputStream.close();
            outputStream.close();
            socket.close();

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
                refreshMonitor("No client jobs found.");
                return;
            }

            StringBuilder results = new StringBuilder();
            results.append("FIFO Completion Times\n---------------------\n");
            for (JobCompletionRecord record : records) {
                results.append(record.toDisplayString()).append("\n");
            }
            refreshMonitor(results.toString().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error calculating completion times.");
        }
    }

    private void refreshMonitor(String resultSection) {
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

    private void clear() {
        idField.setText(""); infoField.setText(""); durField.setText(""); deadlineField.setText("");
    }
}
