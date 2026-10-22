import javax.swing.JOptionPane;

public class Staff extends User {
    public Staff(int id, String username) {
        super(id, username, "Staff");
    }

    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Staff: " + username + "\nAccess: Manage Stock and Process Sales.");
        new InventoryApp("Staff").setVisible(true);
    }
}