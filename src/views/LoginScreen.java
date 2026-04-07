package views;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import services.CloudDataService;
import services.RequestClientService;
import services.VCController;
public class LoginScreen {

    private final CloudDataService service;
    private final RequestClientService requestClientService;
    private final VCController controller;
    private final JFrame frame;

    public LoginScreen(CloudDataService service) {

        this.service = service;
        frame = new JFrame("VCRTS Login Portal");
        frame.setSize(450, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Create Account");

        gbc.gridx = 0; 
        gbc.gridy = 0;
        frame.add(userField, gbc);

        gbc.gridy = 1; 
        frame.add(passField, gbc);
        gbc.gridy = 2; 
        frame.add(loginBtn, gbc);
        gbc.gridy = 3; 
        frame.add(regBtn, gbc);

      loginBtn.addActionListener(e -> {
            // USING INSTANCE METHOD
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (dataService.validateUser(username, password)) {
                frame.dispose();
                new VCRTSDashboard(dataService, requestClientService, controller);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials.");
            }
        });


        regBtn.addActionListener(e -> {
                        String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Username and password cannot be blank.");
                return;
            }

            try {
                dataService.registerUser(username, password);
                JOptionPane.showMessageDialog(frame, "Account created.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error saving user.");
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
