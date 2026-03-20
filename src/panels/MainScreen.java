package panels;

import java.time.format.DateTimeFormatter;

import javax.swing.*;

import services.CloudDataService;

public class MainScreen {

    private final JFrame frame;
    private final ConsolePanel consolePanel;

 // private final JTextField idField, infoField, durField, deadlineField;
  // private final JLabel idLabel, infoLabel, durLabel, deadlineLabel;
    private final JComboBox<String> roleBox;
    private final JTextArea monitorArea;
    private final CloudDataService service;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public MainScreen() {

       
        this.service = new CloudDataService(Paths.get("vcrts_log.txt"));
        

        consolePanel = new ConsolePanel();


        frame = new JFrame("OceanBerry");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
}
