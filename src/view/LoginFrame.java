package view;

import dao.InventoryDao;
import model.User;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login - Inventory Management System");
        setSize(400, 250); // Compact and neat size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centers on screen
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

        // Login Button Action Listener
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Temporary simulation for testing layout:
            if (username.equals("admin") && password.equals("admin")) {
                // Instantiate abstract User via an anonymous subclass fulfilling openDashboard()
                User user = new User(1, "admin", "Admin") {
                    @Override
                    public void openDashboard() {
                        // Satisfies the abstract method requirement
                    }
                };
                
                JOptionPane.showMessageDialog(this, "Login Successful!");
                new DashboardFrame(user).setVisible(true);
                dispose(); // Close login window
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void main(String[] args) {
        // Ensures smooth UI rendering
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}