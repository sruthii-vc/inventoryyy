package view;

import dao.UserDao;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManageUsersFrame extends JFrame {

    private final UserDao userDao = new UserDao();
    private final DefaultTableModel tableModel;
    private final JTable userTable;

    public ManageUsersFrame() {
        setTitle("Manage Users");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        tableModel = new DefaultTableModel(
            new String[]{"User ID", "Username", "Role"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setRowHeight(25);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        JButton createButton = new JButton("Create User");
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(createButton);
        buttons.add(refreshButton);
        buttons.add(closeButton);

        add(buttons, BorderLayout.SOUTH);

        createButton.addActionListener(e -> showCreateUserDialog());

        refreshButton.addActionListener(e -> loadUsers());
        closeButton.addActionListener(e -> dispose());

        loadUsers();
    }

    private void loadUsers() {
        try {
            List<String[]> users = userDao.getAllUsers();
            tableModel.setRowCount(0);

            for (String[] user : users) {
                tableModel.addRow(user);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load users: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    private void showCreateUserDialog() {
    JComboBox<String> roleBox =
        new JComboBox<>(new String[]{"Staff", "Supplier"});

    JTextField usernameField = new JTextField(18);
    JPasswordField passwordField = new JPasswordField(18);

    JTextField supplierNameField = new JTextField(18);
    JTextField emailField = new JTextField(18);
    JTextField phoneField = new JTextField(18);

    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

    form.add(new JLabel("Role:"));
    form.add(roleBox);
    form.add(new JLabel("Username:"));
    form.add(usernameField);
    form.add(new JLabel("Password:"));
    form.add(passwordField);

    form.add(new JLabel("Supplier Name:"));
    form.add(supplierNameField);
    form.add(new JLabel("Email:"));
    form.add(emailField);
    form.add(new JLabel("Phone:"));
    form.add(phoneField);

    Runnable updateSupplierFields = () -> {
        boolean supplier =
            "Supplier".equals(roleBox.getSelectedItem());

        supplierNameField.setEnabled(supplier);
        emailField.setEnabled(supplier);
        phoneField.setEnabled(supplier);
    };

    roleBox.addActionListener(e -> updateSupplierFields.run());
    updateSupplierFields.run();

    int choice = JOptionPane.showConfirmDialog(
        this,
        form,
        "Create User Account",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    if (choice != JOptionPane.OK_OPTION) {
        return;
    }

    String username = usernameField.getText().trim();
    char[] password = passwordField.getPassword();
    String role = (String) roleBox.getSelectedItem();

    String supplierName = supplierNameField.getText().trim();
    String email = emailField.getText().trim();
    String phone = phoneField.getText().trim();

    if (username.isEmpty() || password.length == 0) {
        java.util.Arrays.fill(password, '\0');
        JOptionPane.showMessageDialog(
            this,
            "Username and password are required.",
            "Validation Error",
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    try {
        if (userDao.usernameExists(username)) {
            java.util.Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(
                this,
                "That username already exists. Choose another.",
                "Duplicate Username",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if ("Staff".equals(role)) {
            userDao.createStaff(username, password);
        } else {
            userDao.createSupplier(
                username, password, supplierName, email, phone
            );
        }

        JOptionPane.showMessageDialog(
            this,
            role + " account created successfully.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );

        loadUsers();

    } catch (IllegalArgumentException ex) {
        java.util.Arrays.fill(password, '\0');
        JOptionPane.showMessageDialog(
            this,
            ex.getMessage(),
            "Validation Error",
            JOptionPane.ERROR_MESSAGE
        );
    } catch (SQLException ex) {
        java.util.Arrays.fill(password, '\0');
        JOptionPane.showMessageDialog(
            this,
            "Could not create the account. Check the username and database.\n"
                + ex.getMessage(),
            "Account Creation Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}

private void showForAdmin() {
    setVisible(true);
}
}