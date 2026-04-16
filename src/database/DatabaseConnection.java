package database;

public class DatabaseConnection {
    // is able to provide the username, the passworld and the url.
    private static final String DB_URL = "jdbc:sqlite:vcrts.db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "yourpassword";

    // Must be able to throw the SQL Exception to help the connection to help connect the java with the sql.
    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
}