package services;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import database.DatabaseConnection;

import java.time.format.DateTimeFormatter;
import models.job.Job;
import models.vehicle.Vehicle;

public class VCController {
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

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

        int runningCompletionTime = 0;
        List<Job> jobs = new ArrayList<>(dataService.readJobs());

        jobs.sort(Comparator.comparing(Job::getArrivalTime, Comparator.nullsLast(Comparator.naturalOrder())));


        for (Job job : jobs) {
            runningCompletionTime += job.getDuration();
            job.setCompletionTime(runningCompletionTime);

            completionRecords.add(new JobCompletionRecord(
                job.getJobId(),
                job.getDescription(),
                job.getDuration(),
                formatDeadline(job),
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

        public String getJobId() {
            return jobId;
        }

        public String getInfo() {
            return info;
        }

        public int getResidencyTimeHours() {
            return duration;
        }

        public String getDeadline() {
            return deadline;
        }

        public int getCompletionTime() {
            return completionTime;
        }
    }

    private String formatDeadline(Job job) {
        if (job.getDeadline() == null) {
            return "N/A";
        }
        return DISPLAY_DATE_TIME.format(job.getDeadline());
    }

}
