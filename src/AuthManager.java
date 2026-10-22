import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthManager {
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
                System.out.println("✅ Login Successful for User: " + username + " with Role: " + role);

                if (role.equalsIgnoreCase("Admin")) {
                    return new Admin(id, username);
                } else if (role.equalsIgnoreCase("Staff")) {
                    return new Staff(id, username);
                } else if (role.equalsIgnoreCase("Customer")) {
                    return new Customer(id, username);
                }
            } else {
                System.out.println("❌ No matching user found for Username: '" + username + "' and Password.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database SQL Error during login:");
            e.printStackTrace();
        }
        return null;
    }
}