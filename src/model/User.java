package model;

import javax.swing.JOptionPane;
import view.DashboardFrame;

public abstract class User {
    protected int id;
    protected String username;
    protected String role;

    public User(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    public void openDashboard() {
        JOptionPane.showMessageDialog(null, "Welcome " + role + ": " + username);
        new DashboardFrame(this).setVisible(true);
    }
}