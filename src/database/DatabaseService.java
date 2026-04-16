package database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class DatabaseService {
    // Register a new user to the database
    public void registerUser(String username, String password, String role) throws SQLException {
        // Create a string variable to store the sql query to insert the user into the database with the username, password, and role as parameters
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        // Try to register the user if the username and password are not empty
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the values of the prepared statement to the username, password, and role parameters
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
        }
    }
    
    // Create a public boolean method to validate the user
    public boolean validateUser(String username, String password) throws SQLException {
        // Create a string variable to store the sql query to select the user from the database where the username and password match the parameters
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        // use the try loop to provide the connection to the database and the prepared statement to execute the sql query with the username and password as parameters
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the values of the prepared statement to the username and password parameters
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            // use the try loop to provide the result set to execute the query and check if the user exists in the database.
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Create a public string method to get the current user role
    public String getUserRole(String username) throws SQLException {
        String sql = "SELECT role FROM users WHERE username = ?";
        // use the try loop to provide the connection to the database and the prepared statement to execute the sql query with the username as a parameter
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the value of the prepared statement to the username parameter
            pstmt.setString(1, username);

            // use the try loop to provide the result set to execute the query and get the role of the user from the database.
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Return the role of the user from the database
                    return rs.getString("role");
                }
            }
        }

        return "CLIENT";
    }



}
