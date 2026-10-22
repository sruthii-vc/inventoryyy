import javax.swing.JOptionPane;

public class Admin extends User {
    public Admin(int id, String username) {
        super(id, username, "Admin");
    }

    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Admin: " + username + "\nFull Access: Manage Products, Stock, and Users.");
        new InventoryApp("Admin").setVisible(true);
    }
}