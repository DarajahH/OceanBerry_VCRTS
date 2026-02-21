import javax.swing.*;



public class createPanel{
    public static void main(String[] args) {

        JFrame frame = new JFrame("Account Creation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        
        JPanel panel = new JPanel();
        panel.setBackground(new java.awt.Color(200, 220, 240));
        


        // Adding in text boxes for username and password

    JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(10, 15, 80, 30);
        panel.add(usernameLabel);
    JTextField usernameField = new JTextField(15);
        usernameField.setBounds(100, 15, 160, 30);
        panel.add(usernameField);  

    JLabel passwordLabel = new JLabel("Password:");
        panel.add(passwordLabel);
    JPasswordField passwordField = new JPasswordField(15);
        panel.add(passwordField);

        JButton loginButton = new JButton("Create Account");
        loginButton.setBounds(10, 50, 120, 30);
        panel.add(loginButton);





    frame.add(panel);
    frame.setVisible(true);

    }
}


