import javax.swing.JOptionPane;

public class Customer extends User {
    public Customer(int id, String username) {
        super(id, username, "Customer");
    }

    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Customer: " + username + "\nAccess: Browse Products and Make Purchases.");
        new InventoryApp("Customer").setVisible(true);
    }
}