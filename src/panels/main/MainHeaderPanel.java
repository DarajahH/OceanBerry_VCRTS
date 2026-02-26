package panels.main;

import models.User;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainHeaderPanel extends JPanel {
    public MainHeaderPanel(User user, Runnable onLogout) {
        setOpaque(false);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("VCRTS CLOUD CONTROL CENTER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Helvetica Neue", Font.BOLD, 28));

        JLabel clock = new JLabel();
        clock.setForeground(Color.CYAN);
        clock.setFont(new Font("Helvetica Neue", Font.BOLD, 18));

        String userText = "Logged in: " + user.getUsername() + " (" + formatRole(user.getRole().name()) + ")";
        JLabel userLabel = new JLabel(userText);
        userLabel.setForeground(new Color(200, 200, 200));
        userLabel.setFont(new Font("Helvetica Neue", Font.PLAIN, 13));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> onLogout.run());

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        topRight.add(userLabel, BorderLayout.CENTER);
        topRight.add(logoutButton, BorderLayout.EAST);

        right.add(topRight, BorderLayout.NORTH);
        right.add(clock, BorderLayout.SOUTH);

        add(title, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timer.start();
        clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private String formatRole(String roleName) {
        return roleName.replace('_', ' ').toLowerCase();
    }
}
