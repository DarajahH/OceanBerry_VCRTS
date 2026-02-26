package models;

public enum Role {
    VEHICLE_OWNER,
    JOB_SUBMITTER,
    JOB_CONTROLLER,
    SYSTEM_ADMIN;

    public static Role fromText(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value cannot be null.");
        }

        String normalized = value.trim().toUpperCase();
        if ("OWNER".equals(normalized)) {
            return VEHICLE_OWNER;
        }
        if ("CLIENT".equals(normalized) || "JOB_OWNER".equals(normalized)) {
            return JOB_SUBMITTER;
        }
        if ("ADMIN".equals(normalized)) {
            return SYSTEM_ADMIN;
        }
        return Role.valueOf(normalized);
    }
}
