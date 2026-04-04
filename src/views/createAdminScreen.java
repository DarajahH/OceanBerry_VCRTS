package views;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import services.CloudDataService;
import services.VCController;



public class createAdminScreen extends JPanel {

    private final CloudDataService service;
    private VCController controller;

    public createAdminScreen(CloudDataService service) {
        this.service = service;
        setLayout(new GridBagLayout());
        setBackground(new Color(30, 30, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel titleLabel = new JLabel("Admin Portal");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, gbc);
        gbc.gridy++;

        VCController controllerAS = new VCController(service);

        JButton viewJobsBtn = new JButton("View Job Completion Times");
        viewJobsBtn.addActionListener(e -> {
            try {
              controllerAS.calculateCompletionTimes();
            } catch (IOException ex) {
                System.getLogger(createAdminScreen.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });
        add(viewJobsBtn, gbc);
        

        }


}
