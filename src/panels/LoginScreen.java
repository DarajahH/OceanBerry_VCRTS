package panels;

import javax.swing.*;
import java.awt.*;

public class LoginScreen {
    private final JFrame frame;

    public LoginScreen() {
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
                "It allows owners to register vehicles, clients to submit jobs,<br>" +
                "and administrators to monitor transactions.<br><br>" +
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
        loginBtn.setBounds(160, 180, 120, 40);
        loginBtn.setBackground(new Color(52, 199, 89));
        loginBtn.setForeground(Color.BLACK);
        loginBtn.setFocusPainted(false);
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());

            if ("admin".equals(user) && "1234".equals(pass)) {
                frame.dispose();
                new createPanel();
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Invalid Credentials",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
