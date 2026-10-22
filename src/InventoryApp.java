import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InventoryApp extends JFrame {
    private JTextField nameField, qtyField, priceField;
    private JTable table;
    private DefaultTableModel tableModel;
    private InventoryManager manager;
    private int selectedProductId = -1;
    private String currentRole;

    public InventoryApp(String role) {
        this.currentRole = role;
        manager = new InventoryManager();
        setTitle("Inventory Management System - Role: " + role);
        setSize(750, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Top Form Panel
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Product Details / Operations"));

        formPanel.add(new JLabel("Product Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Quantity / Buy Qty:"));
        qtyField = new JTextField();
        formPanel.add(qtyField);

        formPanel.add(new JLabel("Price ($):"));
        priceField = new JTextField();
        formPanel.add(priceField);

        add(formPanel, BorderLayout.NORTH);

        // Center Table Panel
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Available Qty", "Price"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Product");
        JButton updateButton = new JButton("Update Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton buyButton = new JButton("Checkout / Buy");
        JButton clearButton = new JButton("Clear");

        buttonPanel.add(clearButton);

        // Role-Based Button Access Control
        if (role.equalsIgnoreCase("Admin")) {
            buttonPanel.add(addButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
        } else if (role.equalsIgnoreCase("Staff")) {
            buttonPanel.add(updateButton); // Staff can update stock quantities
        } else if (role.equalsIgnoreCase("Customer")) {
            buttonPanel.add(buyButton); // Customers can make purchases (stock-out)
            nameField.setEditable(false);
            priceField.setEditable(false);
        }

        add(buttonPanel, BorderLayout.SOUTH);

        loadTableData();

        // Table Click Event
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    selectedProductId = (int) tableModel.getValueAt(selectedRow, 0);
                    nameField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    if (!role.equalsIgnoreCase("Customer")) {
                        qtyField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                        priceField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                    } else {
                        qtyField.setText("1"); // Default purchase quantity
                    }
                }
            }
        });

        // Add Button (Admin)
        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                int qty = Integer.parseInt(qtyField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim());

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name cannot be empty!");
                    return;
                }

                Product product = new Product(0, name, qty, price);
                if (manager.addProduct(product)) {
                    JOptionPane.showMessageDialog(this, "Product Added Successfully!");
                    loadTableData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding product.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for Quantity and Price.");
            }
        });

        // Update Button (Admin & Staff)
        updateButton.addActionListener(e -> {
            if (selectedProductId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product from the table first.");
                return;
            }
            try {
                String name = nameField.getText().trim();
                int qty = Integer.parseInt(qtyField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim());

                Product product = new Product(selectedProductId, name, qty, price);
                if (manager.updateProduct(product)) {
                    JOptionPane.showMessageDialog(this, "Product Updated Successfully!");
                    loadTableData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Error updating product.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for Quantity and Price.");
            }
        });

        // Delete Button (Admin only)
        deleteButton.addActionListener(e -> {
            if (selectedProductId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to delete.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (manager.deleteProduct(selectedProductId)) {
                    JOptionPane.showMessageDialog(this, "Product Deleted Successfully!");
                    loadTableData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Error deleting product.");
                }
            }
        });

        // Buy / Stock-out Sale Button (Customer workflow)
        buyButton.addActionListener(e -> {
            if (selectedProductId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product from the table to buy.");
                return;
            }
            try {
                int buyQty = Integer.parseInt(qtyField.getText().trim());
                int currentStock = Integer.parseInt(tableModel.getValueAt(table.getSelectedRow(), 2).toString());
                double unitPrice = Double.parseDouble(tableModel.getValueAt(table.getSelectedRow(), 3).toString());

                if (buyQty <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.");
                    return;
                }
                if (buyQty > currentStock) {
                    JOptionPane.showMessageDialog(this, "Insufficient stock! Available: " + currentStock);
                    return;
                }

                // Process Sale & Update Stock (Stock-out)
                int newStock = currentStock - buyQty;
                double totalPrice = buyQty * unitPrice;

                Product updatedProduct = new Product(selectedProductId, nameField.getText(), newStock, unitPrice);
                if (manager.updateProduct(updatedProduct) && recordSale(selectedProductId, buyQty, totalPrice)) {
                    JOptionPane.showMessageDialog(this, "Purchase Successful! Total Cost: $" + totalPrice);
                    loadTableData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Checkout failed.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid purchase quantity.");
            }
        });

        clearButton.addActionListener(e -> clearForm());
    }

    private boolean recordSale(int productId, int qtySold, double totalPrice) {
        String query = "INSERT INTO sales (product_id, quantity_sold, total_price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, qtySold);
            pstmt.setDouble(3, totalPrice);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        List<Product> products = manager.getAllProducts();
        for (Product p : products) {
            tableModel.addRow(new Object[]{p.id, p.name, p.getQuantity(), p.getPrice()});
        }
    }

    private void clearForm() {
        nameField.setText("");
        qtyField.setText("");
        priceField.setText("");
        selectedProductId = -1;
        table.clearSelection();
    }
}