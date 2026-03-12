package models;

import java.util.EnumSet;
import java.util.Set;

public class Admin extends User {

    private final Set<Permission> permissions;

    public Admin(String userId, String username, String password) {
        // Automatically assign the SYSTEM_ADMIN role when an Admin is instantiated
        super(userId, username, password, Role.SYSTEM_ADMIN);
        
        // Grant admin-specific permissions based on Permission.java
        this.permissions = EnumSet.of(
            Permission.MANAGE_USERS,
            Permission.MANAGE_VEHICLES,
            Permission.MANAGE_JOBS,
            Permission.VIEW_ALL_JOBS,
            Permission.ADJUST_JOB_QUEUE
        );
    }

    /**
     * Retrieves the set of permissions granted to this admin.
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Checks if the admin has a specific permission.
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public void banUser(User user) {
        // Disable or remove a user
        System.out.println("Admin " + this.getUsername() + " banned user: " + user.getUsername());
    }

    public void cancelJob(Job job) {
        // Forcibly cancel a job and update its status
        job.setStatus(JobStatus.FAILED);
        job.addReportLine("Job forcefully cancelled by System Admin: " + this.getUsername());
    }

    public void overrideJobPriority(Job job, int newPriority) {

        job.setPriorityRank(newPriority);
    }
}