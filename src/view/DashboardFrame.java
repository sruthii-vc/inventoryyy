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

        // ── Form Panel ──────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Product Management"));
        form.add(new JLabel("Name:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Quantity:"));
        qtyField = new JTextField();
        form.add(qtyField);

        form.add(new JLabel("Price ($):"));
        priceField = new JTextField();
        form.add(priceField);
        add(form, BorderLayout.NORTH);

        // ── Product Table ────────────────────────────────────────────────────────
        model = new DefaultTableModel(new String[]{"ID", "Product Name", "Stock", "Price ($)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Buttons (role-based) ─────────────────────────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout());
        JButton addBtn    = new JButton("Add Product");
        JButton updateBtn = new JButton("Update Stock");
        JButton deleteBtn = new JButton("Delete");
        JButton buyBtn    = new JButton("Buy / Checkout");
        JButton clearBtn  = new JButton("Clear");
        JButton logoutBtn = new JButton("Logout");

        String role = user.getRole();

        if (role.equalsIgnoreCase("Admin")) {
            buttons.add(addBtn);
            buttons.add(updateBtn);
            buttons.add(deleteBtn);
        } else if (role.equalsIgnoreCase("Staff")) {
            buttons.add(updateBtn);
            nameField.setEditable(false);
            priceField.setEditable(false);
        } else {
            // Customer — view + buy
            buttons.add(buyBtn);
            nameField.setEditable(false);
            priceField.setEditable(false);
        }

        buttons.add(clearBtn);
        buttons.add(logoutBtn);
        add(buttons, BorderLayout.SOUTH);

        loadData();

        // ── Table Row Selection ───────────────────────────────────────────────────
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedId = (int) model.getValueAt(row, 0);
                    nameField.setText(model.getValueAt(row, 1).toString());
                    // Customers enter the qty they want to buy; others see current stock
                    if (role.equalsIgnoreCase("Customer")) {
                        qtyField.setText("1");
                    } else {
                        qtyField.setText(model.getValueAt(row, 2).toString());
                    }
                    priceField.setText(model.getValueAt(row, 3).toString());
                }
            }
        });

        // ── Button Actions ────────────────────────────────────────────────────────
        addBtn.addActionListener(e -> {
            try {
                Product p = new Product(0, nameField.getText().trim(),
                    Integer.parseInt(qtyField.getText().trim()),
                    Double.parseDouble(priceField.getText().trim()));
                if (dao.addProduct(p)) {
                    JOptionPane.showMessageDialog(this, "Product added successfully!");
                    loadData(); clear();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity or price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        updateBtn.addActionListener(e -> {
            if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Please select a product first."); return; }
            try {
                Product p = new Product(selectedId, nameField.getText().trim(),
                    Integer.parseInt(qtyField.getText().trim()),
                    Double.parseDouble(priceField.getText().trim()));
                if (dao.updateProduct(p)) {
                    JOptionPane.showMessageDialog(this, "Product updated successfully!");
                    loadData(); clear();
                } else {
                    JOptionPane.showMessageDialog(this, "Update failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity or price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteBtn.addActionListener(e -> {
            if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Please select a product first."); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (dao.deleteProduct(selectedId)) {
                    JOptionPane.showMessageDialog(this, "Product deleted.");
                    loadData(); clear();
                }
            }
        });

        buyBtn.addActionListener(e -> {
            if (selectedId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product from the table first.");
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

                double subtotal   = buyQty * unitPrice;
                double tax        = subtotal * 0.05;   // 5% tax
                double grandTotal = subtotal + tax;

                String cashInput = JOptionPane.showInputDialog(this,
                    "--- INVOICE SUMMARY ---\n" +
                    "Product  : " + productName + "\n" +
                    "Quantity : " + buyQty + "\n" +
                    "Subtotal : $" + String.format("%.2f", subtotal) + "\n" +
                    "Tax (5%) : $" + String.format("%.2f", tax) + "\n" +
                    "Total    : $" + String.format("%.2f", grandTotal) + "\n\n" +
                    "Enter Cash Amount ($):");

                if (cashInput == null) return; // cancelled

                double cashPaid = Double.parseDouble(cashInput.trim());
                if (cashPaid < grandTotal) {
                    JOptionPane.showMessageDialog(this,
                        "Insufficient cash! Need at least $" + String.format("%.2f", grandTotal),
                        "Payment Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double change = cashPaid - grandTotal;

                if (dao.sellProduct(selectedId, buyQty, currentStock)) {
                    JOptionPane.showMessageDialog(this,
                        "=== PAYMENT SUCCESSFUL ===\n" +
                        "Item   : " + productName + " x" + buyQty + "\n" +
                        "Paid   : $" + String.format("%.2f", cashPaid) + "\n" +
                        "Change : $" + String.format("%.2f", change) + "\n\nThank you!",
                        "Receipt", JOptionPane.INFORMATION_MESSAGE);
                    loadData(); clear();
                } else {
                    JOptionPane.showMessageDialog(this, "Not enough stock!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers for quantity and cash.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        clearBtn.addActionListener(e -> clear());

        // ── Logout: dispose dashboard and return to login screen ─────────────────
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

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