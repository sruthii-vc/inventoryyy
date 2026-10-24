package view;

import dao.InventoryDao;
import model.Product;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.sql.SQLException;

public class DashboardFrame extends JFrame {
    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));

    private JTable table;
    private DefaultTableModel model;
    private InventoryDao dao;
    private int selectedId = -1;
    private List<Product> products = new ArrayList<>();
    private String searchText = "";
    private final Set<Integer> notifiedOutOfStock = new HashSet<>();

    public DashboardFrame(User user) {
        this.dao = new InventoryDao();

        setTitle("Dashboard - Role: " + user.getRole() + " (" + user.getUsername() + ")");
        setSize(780, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Product Table ────────────────────────────────────────────────────────
        model = new DefaultTableModel(
            new String[]{"ID", "Product Name", "Stock", "Stock Status", "Price (₹)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JButton showAllBtn = new JButton("Show All");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Product name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(showAllBtn);
        add(searchPanel, BorderLayout.NORTH);
        searchBtn.addActionListener(e -> {
            searchText = searchField.getText().trim();
            displayProducts();
            int visibleCount = model.getRowCount();
            JOptionPane.showMessageDialog(this,
                visibleCount == 0 ? "No matching products found."
                    : "Search completed. " + visibleCount + " product(s) found.",
                "Search", JOptionPane.INFORMATION_MESSAGE);
        });
        searchField.addActionListener(e -> searchBtn.doClick());
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            searchText = "";
            displayProducts();
            showSuccess("Showing all products.");
        });

        // ── Buttons (role-based) ─────────────────────────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout());
        JButton addBtn    = new JButton("Add Product");
        JButton updateBtn = new JButton("Update Stock");
        JButton deleteBtn = new JButton("Delete");
        JButton clearBtn  = new JButton("Clear");
        JButton logoutBtn = new JButton("Logout");

        String role = user.getRole();

        if (role.equalsIgnoreCase("Admin")) {
            buttons.add(addBtn);
            buttons.add(updateBtn);
            buttons.add(deleteBtn);
            buttons.add(clearBtn);
        } else if (role.equalsIgnoreCase("Staff")) {
            buttons.add(updateBtn);
        }

        buttons.add(logoutBtn);
        add(buttons, BorderLayout.SOUTH);

        // ── Table Row Selection ───────────────────────────────────────────────────
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedId = (int) table.getValueAt(row, 0);
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                loadData(true);
            }
        });

        // ── Button Actions ────────────────────────────────────────────────────────
        addBtn.addActionListener(e -> showProductDialog(null, user));

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a product first.");
                return;
            }
            int productId = (int) table.getValueAt(row, 0);
            Product product = products.stream()
                .filter(item -> item.getId() == productId)
                .findFirst()
                .orElse(null);
            if (product == null) {
                JOptionPane.showMessageDialog(this,
                    "Could not find the selected product. Refresh the product list and try again.",
                    "Product Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedId = product.getId();
            showProductDialog(product, user);
        });

        deleteBtn.addActionListener(e -> {
            if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Please select a product first."); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (dao.deleteProduct(selectedId)) {
                        showSuccess("Product deleted successfully.");
                        loadData(false);
                        clear();
                    } else {
                        JOptionPane.showMessageDialog(this, "Product could not be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    showDatabaseError(ex);
                }
            }
        });

        clearBtn.addActionListener(e -> {
            clear();
            showSuccess("Selection cleared.");
        });

        // ── Logout: dispose dashboard and return to login screen ─────────────────
        logoutBtn.addActionListener(e -> {
            showSuccess("Logged out successfully.");
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    private void showProductDialog(Product existingProduct, User user) {
        boolean editing = existingProduct != null;
        boolean staff = user.getRole().equalsIgnoreCase("Staff");
        JDialog dialog = new JDialog(this, editing ? "Update Product" : "Add Product", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        JTextField nameField = new JTextField(editing ? existingProduct.getName() : "");
        JTextField quantityField = new JTextField(editing ? String.valueOf(existingProduct.getQuantity()) : "");
        JTextField priceField = new JTextField(editing ? String.valueOf(existingProduct.getPrice()) : "");
        installNumberFilter(quantityField, false);
        installNumberFilter(priceField, true);
        if (staff) {
            nameField.setEditable(false);
            priceField.setEditable(false);
        }

        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Quantity:"));
        form.add(quantityField);
        form.add(new JLabel("Price (₹):"));
        form.add(priceField);
        dialog.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(editing ? "Update Product" : "Add Product");
        JButton cancelButton = new JButton("Cancel");
        buttons.add(saveButton);
        buttons.add(cancelButton);
        dialog.add(buttons, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());
                BigDecimal priceValue = new BigDecimal(priceField.getText().trim());
                double price = priceValue.doubleValue();
                if (quantity < 0 || priceValue.signum() <= 0 || Double.isInfinite(price) || price == 0) {
                    JOptionPane.showMessageDialog(dialog, "Quantity cannot be negative, and price must be greater than zero.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Integer excludeId = editing ? existingProduct.getId() : null;
                try {
                    if (dao.productExists(name, excludeId)) {
                        JOptionPane.showMessageDialog(dialog, "A product with this name already exists. Please use a different product name.", "Duplicate Product", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Product product = new Product(editing ? existingProduct.getId() : 0, name, quantity, price);
                    boolean saved = editing ? dao.updateProduct(product) : dao.addProduct(product);
                    if (saved) {
                        showSuccess(dialog, editing ? "Product updated successfully!" : "Product added successfully!");
                        dialog.dispose();
                        loadData(true);
                        clear();
                    } else {
                        JOptionPane.showMessageDialog(dialog, editing ? "Update failed." : "Failed to add product.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    showDatabaseError(dialog, ex);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Enter a whole-number quantity of zero or more and a positive price.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(360, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void installNumberFilter(JTextField field, boolean decimal) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            private boolean isAllowed(String value) {
                return decimal ? value.matches("[0-9]*\\.?[0-9]*") : value.matches("[0-9]*");
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String updated = current.substring(0, offset) + (text == null ? "" : text)
                    + current.substring(offset + length);
                if (isAllowed(updated)) {
                    fb.replace(offset, length, text, attrs);
                }
            }
        });
    }

    private void loadData(boolean showOutOfStockAlerts) {
        try {
            products = dao.getAllProducts();
            displayProducts();
            if (showOutOfStockAlerts) {
                alertForOutOfStockProducts();
            }
        } catch (SQLException ex) {
            products = new ArrayList<>();
            model.setRowCount(0);
            showDatabaseError(ex);
        }
    }

    private void displayProducts() {
        clear();
        model.setRowCount(0);
        String query = searchText.toLowerCase(java.util.Locale.ROOT);
        for (Product product : products) {
            if (product.getName().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                String stockStatus = product.getQuantity() == 0 ? "Out of Stock" : "In Stock";
                model.addRow(new Object[]{product.getId(), product.getName(), product.getQuantity(),
                    stockStatus, PRICE_FORMAT.format(product.getPrice())});
            }
        }
    }

    private void alertForOutOfStockProducts() {
        List<String> newlyOutOfStock = new ArrayList<>();
        Set<Integer> currentlyOutOfStock = new HashSet<>();
        for (Product product : products) {
            if (product.getQuantity() == 0) {
                currentlyOutOfStock.add(product.getId());
                if (notifiedOutOfStock.add(product.getId())) {
                    newlyOutOfStock.add(product.getName());
                }
            }
        }
        notifiedOutOfStock.retainAll(currentlyOutOfStock);
        if (!newlyOutOfStock.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Out of stock: " + String.join(", ", newlyOutOfStock),
                "Stock Alert", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showDatabaseError(SQLException ex) {
        showDatabaseError(this, ex);
    }

    private void showSuccess(String message) {
        showSuccess(this, message);
    }

    private void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDatabaseError(Component parent, SQLException ex) {
        JOptionPane.showMessageDialog(parent,
            "A database error occurred. Please check the database connection and try again.\n" + ex.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private void clear() {
        selectedId = -1;
        table.clearSelection();
    }
}