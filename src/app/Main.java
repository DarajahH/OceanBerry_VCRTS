package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import views.ConsolePanel;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(ConsolePanel::new);
    }
}
