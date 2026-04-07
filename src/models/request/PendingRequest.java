package models.request;

public class PendingRequest {
    private final String requestId;
    private final String role;
    private final String entry;
    private final String timestamp;   
    private String status; // "Pending", "Approved", "Rejected" - Evans Cortez
    
    public PendingRequest(String requestId, String role, String entry, String timestamp) {
        this.requestId = requestId;
        this.role = role;
        this.entry = entry;
        this.timestamp = timestamp;
        this.status = "Pending"; // Default status is "Pending"
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRole() {
        return role;
    }

    public String getEntry() {
        return entry;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
