package app;

import java.nio.file.Paths;
import javax.swing.*;
import services.CloudDataService;
import views.ConsolePanel;
public final class Main {
    public static void main(String[] args) {
        // Modernize the UI look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        // Initialize the data service with both log and user file paths
        CloudDataService dataService = new CloudDataService(
            Paths.get("vcrts_log.txt"), 
            Paths.get("users.txt")
        );

        // Launch the application
        SwingUtilities.invokeLater(() -> new ConsolePanel(dataService));
<<<<<<< HEAD
        }
=======
    }
>>>>>>> ba607fc5420e109dee03bf5620f8b143225b43b6
}

