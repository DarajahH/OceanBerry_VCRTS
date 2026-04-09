package views;

import java.awt.*;
import javax.swing.*;
import services.CloudDataService;

public class LoginScreen {

    private final CloudDataService service;
    private final JFrame frame;

    public LoginScreen(CloudDataService service) {

        this.service = service;
        frame = new JFrame("VCRTS Login Portal");
        frame.setSize(450, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"CLIENT", "OWNER", "ADMIN"});
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Create Account");

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel title = new JLabel("VEHICULAR CLOUD LOGIN");
        title.setForeground(Color.CYAN);
        frame.add(title, gbc);

        gbc.gridy = 1; frame.add(new JLabel("<html><font color='white'>Username:</font></html>"), gbc);
        gbc.gridy = 2; frame.add(userField, gbc);
        gbc.gridy = 3; frame.add(new JLabel("<html><font color='white'>Password:</font></html>"), gbc);
        gbc.gridy = 4; frame.add(passField, gbc);
        gbc.gridy = 5; frame.add(loginBtn, gbc);
        gbc.gridy = 6; frame.add(regBtn, gbc);

      loginBtn.addActionListener(e -> {
            // USING INSTANCE METHOD
            if (service.validateUser(userField.getText(), new String(passField.getPassword()))) {

                //DH -- Validates role for user-specific welcome message
                String role = service.getCurrentUserRole();
                String welcomeMessage;
                switch (role) {
                    case "ADMIN":
                        welcomeMessage = "Welcome, Admin!\n\n" +
                            "Use the Admin Screen to review pending client requests, " +
                            "accept or reject jobs, and calculate completion times.";
                        break;
                    case "OWNER":
                        welcomeMessage = "Welcome, Owner!\n\n" +
                            "Use the Task Owner Portal to submit tasks and the Vehicle Owner Portal " +
                            "to update vehicle status and availability.";
                        break;
                    default:
                        welcomeMessage = "Welcome, Client!\n\n" +
                            "Use Submit New Transaction to send a job request and then " +
                            "calculate completion times once the request is processed.";
                        break;
                }
                JOptionPane.showMessageDialog(frame, welcomeMessage, "Welcome to VCRTS", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                new VCRTSDashboard(service, role); // Pass authenticated role to dashboard
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid Credentials");
            };


        });

        regBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Username and password cannot be blank.");
                return;
            }

            String[] roles = {"CLIENT", "OWNER", "ADMIN"};
            String role = (String) JOptionPane.showInputDialog(
                frame, "Select your account role:", "Account Role",
                JOptionPane.QUESTION_MESSAGE, null, roles, roles[0]);
            if (role == null) return;

            try {
                service.registerUser(username, password, role);
                JOptionPane.showMessageDialog(frame, "Account Created!");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(frame, "Error saving user."); 
            };

            
        });

        // Center the frame on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
