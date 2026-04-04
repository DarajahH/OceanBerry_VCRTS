package views;

import java.awt.*;
import javax.swing.*;
import services.CloudDataService;

//Being Changed to a Panel to be added to the Dashboard, will be used for admin functions like user management and log viewing. -DH

public class AdminScreen {

    private final CloudDataService service;
    private final JPanel frame;

    public AdminScreen(CloudDataService service) {
        this.service = service;
        frame = new JPanel();
        frame.setSize(450, 400);
        frame.setLayout(new GridBagLayout());
        frame.setBackground(new Color(30, 30, 35));

    }



}
