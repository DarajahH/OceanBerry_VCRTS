package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // MySQL connection info for VCRTS database (Milestone 6)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vcrts_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "3426@Bronx";

    // Returns a live MySQL connection for DatabaseService to use.
    public static Connection getConnection() throws SQLException {
        try {
            // Explicit driver load for older JVMs (harmless on modern JDBC 4+)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on your classpath.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    // --- NEW: Built-in connection tester ---
    // Run this file independently to verify your database is accessible.
    public static void main(String[] args) {
        System.out.println("Attempting to connect to the database at: " + DB_URL);
        System.out.println("Using username: " + DB_USERNAME);
        
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("\nSUCCESS! You are securely connected to the MySQL database.");
                System.out.println("The DatabaseConnection class is ready for the VCRTS application.");
            }
        } catch (SQLException e) {
            System.err.println("\nFAILED! Could not connect to the database.");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("\nTroubleshooting Checklist:");
            System.err.println("1. Is your MySQL server currently running (e.g., via XAMPP or Services)?");
            System.err.println("2. Did you run the schema.sql script to create 'vcrts_db'?");
            System.err.println("3. Is your mysql-connector.jar included in your classpath?");
            System.err.println("4. Are the username and password exactly correct?");
        }
    }
}