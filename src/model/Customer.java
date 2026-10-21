package model;
import view.DashboardFrame;
import javax.swing.JOptionPane;

public class Customer extends User {
    public Customer(int id, String username) { super(id, username, "Customer"); }
    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Customer: " + username);
        new DashboardFrame(this).setVisible(true);
    }
}