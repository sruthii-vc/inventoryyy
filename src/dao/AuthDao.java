package dao;

import model.*;
import java.sql.*;

public class AuthDao {
    public static User login(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            System.out.println("✅ Database Connected Successfully!");
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String role = rs.getString("role");
                System.out.println("✅ User Found! Role: " + role);

                if (role.equalsIgnoreCase("Admin")) return new Admin(id, username);
                if (role.equalsIgnoreCase("Staff")) return new Staff(id, username);
                if (role.equalsIgnoreCase("Customer")) return new Customer(id, username);
            } else {
                System.out.println("❌ No user found matching username: '" + username + "' and password.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database Connection or SQL Error:");
            e.printStackTrace();
        }
        return null;
    }
}