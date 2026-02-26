package app;

import panels.LoginScreen;
import services.AuthService;
import services.JobService;
import services.VehicleService;

import javax.swing.*;
import java.lang.reflect.Method;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        configureLookAndFeel();
        AuthService authService = AuthService.withSeedUsers();
        JobService jobService = new JobService();
        VehicleService vehicleService = new VehicleService();
        SwingUtilities.invokeLater(() -> new LoginScreen(authService, jobService, vehicleService));
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
