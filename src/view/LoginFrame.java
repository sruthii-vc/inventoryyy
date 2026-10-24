package view;

import dao.AuthDao;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login - Inventory Management System");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Title Header
        JLabel titleLabel = new JLabel("System Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Form Center Panel
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        usernameField = new JTextField(15);
        centerPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1;
        centerPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        passwordField = new JPasswordField(15);
        centerPanel.add(passwordField, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton loginBtn = new JButton("Login");
        loginBtn.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(loginBtn);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(buttonPanel, BorderLayout.SOUTH);

        // Login Button Action — calls database via AuthDao
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();

            if (username.isEmpty() || password.length == 0) {
                java.util.Arrays.fill(password, '\0');
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                User user = AuthDao.login(username, password);
                if (user != null) {
                    JOptionPane.showMessageDialog(this, "Login Successful! Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
                    new DashboardFrame(user).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not connect to the database. Check your database settings and try again.\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not securely verify the password. Please contact the system administrator.",
                    "Authentication Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });

        // Allow pressing Enter to login
        getRootPane().setDefaultButton(loginBtn);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}