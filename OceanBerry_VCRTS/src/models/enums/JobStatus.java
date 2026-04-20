package models.enums;

public enum JobStatus {
    IN_PROGRESS("Job in progress"),
    QUEUED("Job waiting in queue"),
    COMPLETED("Job has completed"),
    FAILED("Job has failed");

    private String desc;

    private JobStatus(String desc) {
        this.desc = desc;
    }

    public String getDescription() {
        return this.desc;
    }
}