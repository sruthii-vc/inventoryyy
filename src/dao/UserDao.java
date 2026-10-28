package dao;

import dao.PasswordHasher;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<String[]> getAllUsers() throws SQLException {
        String sql = "SELECT id, username, role FROM users ORDER BY role, username";
        List<String[]> users = new ArrayList<>();

        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("username"),
                    rs.getString("role")
                });
            }
        }

        return users;
    }

    public void createStaff(String username, char[] password)
            throws SQLException {

        createAccount(username, password, "Staff", null, null, null);
    }

    public void createSupplier(
            String username,
            char[] password,
            String supplierName,
            String email,
            String phone) throws SQLException {

        if (supplierName == null || supplierName.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }

        createAccount(
            username, password, "Supplier",
            supplierName.trim(), email, phone
        );
    }

    private void createAccount(
            String username,
            char[] password,
            String role,
            String supplierName,
            String email,
            String phone) throws SQLException {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required.");
        }

        String hashedPassword;

        try {
            hashedPassword = PasswordHasher.hash(password);
        } catch (GeneralSecurityException ex) {
        throw new SQLException("Could not securely hash the password.", ex);
        }

        String userSql =
            "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection con =
                 DatabaseConnection.getInstance().getConnection()) {

            boolean originalAutoCommit = con.getAutoCommit();

            try {
                con.setAutoCommit(false);

                try (PreparedStatement ps =
                         con.prepareStatement(
                             userSql,
                             PreparedStatement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, username.trim());
                    ps.setString(2, hashedPassword);
                    ps.setString(3, role);
                    ps.executeUpdate();

                    if ("Supplier".equals(role)) {
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                    "Could not retrieve the new user ID."
                                );
                            }

                            int userId = keys.getInt(1);

                            String supplierSql =
                                "INSERT INTO suppliers " +
                                "(user_id, supplier_name, email, phone) " +
                                "VALUES (?, ?, ?, ?)";

                            try (PreparedStatement supplierPs =
                                     con.prepareStatement(supplierSql)) {

                                supplierPs.setInt(1, userId);
                                supplierPs.setString(2, supplierName);
                                supplierPs.setString(3, email);
                                supplierPs.setString(4, phone);
                                supplierPs.executeUpdate();
                            }
                        }
                    }
                }

                con.commit();

            } catch (SQLException | RuntimeException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;

            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}