import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class DBConnection {
    private static final String URL =
        "jdbc:h2:file:./restaurant_billing;MODE=MySQL;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    static {
        initializeDatabase();
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 JDBC driver not found on the classpath.", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static void initializeDatabase() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC driver not found on the classpath.", e);
        }

        Path schemaPath = Path.of("scheme");
        if (Files.notExists(schemaPath)) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            String sql = Files.readString(schemaPath);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String normalized = trimmed.toUpperCase(Locale.ROOT);
                if (normalized.startsWith("CREATE DATABASE")
                        || normalized.startsWith("USE ")) {
                    continue;
                }

                stmt.execute(trimmed);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to initialize the embedded database.", e);
        }
    }
}
