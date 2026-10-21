package model;
import view.DashboardFrame;
import javax.swing.JOptionPane;

public class Staff extends User {
    public Staff(int id, String username) { super(id, username, "Staff"); }
    @Override
    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome Staff: " + username);
        new DashboardFrame(this).setVisible(true);
    }
}