package dao;

import model.*;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.*;

public class AuthDao {
    public static User login(String username, char[] password) throws SQLException, GeneralSecurityException {
        String query = "SELECT id, username, password, role FROM users WHERE username = ?";
        int id;
        String storedUsername;
        String storedPassword;
        String role;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                id = rs.getInt("id");
                storedUsername = rs.getString("username");
                storedPassword = rs.getString("password");
                role = rs.getString("role");
            }

            boolean hashed = PasswordHasher.isHashed(storedPassword);
            boolean valid = hashed
                ? PasswordHasher.verify(password, storedPassword)
                : MessageDigest.isEqual(
                    new String(password).getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                return null;
            }

            if (!hashed) {
                String upgradedPassword = PasswordHasher.hash(password);
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE users SET password = ? WHERE id = ?")) {
                    update.setString(1, upgradedPassword);
                    update.setInt(2, id);
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("Could not upgrade the stored password hash.");
                    }
                }
            }

            if ("Admin".equalsIgnoreCase(role)) {
                return new Admin(id, storedUsername);
            }
            if ("Staff".equalsIgnoreCase(role)) {
                return new Staff(id, storedUsername);
            }
            return null;
        }
    }
}