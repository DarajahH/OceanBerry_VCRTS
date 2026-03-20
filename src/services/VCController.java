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

    public VCController() {
        this(null);
    }

    public VCController(CloudDataService dataService) {
        this.dataService = dataService;
    }

    public List<JobCompletionRecord> calculateCompletionTimes() throws IOException {
        List<JobCompletionRecord> completionRecords = new ArrayList<>();
        if (dataService == null) {
            return completionRecords;
        }

        int runningCompletionTime = 0;
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

    public Vehicle recruitVehicle() {
        return new Vehicle();
    }

    public void transferFromJob(Job job) {
    }

    public void setFileRedundancyLevel(int redundancyLevel) {
    }

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
