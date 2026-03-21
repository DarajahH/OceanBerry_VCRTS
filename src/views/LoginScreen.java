package views;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import services.CloudDataService;
import javax.swing.*;

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
                frame.dispose();
                new VCRTSDashboard(service); // Pass it down the chain
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid Credentials");
            }
        });

        regBtn.addActionListener(e -> {
            try {
                // USING INSTANCE METHOD
                service.registerUser(userField.getText(), new String(passField.getPassword()));
                JOptionPane.showMessageDialog(frame, "Account Created!");
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(frame, "Error saving user."); 
            }
        });

        // Center the frame on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
