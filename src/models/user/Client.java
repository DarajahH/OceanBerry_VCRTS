package models.user;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client extends User {

    /* Client should have:
        - Ability to submit jobs to the system with specific durations and deadlines.
        - View the status of their submitted jobs and their completion records.
        -Log in and log out of the system to manage their job submissions securely.
-DH 
    */

    private String role = "CLIENT";
    private String id;
    private String info;
    private String duration;
    private String deadline;
    private LocalDateTime timestamp;

    public Client(String id, String info, String duration, String deadline) {
        this.id = id;
        this.info = info;
        this.duration = duration;
        this.deadline = deadline;
        this.timestamp = LocalDateTime.now();
    }

    public String getRole() {
        return role;
    }

    public String getId() {
        return id;
    }

    public String getInfo() {
        return info;
    }

    public String getDuration() {
        return duration;
    }

    public String getDeadline() {
        return deadline;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return String.format("[%s] ROLE:%s | ID:%s | INFO:%s | TIME:%s | DEADLINE:%s",
                dtf.format(timestamp), role, id, info, duration, deadline);
    }

}

