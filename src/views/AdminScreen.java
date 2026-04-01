package views;

import views.VCRTSDashboard;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import services.CloudDataService;
import javax.swing.*;
import java.awt.event.KeyEvent;


public class AdminScreen {

    private final CloudDataService service;
    private final JFrame frame;

    public AdminScreen(CloudDataService service) {
        this.service = service;
        frame = new JFrame("VCRTS Admin Portal");
        frame.setSize(450, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 35));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);



    }



}
