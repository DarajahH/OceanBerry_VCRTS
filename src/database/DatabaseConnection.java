package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/vcrts_db";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String DEFAULT_DB_PASSWORD = "3426@Bronx";

    // Returns a live MySQL connection for DatabaseService to use.
    public static Connection getConnection() throws SQLException {
        try {
            // Explicit driver load for older JVMs (harmless on modern JDBC 4+)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on your classpath.", e);
        }
        return DriverManager.getConnection(
            getSetting("VCRTS_DB_URL", DEFAULT_DB_URL),
            getSetting("VCRTS_DB_USERNAME", DEFAULT_DB_USERNAME),
            getSetting("VCRTS_DB_PASSWORD", DEFAULT_DB_PASSWORD)
        );
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection succeeded.");
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private static String getSetting(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
