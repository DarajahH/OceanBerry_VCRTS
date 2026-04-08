package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import services.CloudDataService;
import views.LoginScreen;



public final class Main {
    public static void main(String[] args) {
        // Configure the look and feel - Evans Cortez
        configureLookAndFeel();

        // Modernize the UI lookand feel
         CloudDataService service = new CloudDataService(
            java.nio.file.Paths.get("vcrts_log.txt"), 
            java.nio.file.Paths.get("users.txt")
        );
        
        // Launch the application
        SwingUtilities.invokeLater(() -> new LoginScreen(service)); // Changed from createConsole to LoginScreen for better user experience and functionality. -DH
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
        }
    }
}

