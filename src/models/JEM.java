package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JEM {

    private String role;
    private String id;
    private String info;
    private String duration;
    private String deadline;
    private LocalDateTime timestamp;

    public JEM(String role, String id, String info, String duration, String deadline) {
        this.role = role;
        this.id = id;
        this.info = info;
        this.duration = duration;
        this.deadline = deadline;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return String.format("[%s] ROLE:%s | ID:%s | INFO:%s | TIME:%s | DEADLINE:%s",
                dtf.format(timestamp), role, id, info, duration, deadline);
    }

}
