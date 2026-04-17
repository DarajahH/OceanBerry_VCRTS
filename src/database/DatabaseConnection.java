package database;

public class DatabaseConnection {
    // MySQL connection info for VCRTS database (Milestone 6)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vcrts_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "3426@Bronx";

    // Returns a live MySQL connection for DatabaseService to use.
    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        try {
            // Explicit driver load for older JVMs (harmless on modern JDBC 4+)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new java.sql.SQLException("MySQL JDBC driver not found on classpath.", e);
        }
        return java.sql.DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
}
