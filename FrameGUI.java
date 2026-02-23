import javax.swing.*;
import java.awt.event.ActionListener;

public class PanelExample {   // removed "extends frame"

    private String ownerName;        // CHANGED: string -> String
    private int ownerID;
    private String ownerPassword;    // CHANGED -> Capitalized

    private String userName;         // CHANGED -> Capitalized
    private int userID;
    private String userPassword;     // CHANGED -> Capitalized

    public static void main(String[] args) {
        JFrame frame = new JFrame("JPanel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);

        JPanel panel = new JPanel();
        panel.setBackground(new java.awt.Color(200, 220, 240));

        JLabel label = new JLabel("Login:");
        panel.add(label);

        // Adding in text boxes for username and password creation
        JLabel usernameLabel = new JLabel("Username:");   // renamed so not duplicated
        panel.add(usernameLabel);

        JTextField usernameField = new JTextField(15);
        panel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");   // only one passwordLabel now
        panel.add(passwordLabel);

        JPasswordField passwordField = new JPasswordField(15);
        panel.add(passwordField);

        // call createButton correctly (static + pass what it needs)
        createButton(panel, frame);

        frame.add(panel);
        frame.setVisible(true);
    }

    //  made static + pass panel/frame so it can add button + show message
    private static void createButton(JPanel panel, JFrame frame) {
        JButton button = new JButton("Create Account");   // declared button

        // replaced missing AddInterestListener with simple working listener
        ActionListener listener = e ->
                JOptionPane.showMessageDialog(frame, "Account Created!");

        button.addActionListener(listener);

        panel.add(button); // add button to panel so it appears
    }
}



