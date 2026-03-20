package models.job;

public class JobAssignment {
    private final Job job;
    private final String assignedVehicleId;

    public JobAssignment(Job job, String assignedVehicleId) {
        this.job = job;
        this.assignedVehicleId = assignedVehicleId;
    }

    public Job getJob() {
        return job;
    }

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }
}
