import dao.DatabaseConnection;
import dao.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

public class CreateUser {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter normal password: ");
        char[] password = sc.nextLine().toCharArray();

        System.out.print("Enter role (Admin/Staff): ");
        String role = sc.nextLine().trim();

        if (!role.equalsIgnoreCase("Admin")
                && !role.equalsIgnoreCase("Staff")) {
            System.out.println("Invalid role! Enter Admin or Staff.");
            Arrays.fill(password, '\0');
            sc.close();
            return;
        }

        String storedRole = role.equalsIgnoreCase("Admin")
                ? "Admin" : "Staff";

        try {
            String hash = PasswordHasher.hash(password);

            try (Connection con = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {

                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, storedRole);

                ps.executeUpdate();
                System.out.println(storedRole + " user created successfully!");
            }
        } catch (Exception e) {
            System.out.println("Could not create user: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            sc.close();
        }
    }
}