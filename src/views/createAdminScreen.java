/*package views;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import services.CloudDataService;
import services.VCController;
import views.VCRTSDashboard;



public class createAdminScreen extends JPanel {

    public CloudDataService service;
    public VCController controller;
    public final JPanel adminPanel = new JPanel();


    public createAdminScreen(CloudDataService service) {

        this.service = service;
    add(adminPanel);

        adminPanel.setBackground(new Color(30, 2, 35));
        adminPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

// GridBagConstraints for layout management

        adminPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(30, 30, 30, 10);
        
        // Title label for the admin screen
        JLabel titleLabel = new JLabel("Admin Screen");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(10, 10, 23));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        adminPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(20, 30, 20, 10);
        // Adjusting spacing for the button

        VCController controllerAP = new VCController(service);

        JButton viewJobsBtn = new JButton("View Job Completion Times");
        viewJobsBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        viewJobsBtn.setBackground(new Color(70, 130, 180));
        viewJobsBtn.setForeground(Color.RED);
        viewJobsBtn.addActionListener(e -> {
            try {
              controllerAP.calculateCompletionTimes();
            } catch (IOException ex) {
                System.getLogger(createAdminScreen.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });

        gbc.gridy = 2; // Positioning the button below the title with some spacing
        gbc.insets = new Insets(20, 30, 20, 10);
        add(viewJobsBtn, gbc);
        
        JButton btnCalcTimes = new JButton("Calculate Completion Times");
        btnCalcTimes.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCalcTimes.addActionListener(e -> calculateCompletionTimes());

        gbc.weighty = 0.1;
        gbc.insets = new Insets(10, 0, 20, 0); 
        gbc.gridy = 3; adminPanel.add(btnCalcTimes, gbc);

    JButton Accept = new JButton("Accept Job");
    Accept.addActionListener(e -> {
        // Logic to accept a job goes here
        JOptionPane.showMessageDialog(adminPanel, "Job Accepted!");
    });
    adminPanel.add(Accept);

    JButton Reject = new JButton("Reject Job");
    Reject.addActionListener(e -> {
        // Logic to reject a job goes here
        JOptionPane.showMessageDialog(adminPanel, "Job Rejected!");
    });
    adminPanel.add(Reject);

    }

    

}
*/