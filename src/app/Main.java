package app;

<<<<<<< Updated upstream
import javax.swing.*;
import panels.LoginScreen;
=======
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import views.VCRTSDashboard;


>>>>>>> Stashed changes

public final class Main {
    public static void main(String[] args) {
        // Modernize the UI look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
<<<<<<< Updated upstream
        } catch (Exception ignored) {}
        
        // Launch the application
=======
        } catch (Exception ignored) {
            System.err.println("Failed to set look and feel, using default.");
        }

>>>>>>> Stashed changes
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}

