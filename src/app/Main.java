package app;

import panels.LoginScreen;

import javax.swing.*;
import java.lang.reflect.Method;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        configureLookAndFeel();
        SwingUtilities.invokeLater(LoginScreen::new);
    }

    private static void configureLookAndFeel() {
        try {
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Method setup = lafClass.getMethod("setup");
            setup.invoke(null);
        } catch (Throwable ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception alsoIgnored) {
                // fallback to Swing default
            }
        }
    }
}
