package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import services.CloudDataService;
import views.LoginScreen;  



public final class Main {
    public static void main(String[] args) {
        // Modernize the UI lookand feel
         CloudDataService service = new CloudDataService(
            java.nio.file.Paths.get("vcrts_log.txt"), 
            java.nio.file.Paths.get("users.txt")
        );try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        // Launch the application
        SwingUtilities.invokeLater(() -> new LoginScreen(service)); // Changed from createConsole to LoginScreen for better user experience and functionality. -DH
    }
}

