package app;

import javax.swing.SwingUtilities;
import services.CloudDataService;
import views.LoginScreen;



public final class Main {
    public static void main(String[] args) {
        CloudDataService service = new CloudDataService();
        ThemeWrapper.apply();
        
        // Launch the application
        SwingUtilities.invokeLater(() -> new LoginScreen(service)); // Changed from createConsole to LoginScreen for better 
                                                                    // user experience and functionality. -DH
    }
}
