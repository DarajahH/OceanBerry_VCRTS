package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import services.CloudDataService;
import services.RequestClientService;
import services.VCController;
import views.LoginScreen;



public final class Main {

    private Main() {
        // Prevent instantiation of the Main class
        throw new UnsuporttedOperationException("Main class cannot be instantiated");
    }
    public static void main(String[] args) {
        // Configure the look and feel - Evans Cortez
        configureLookAndFeel();

        // Modernize the UI lookand feel
         CloudDataService service = new CloudDataService(
            java.nio.file.Paths.get("vcrts_log.txt"), 
            java.nio.file.Paths.get("users.txt")
        );
        
        RequestClientService requestService = new RequestClientService();
        VCController controller = new VCController(dataService, requestService);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        // Launch the application
        SwingUtilities.invokeLater(() -> new LoginScreen(service)); // Changed from createConsole to LoginScreen for better user experience and functionality. -DH
    }
}

