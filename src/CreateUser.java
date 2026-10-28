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

        System.out.print("Enter role (Admin/Staff/Supplier): ");
        String role = sc.nextLine().trim();

        if (!role.equalsIgnoreCase("Admin")
                && !role.equalsIgnoreCase("Staff")
                && !role.equalsIgnoreCase("Supplier")) {
            System.out.println(
                "Invalid role! Enter Admin, Staff, or Supplier.");
            Arrays.fill(password, '\0');
            sc.close();
            return;
        }

        String storedRole;
        if (role.equalsIgnoreCase("Admin")) {
            storedRole = "Admin";
        } else if (role.equalsIgnoreCase("Staff")) {
            storedRole = "Staff";
        } else {
            storedRole = "Supplier";
        }

        System.out.print("Enter supplier name (press Enter if not Supplier): ");
        String supplierName = sc.nextLine().trim();

        System.out.print("Enter supplier email (optional): ");
        String email = sc.nextLine().trim();

        System.out.print("Enter supplier phone (optional): ");
        String phone = sc.nextLine().trim();

        try {
            String hash = PasswordHasher.hash(password);

            try (Connection con =
                     DatabaseConnection.getInstance().getConnection()) {

                con.setAutoCommit(false);

                try {
                    int userId;

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO users (username, password, role) "
                            + "VALUES (?, ?, ?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, username);
                        ps.setString(2, hash);
                        ps.setString(3, storedRole);
                        ps.executeUpdate();

                        try (java.sql.ResultSet keys =
                                 ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                    "Could not retrieve the new user ID.");
                            }
                            userId = keys.getInt(1);
                        }
                    }

                    if (storedRole.equals("Supplier")) {
                        if (supplierName.isEmpty()) {
                            throw new SQLException(
                                "Supplier name is required.");
                        }

                        try (PreparedStatement ps = con.prepareStatement(
                                "INSERT INTO suppliers "
                                + "(user_id, supplier_name, email, phone) "
                                + "VALUES (?, ?, ?, ?)")) {

                            ps.setInt(1, userId);
                            ps.setString(2, supplierName);
                            ps.setString(3,
                                email.isEmpty() ? null : email);
                            ps.setString(4,
                                phone.isEmpty() ? null : phone);
                            ps.executeUpdate();
                        }
                    }

                    con.commit();
                    System.out.println(
                        storedRole + " user created successfully!");

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (Exception e) {
            System.out.println(
                "Could not create user: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            sc.close();
        }
    }
}