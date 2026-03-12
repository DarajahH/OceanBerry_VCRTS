package panels;

import java.awt.*;
import java.util.Optional;
import javax.swing.*;
import models.Role;
import models.User;
import services.AuthService;
import services.JobService;
import services.VehicleService;

public class LoginScreen {
    private final JFrame frame;
    private final AuthService authService;
    private final JobService jobService;
    private final VehicleService vehicleService;

    public LoginScreen() {
        this(AuthService.withSeedUsers(), new JobService(), new VehicleService());
    }

    public LoginScreen(AuthService authService) {
        this(authService, new JobService(), new VehicleService());
    }

    public LoginScreen(AuthService authService, JobService jobService) {
        this(authService, jobService, new VehicleService());
    }

    public LoginScreen(AuthService authService, JobService jobService, VehicleService vehicleService) {
        this.authService = authService;
        this.jobService = jobService;
        this.vehicleService = vehicleService;
        frame = new JFrame("VCRTS Login Portal");
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(18, 18, 18));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(15, 15, getWidth() - 30, getHeight() - 30, 30, 30);
                g2.dispose();
            }
        };

        panel.setBounds(0, 0, 700, 600);
        panel.setOpaque(false);
        panel.setLayout(null);

        JLabel title = new JLabel("VCRTS LOGIN");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBounds(140, 30, 200, 30);
        panel.add(title);

        JLabel description = new JLabel(
                "<html>VCRTS is a vehicular cloud transaction console system.<br>" +
                "It allows vehicle owners to register up to 3 vehicles,<br>" +
                "job submitters to create and track jobs, job controllers<br>" +
                "to adjust queue order, and system admins to manage all.<br><br>" +
                "This project demonstrates GUI interaction and persistent<br>" +
                "transaction logging using file storage.</html>"
        );

        description.setForeground(new Color(200, 200, 200));
        description.setFont(new Font("Arial", Font.PLAIN, 13));
        description.setBounds(380, 90, 280, 200);
        panel.add(description);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(new Color(200, 200, 200));
        userLabel.setBounds(60, 90, 100, 30);
        panel.add(userLabel);

        JTextField userField = new JTextField();
        userField.setBounds(160, 90, 180, 30);
        panel.add(userField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(new Color(200, 200, 200));
        passLabel.setBounds(60, 130, 100, 30);
        panel.add(passLabel);

        JPasswordField passField = new JPasswordField();
        passField.setBounds(160, 130, 180, 30);
        panel.add(passField);

        JButton loginBtn = new JButton("SIGN IN");
        loginBtn.setBounds(160, 180, 180, 40);
        loginBtn.setBackground(new Color(52, 199, 89));
        loginBtn.setForeground(Color.BLACK);
        loginBtn.setFocusPainted(false);
        panel.add(loginBtn);

        JButton registerBtn = new JButton("REGISTER");
        registerBtn.setBounds(160, 230, 180, 40);
        registerBtn.setBackground(new Color(10, 132, 255));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        panel.add(registerBtn);

        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());

            Optional<User> authenticated = authService.authenticate(user, pass);
            if (authenticated.isPresent()) {
                frame.dispose();
                new MainScreen(authenticated.get(), authService, jobService, vehicleService);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Invalid Credentials",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> showRegistrationDialog(userField, passField));

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showRegistrationDialog(JTextField loginUserField, JPasswordField loginPassField) {
        JTextField usernameField = new JTextField(16);
        JPasswordField passwordField = new JPasswordField(16);
        JPasswordField confirmField = new JPasswordField(16);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{
                "Client (Job Submitter)",
                "Vehicle Owner"
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        form.add(usernameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        form.add(confirmField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Register As:"), gbc);
        gbc.gridx = 1;
        form.add(roleCombo, gbc);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    form,
                    "Create Account",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All registration fields are required.");
                continue;
            }

            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(frame, "Passwords do not match.");
                continue;
            }

            Role selectedRole = "Vehicle Owner".equals(roleCombo.getSelectedItem())
                    ? Role.VEHICLE_OWNER
                    : Role.JOB_SUBMITTER;

            boolean created = authService.addUser(
                    authService.nextUserId(),
                    username,
                    password,
                    selectedRole);

            if (!created) {
                JOptionPane.showMessageDialog(frame, "Could not create account. Username may already exist.");
                continue;
            }

            loginUserField.setText(username);
            loginPassField.setText(password);
            JOptionPane.showMessageDialog(frame, "Account created. You can now sign in.");
            return;
        }
    }
}
