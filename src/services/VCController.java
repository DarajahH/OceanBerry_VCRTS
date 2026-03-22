package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.job.Job;
import models.vehicle.Vehicle;

public class VCController {

    private String controllerId;
    private String name;
    private String checkpoint;
    private final CloudDataService dataService;

    // Constructor to initialize the VCController with a reference to the CloudDataService for data operations. EC
    public VCController(CloudDataService dataService) {
        this.dataService = dataService;
    }

    // Method to recruit a new vehicle, currently returns a new Vehicle instance with default values. In a real implementation, this would involve more complex logic and data handling. EC
    public Vehicle recruitVehicle() {
        return new Vehicle();
    }

    // Method to transfer job details to the controller, currently a placeholder. In a real implementation, this would involve logic to assign jobs to vehicles and manage their execution. EC
    public void transferFromJob(Job job) {
    }

    // Method to set the file redundancy level, currently a placeholder. In a real implementation, this would involve logic to adjust how data is stored and replicated for fault tolerance. EC
    public void setFileRedundancyLevel(int redundancyLevel) {
    }

      //Completion time calculation based on job durations and deadlines. EC

    public List<JobCompletionRecord> calculateCompletionTimes() throws IOException {
        
        List<JobCompletionRecord> completionRecords = new ArrayList<>();
        if (dataService == null) {
            return completionRecords;
        }

        // This is a simplified example of how completion times might be calculated. In a real implementation, this would involve more complex logic to consider job dependencies, vehicle availability, and other factors. EC
        int runningCompletionTime = 0;
        // Read job records from the data service and calculate completion times based on durations and deadlines. This is a simplified example; real logic would be more complex and consider various factors. EC
        List<Map<String, String>> jobs = dataService.readClientJobRecords();


        for (Map<String, String> job : jobs) {

            int duration = Integer.parseInt(job.getOrDefault("DURATION", "0"));
            runningCompletionTime += duration;

            completionRecords.add(new JobCompletionRecord(
                job.getOrDefault("ID", ""),
                job.getOrDefault("INFO", ""),
                duration,
                job.getOrDefault("DEADLINE", "N/A"),
                runningCompletionTime
            ));
        }

        return completionRecords;
    }

    //A static inner class to represent job completion records for display purposes. EC

    public static final class JobCompletionRecord {
        private final String jobId;
        private final String info;
        private final int duration;
        private final String deadline;
        private final int completionTime;

        public JobCompletionRecord(
            String jobId,
            String info,
            int duration,
            String deadline,
            int completionTime
        ) {
            this.jobId = jobId;
            this.info = info;
            this.duration = duration;
            this.deadline = deadline;
            this.completionTime = completionTime;
        }

        public String toDisplayString() {
            return String.format(
                "Job ID: %s | Description: %s | Duration: %d | Deadline: %s | Completion Time: %d",
                jobId,
                info,
                duration,
                deadline,
                completionTime
            );
        }
    }
}
