import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginFrame() {
        setTitle("Inventory System - Login");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        add(new JLabel("  Username:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("  Password:"));
        passField = new JPasswordField();
        add(passField);

        JLabel infoLabel = new JLabel("  (Try: admin/admin123)");
        infoLabel.setForeground(Color.GRAY);
        add(infoLabel);

        JButton loginButton = new JButton("Login");
        add(loginButton);

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            User user = AuthManager.login(username, password);
            if (user != null) {
                dispose(); // Close login window
                user.openDashboard(); // Polymorphically open correct dashboard
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password!", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}