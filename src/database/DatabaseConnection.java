package database;


public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:vcrts.db";

    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(DB_URL);
    }
}