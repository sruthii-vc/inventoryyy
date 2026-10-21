package model;
import view.DashboardFrame;
import javax.swing.JOptionPane;

public class Admin extends User {
    public Admin(int id, String username) { super(id, username, "Admin"); }
    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Admin: " + username);
        new DashboardFrame(this).setVisible(true);
    }
}