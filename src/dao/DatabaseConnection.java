package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DatabaseConnection {
    private static final DatabaseConnection INSTANCE =
        new DatabaseConnection();

private DatabaseConnection() {
    // Prevent objects from being created outside this class.
}

public static DatabaseConnection getInstance() {
    return INSTANCE;
}
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public Connection getConnection() throws SQLException {
        Properties localSettings = loadLocalSettings();
        String url = setting("INVENTORY_DB_URL", "url", DEFAULT_URL, localSettings);
        String username = setting("INVENTORY_DB_USER", "user", null, localSettings);
        String password = setting("INVENTORY_DB_PASSWORD", "password", null, localSettings, true);
        if (username == null || username.isBlank() || password == null) {
            throw new SQLException("Database credentials are missing. Set INVENTORY_DB_USER and INVENTORY_DB_PASSWORD, "
                + "or set user and password in inventory-db.properties in the project folder. "
                + "An explicitly empty password is allowed for local MySQL accounts.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver is not available.", e);
        }
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new SQLException("Could not connect to MySQL. Check that MySQL is running, the database URL is correct, "
                + "and the configured username and password are valid. MySQL reported: " + e.getMessage(), e);
        }
    }

    private static Properties loadLocalSettings() throws SQLException {
        Properties settings = new Properties();
        Path configPath = Path.of("inventory-db.properties");
        if (!Files.exists(configPath)) {
            return settings;
        }

        try (InputStream input = Files.newInputStream(configPath)) {
            settings.load(input);
        } catch (IOException | IllegalArgumentException e) {
            throw new SQLException("Could not read inventory-db.properties. Check that it is a valid Java properties file.", e);
        }
        return settings;
    }

    private static String setting(String environmentName, String propertyName, String defaultValue,
                                  Properties localSettings) {
        return setting(environmentName, propertyName, defaultValue, localSettings, false);
    }

    private static String setting(String environmentName, String propertyName, String defaultValue,
                                  Properties localSettings, boolean allowBlank) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        String localValue = localSettings.getProperty(propertyName);
        return localValue == null || (!allowBlank && localValue.isBlank()) ? defaultValue : localValue;
    }
}