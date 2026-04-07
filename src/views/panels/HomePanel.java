package views.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HomePanel extends JPanel {
    public HomePanel(
        Runnable openTaskOwner,
        Runnable openVehicleOwner,
        Runnable openAdmin
        ) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);

        JLabel title = new JLabel("Welcome to VCRTS");
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(title, gbc);

        JButton taskOwnerButton = new JButton("Task Owner Dashboard");
        taskOwnerButton.addActionListener(e -> openTaskOwner.run());
        gbc.gridy = 1;
        add(taskOwnerButton, gbc);

        JButton vehicleOwnerButton = new JButton("Vehicle Owner Dashboard");
        vehicleOwnerButton.addActionListener(e -> openVehicleOwner.run());
        gbc.gridy = 2;
        add(vehicleOwnerButton, gbc);

        JButton adminButton = new JButton("VC Controller Dashboard");
        adminButton.addActionListener(e -> openAdmin.run());
        gbc.gridy = 3;
        add(adminButton, gbc);
    }
}
