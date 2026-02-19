import java.swing.*;

public class PanelExample extends frame {

private string ownerName;
private int ownerID;
private string ownerPassword;
    
private string userName;
private int userID;
private string userPassword;

    
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


        // Adding in text boxes for username and password creation
        
        JLabel passwordLabel = new JLabel("Username:");
        panel.add(passwordLabel);
        
        JTextField usernameField = new JTextField(15);

        panel.add(usernameField);  

        JLabel passwordLabel = new JLabel("Password:");
        panel.add(passwordLabel);

        JPasswordField passwordField = new JPasswordField(15);
        panel.add(passwordField);

        createButton();
        
    }

    
 // Create button
   private void createButton()
   {
      button = new JButton("Create Account");
      
      ActionListener listener = new AddInterestListener();
      button.addActionListener(listener);
      
   }

    private

    }
}


