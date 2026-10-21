package view;

import dao.AuthDao;
import model.User;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginFrame() {
        setTitle("Inventory Management System - Login");
        setSize(360, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        add(new JLabel("  Username:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("  Password:"));
        passField = new JPasswordField();
        add(passField);

        add(new JLabel("  (admin/admin123)"));
        JButton loginBtn = new JButton("Login");
        add(loginBtn);

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            User user = AuthDao.login(u, p);
            if (user != null) {
                dispose();
                user.openDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Login Credentials!");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}