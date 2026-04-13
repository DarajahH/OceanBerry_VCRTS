package database;

public class DatabaseService {

    private static final String DB_URL = "jdbc:sqlite:vcrts.db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "";

    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
}
