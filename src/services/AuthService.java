package services;

import models.User;
import models.Role;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthService {
    private final Map<String, User> usersByUsername;

    public AuthService() {
        this.usersByUsername = new LinkedHashMap<>();
    }

    public static AuthService withSeedUsers() {
        AuthService authService = new AuthService();
        authService.register(new User("U-001", "admin", "1234", Role.SYSTEM_ADMIN));
        authService.register(new User("U-002", "vehicleowner", "1234", Role.VEHICLE_OWNER));
        authService.register(new User("U-003", "submitter", "1234", Role.JOB_SUBMITTER));
        authService.register(new User("U-004", "controller", "1234", Role.JOB_CONTROLLER));
        return authService;
    }

    public void register(User user) {
        usersByUsername.put(user.getUsername().toLowerCase(), user);
    }

    public boolean addUser(String userId, String username, String password, Role role) {
        String key = normalizeUsername(username);
        if (key.isEmpty() || password == null || password.isEmpty() || role == null) {
            return false;
        }
        if (usersByUsername.containsKey(key)) {
            return false;
        }

        usersByUsername.put(key, new User(userId, username.trim(), password, role));
        return true;
    }

    public boolean removeUser(String username) {
        String key = normalizeUsername(username);
        if (key.isEmpty()) {
            return false;
        }
        return usersByUsername.remove(key) != null;
    }

    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        User user = usersByUsername.get(username.trim().toLowerCase());
        if (user == null) {
            return Optional.empty();
        }

        if (!user.getPassword().equals(password)) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(usersByUsername.values());
    }

    public List<User> getUsersList() {
        return new ArrayList<>(usersByUsername.values());
    }

    public Optional<User> findByUsername(String username) {
        String key = normalizeUsername(username);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(key));
    }

    public String nextUserId() {
        return String.format("U-%03d", usersByUsername.size() + 1);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }
}
