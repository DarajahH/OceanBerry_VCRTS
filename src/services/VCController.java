package services;

import models.job.Job;
import models.job.JobAssignment;
import models.vehicle.Vehicle;

public class VCController {
    private String controllerId;
    private String name;
    private String checkpoint;

    public JobAssignment creatJobAssignment(Job job) {
        if (job == null) {
            return null;
        }
        return new JobAssignment(job, "UNASSIGNED");
    }

    public Vehicle recruitVehicle() {
        return new Vehicle();
    }

    public void transferFromJob(Job job) {
    }

    public void setFileRedundancyLevel(int redundancyLevel) {
    }
}
