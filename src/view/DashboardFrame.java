package view;

import dao.InventoryDao;
import model.Product;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class DashboardFrame extends JFrame {
    private JTextField nameField, qtyField, priceField;
    private JTable table;
    private DefaultTableModel model;
    private InventoryDao dao;
    private User currentUser;
    private int selectedId = -1;

    public DashboardFrame(User user) {
        this.currentUser = user;
        this.dao = new InventoryDao();

        setTitle("Dashboard - Role: " + user.getRole() + " (" + user.getUsername() + ")");
        setSize(780, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Product Management / Sales Window"));
        form.add(new JLabel("Name:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Quantity / Buy Qty:"));
        qtyField = new JTextField();
        form.add(qtyField);

        form.add(new JLabel("Price ($):"));
        priceField = new JTextField();
        form.add(priceField);
        add(form, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Product Name", "Stock", "Price ($)"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Product");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton buyBtn = new JButton("Checkout / Buy");
        JButton clearBtn = new JButton("Clear");
        JButton logoutBtn = new JButton("Logout");

        buttons.add(clearBtn);
        String role = user.getRole();

        if (role.equalsIgnoreCase("Admin")) {
            buttons.add(addBtn); buttons.add(updateBtn); buttons.add(deleteBtn);
        } else if (role.equalsIgnoreCase("Staff")) {
            buttons.add(updateBtn);
            nameField.setEditable(false);
        } else if (role.equalsIgnoreCase("Customer")) {
            buttons.add(buyBtn);
            nameField.setEditable(false); priceField.setEditable(false);
        }

        buttons.add(logoutBtn);
        add(buttons, BorderLayout.SOUTH);

        loadData();

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedId = (int) model.getValueAt(row, 0);
                    nameField.setText(model.getValueAt(row, 1).toString());
                    if (role.equalsIgnoreCase("Customer")) {
                        qtyField.setText("1");
                    } else {
                        qtyField.setText(model.getValueAt(row, 2).toString());
                        priceField.setText(model.getValueAt(row, 3).toString());
                    }
                }
            }
        });

        addBtn.addActionListener(e -> {
            try {
                if (dao.addProduct(new Product(0, nameField.getText(), Integer.parseInt(qtyField.getText()), Double.parseDouble(priceField.getText())))) {
                    JOptionPane.showMessageDialog(this, "Added successfully!"); loadData(); clear();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input data."); }
        });

        updateBtn.addActionListener(e -> {
            if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a product first."); return; }
            try {
                if (dao.updateProduct(new Product(selectedId, nameField.getText(), Integer.parseInt(qtyField.getText()), Double.parseDouble(priceField.getText())))) {
                    JOptionPane.showMessageDialog(this, "Updated successfully!"); loadData(); clear();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input data."); }
        });

        deleteBtn.addActionListener(e -> {
            if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a product first."); return; }
            if (dao.deleteProduct(selectedId)) { JOptionPane.showMessageDialog(this, "Deleted successfully!"); loadData(); clear(); }
        });

        buyBtn.addActionListener(e -> {
            if (selectedId == -1) { 
                JOptionPane.showMessageDialog(this, "Please select a product from the table to buy."); 
                return; 
            }
            try {
                int buyQty = Integer.parseInt(qtyField.getText().trim());
                int currentStock = Integer.parseInt(model.getValueAt(table.getSelectedRow(), 2).toString());
                double unitPrice = Double.parseDouble(model.getValueAt(table.getSelectedRow(), 3).toString());
                String productName = nameField.getText();

                if (buyQty <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be at least 1.");
                    return;
                }

                double subtotal = buyQty * unitPrice;
                double tax = subtotal * 0.05; // 5% tax simulation
                double grandTotal = subtotal + tax;

                String cashInput = JOptionPane.showInputDialog(this, 
                    "--- INVOICE SUMMARY ---\n" +
                    "Product: " + productName + "\n" +
                    "Quantity: " + buyQty + "\n" +
                    "Subtotal: $" + String.format("%.2f", subtotal) + "\n" +
                    "Tax (5%): $" + String.format("%.2f", tax) + "\n" +
                    "Grand Total: $" + String.format("%.2f", grandTotal) + "\n\n" +
                    "Enter Cash Amount Tendered ($):");

                if (cashInput == null) return; // User cancelled

                double cashPaid = Double.parseDouble(cashInput.trim());

                if (cashPaid < grandTotal) {
                    JOptionPane.showMessageDialog(this, "Insufficient cash! Grand Total is $" + String.format("%.2f", grandTotal), "Payment Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double change = cashPaid - grandTotal;

                if (dao.processCheckout(selectedId, productName, buyQty, grandTotal, currentStock)) {
                    JOptionPane.showMessageDialog(this, 
                        "=== PAYMENT SUCCESSFUL ===\n" +
                        "Item: " + productName + " (x" + buyQty + ")\n" +
                        "Total Paid: $" + String.format("%.2f", cashPaid) + "\n" +
                        "Change Returned: $" + String.format("%.2f", change) + "\n\n" +
                        "Thank you for your purchase!", "Receipt", JOptionPane.INFORMATION_MESSAGE);
                    
                    loadData(); 
                    clear();
                } else {
                    JOptionPane.showMessageDialog(this, "Checkout failed due to insufficient stock.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for quantity and cash payment.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        clearBtn.addActionListener(e -> clear());
        logoutBtn.addActionListener(e -> {
            dispose();
            // Assuming you have a LoginFrame class
            // new LoginFrame().setVisible(true);
        });
    } // Closes DashboardFrame constructor

    private void loadData() {
        model.setRowCount(0);
        List<Product> products = dao.getAllProducts();
        for (Product p : products) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getQuantity(), p.getPrice()});
        }
    }

    private void clear() {
        selectedId = -1;
        nameField.setText("");
        qtyField.setText("");
        priceField.setText("");
        table.clearSelection();
    }
} 