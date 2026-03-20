package app;


import javax.swing.*;
import views.ConsolePanel;


public final class Main {
    public static void main(String[] args) {
        // Modernize the UI look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
             // Launch the application

        SwingUtilities.invokeLater(() -> new ConsolePanel(dataService));
        }
    


