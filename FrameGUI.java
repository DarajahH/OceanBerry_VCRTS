import java.swing.*;

class PanelExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("JPanel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        
        JPanel panel = new JPanel();
        panel.setBackground(new java.awt.Color(200, 220, 240));
        
        JLabel label = new JLabel("Login:");
        panel.add(label);
        
        frame.add(panel);
        frame.setVisible(true);


        // Adding in text boxes for username and password
        JTextField usernameField = new JTextField(15);

        panel.add(usernameField);  

        JLabel passwordLabel = new JLabel("Password:");
        panel.add(passwordLabel);

        JPasswordField passwordField = new JPasswordField(15);
        panel.add(passwordField);

    }
}


