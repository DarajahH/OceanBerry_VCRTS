package services;

import models.Job;
import models.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JobService {
    private final List<Job> jobs = new ArrayList<>();

    public synchronized Job createClientJob(
            String clientId,
            String description,
            int approxDurationHours,
            LocalDateTime deadline,
            BigDecimal costForJob) {
        String jobId = String.format("J-%04d", jobs.size() + 1);
        Job job = new Job(
                jobId,
                clientId,
                description,
                approxDurationHours,
                deadline,
                costForJob,
                0,
                0,
                JobStatus.QUEUED,
                LocalDateTime.now(),
                nextPriorityRank());

        jobs.add(job);
        return job;
    }

    public synchronized List<Job> getJobsForClient(String clientId) {
        return jobs.stream()
                .filter(job -> job.getClientId().equals(clientId))
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized List<Job> getAllJobs() {
        return new ArrayList<>(jobs);
    }

    public synchronized Job createJobOwnerSubmission(
            String jobOwnerId,
            String description,
            int approxDurationHours,
            LocalDateTime deadline,
            BigDecimal submissionCost) {
        String jobId = String.format("J-%04d", jobs.size() + 1);
        Job job = new Job(
                jobId,
                jobOwnerId,
                description,
                approxDurationHours,
                deadline,
                submissionCost,
                0,
                0,
                JobStatus.QUEUED,
                LocalDateTime.now(),
                nextPriorityRank());

        jobs.add(job);
        return job;
    }

    public synchronized List<Job> getJobsForSubmitter(String userId) {
        return jobs.stream()
                .filter(job -> job.getClientId().equals(userId))
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Optional<Job> findById(String jobId) {
        return jobs.stream()
                .filter(job -> job.getJobId().equalsIgnoreCase(jobId))
                .findFirst();
    }

    public synchronized List<Job> getQueueOrderedJobs() {
        return jobs.stream()
                .sorted(queueComparator())
                .collect(Collectors.toList());
    }

    public synchronized boolean moveJobUp(String jobId) {
        List<Job> ordered = getQueueOrderedJobs();
        int index = indexOf(ordered, jobId);
        if (index <= 0) {
            return false;
        }

        Job current = ordered.get(index);
        Job previous = ordered.get(index - 1);
        int rank = current.getPriorityRank();
        current.setPriorityRank(previous.getPriorityRank());
        previous.setPriorityRank(rank);
        return true;
    }

    public synchronized boolean moveJobDown(String jobId) {
        List<Job> ordered = getQueueOrderedJobs();
        int index = indexOf(ordered, jobId);
        if (index < 0 || index >= ordered.size() - 1) {
            return false;
        }

        Job current = ordered.get(index);
        Job next = ordered.get(index + 1);
        int rank = current.getPriorityRank();
        current.setPriorityRank(next.getPriorityRank());
        next.setPriorityRank(rank);
        return true;
    }

    public synchronized boolean updateStatus(String jobId, JobStatus status) {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty()) {
            return false;
        }
        job.get().setStatus(status);
        return true;
    }

    public synchronized boolean incrementInterrupts(String jobId) {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty()) {
            return false;
        }
        Job current = job.get();
        current.setInterrupts(current.getInterrupts() + 1);
        if (current.getStatus() == JobStatus.IN_PROGRESS || current.getStatus() == JobStatus.QUEUED) {
            current.setStatus(JobStatus.INTERRUPTED);
        }
        return true;
    }

    public synchronized boolean incrementFailures(String jobId) {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty()) {
            return false;
        }
        Job current = job.get();
        current.setFailures(current.getFailures() + 1);
        current.setStatus(JobStatus.FAILED);
        return true;
    }

    public synchronized boolean addReportLine(String jobId, String reportLine) {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty()) {
            return false;
        }
        job.get().addReportLine(reportLine);
        return true;
    }

    public synchronized boolean assignJobToVehicle(String jobId, String vehicleId) {
        Optional<Job> job = findById(jobId);
        if (job.isEmpty() || vehicleId == null || vehicleId.trim().isEmpty()) {
            return false;
        }
        job.get().setAssignedVehicleId(vehicleId.trim());
        return true;
    }

    private int nextPriorityRank() {
        int maxRank = 0;
        for (Job job : jobs) {
            maxRank = Math.max(maxRank, job.getPriorityRank());
        }
        return maxRank + 1;
    }

    private Comparator<Job> queueComparator() {
        return Comparator.comparingInt(Job::getPriorityRank)
                .thenComparing(Job::getDeadline)
                .thenComparing(Job::getCreatedAt);
    }

    private int indexOf(List<Job> jobs, String jobId) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getJobId().equalsIgnoreCase(jobId)) {
                return i;
            }
        }
        return -1;
    }
}
