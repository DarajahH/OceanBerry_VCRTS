package panels;

import models.Role;
import models.User;
import services.AuthService;
import services.JobService;
import services.VehicleService;

/**
 * Backward-compatible wrapper.
 */
public class createPanel {
    public createPanel() {
        AuthService authService = AuthService.withSeedUsers();
        JobService jobService = new JobService();
        VehicleService vehicleService = new VehicleService();
        MainScreen mainScreen = new MainScreen(new User("legacy", "legacy", "", Role.SYSTEM_ADMIN), authService, jobService, vehicleService);
    }
}
