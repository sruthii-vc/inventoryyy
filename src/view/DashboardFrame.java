
package view;

import dao.InventoryDao;
import dao.SupplierDao;
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
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardFrame extends JFrame {

    private static final NumberFormat PRICE_FORMAT =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("en-IN"));

    private JTable table;
    private DefaultTableModel model;
    private InventoryDao dao;

    private int selectedId = -1;
    private int supplierId = -1;

    private List<Product> products = new ArrayList<>();
    private String searchText = "";
    private boolean lowStockOnly = false;

    private final Set<Integer> notifiedOutOfStock = new HashSet<>();

    public DashboardFrame(User user) {
        dao = new InventoryDao();

        String role = user.getRole();

        // Load the supplier profile for supplier accounts.
        if (role.equalsIgnoreCase("Supplier")) {
            try {
                supplierId = dao.getSupplierIdForUser(user.getId());

                if (supplierId == -1) {
                    JOptionPane.showMessageDialog(
                            this,
                            "No supplier profile is linked to this account.",
                            "Supplier Profile Not Found",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not load supplier profile:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        setTitle("Dashboard - Role: " + role
                + " (" + user.getUsername() + ")");
        setSize(780, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Product table
        model = new DefaultTableModel(
                new String[]{
                    "ID", "Product Name", "Stock",
                    "Stock Status", "Price (₹)"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Search and stock filters
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JButton showAllBtn = new JButton("Show All");
        JButton lowStockBtn = new JButton("View Low-Stock Products");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Product name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(showAllBtn);
        searchPanel.add(lowStockBtn);

        add(searchPanel, BorderLayout.NORTH);

        searchBtn.addActionListener(e -> {
            searchText = searchField.getText().trim();
            displayProducts();

            int count = model.getRowCount();

            JOptionPane.showMessageDialog(
                    this,
                    count == 0
                            ? "No matching products found."
                            : "Search completed. " + count
                                    + " product(s) found.",
                    "Search",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        searchField.addActionListener(e -> searchBtn.doClick());

        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            searchText = "";
            lowStockOnly = false;
            displayProducts();
            showSuccess("Showing all products.");
        });

        lowStockBtn.addActionListener(e -> {
            searchField.setText("");
            searchText = "";
            lowStockOnly = true;
            displayProducts();

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "No low-stock products found.",
                        "Low Stock",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Role-based buttons
        JPanel buttons = new JPanel(new FlowLayout());

        JButton addBtn = new JButton("Add Product");
        JButton updateBtn = new JButton("Update Stock");
        JButton deleteBtn = new JButton("Delete");
        JButton clearBtn = new JButton("Clear");
        JButton logoutBtn = new JButton("Logout");
        JButton manageUsersBtn = new JButton("Manage Users");

        if (role.equalsIgnoreCase("Admin")) {
            buttons.add(addBtn);
            buttons.add(updateBtn);
            buttons.add(deleteBtn);
            buttons.add(clearBtn);
            buttons.add(manageUsersBtn);
        } else if (role.equalsIgnoreCase("Staff")) {
            buttons.add(updateBtn);
        }

        // Supplier accounts have no product-editing buttons.
        buttons.add(logoutBtn);
        add(buttons, BorderLayout.SOUTH);

        // Table row selection
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

        // Add product
        addBtn.addActionListener(e -> showProductDialog(null, user));

        // Update product
        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();

            if (row < 0) {
                JOptionPane.showMessageDialog(
                        DashboardFrame.this,
                        "Please select a product first.");
                return;
            }

            int productId = (int) table.getValueAt(row, 0);

            Product product = products.stream()
                    .filter(p -> p.getId() == productId)
                    .findFirst()
                    .orElse(null);

            if (product == null) {
                JOptionPane.showMessageDialog(
                        DashboardFrame.this,
                        "Could not find the selected product. Refresh and try again.",
                        "Product Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            selectedId = product.getId();
            showProductDialog(product, user);
        });

        // Manage users
        manageUsersBtn.addActionListener(e ->
                new ManageUsersFrame().setVisible(true));

        // Delete product
        deleteBtn.addActionListener(e -> {
            if (selectedId == -1) {
                JOptionPane.showMessageDialog(
                        this, "Please select a product first.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete this product?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (dao.deleteProduct(selectedId)) {
                        showSuccess("Product deleted successfully.");
                        loadData(false);
                        clear();
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Product could not be deleted.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
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

        // Logout
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    private void showProductDialog(Product existingProduct, User user) {
        boolean editing = existingProduct != null;
        boolean staff = user.getRole().equalsIgnoreCase("Staff");

        Map<Integer, String> suppliers;
        try {
            suppliers = new SupplierDao().getAllSuppliers();
        } catch (SQLException ex) {
            showDatabaseError(ex);
            return;
        }

        JDialog dialog = new JDialog(
                this,
                editing ? "Update Product" : "Add Product",
                true);

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 4, 12));

        JTextField nameField = new JTextField(
                editing ? existingProduct.getName() : "");

        JTextField quantityField = new JTextField(
                editing ? String.valueOf(existingProduct.getQuantity()) : "");

        JTextField priceField = new JTextField(
                editing ? String.valueOf(existingProduct.getPrice()) : "");

        JComboBox<SupplierOption> supplierCombo = new JComboBox<>();
        supplierCombo.addItem(new SupplierOption(-1, "No supplier"));
        for (Map.Entry<Integer, String> supplier : suppliers.entrySet()) {
            supplierCombo.addItem(
                    new SupplierOption(supplier.getKey(), supplier.getValue()));
        }

        if (editing && existingProduct.getSupplierId() > 0) {
            boolean supplierFound = false;
            for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                SupplierOption option = supplierCombo.getItemAt(i);
                if (option.getId() == existingProduct.getSupplierId()) {
                    supplierCombo.setSelectedIndex(i);
                    supplierFound = true;
                    break;
                }
            }

            if (!supplierFound) {
                supplierCombo.addItem(new SupplierOption(
                        existingProduct.getSupplierId(),
                        "Current supplier (ID "
                                + existingProduct.getSupplierId() + ")"));
                supplierCombo.setSelectedIndex(supplierCombo.getItemCount() - 1);
            }
        }

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
        form.add(new JLabel("Supplier (optional):"));
        form.add(supplierCombo);

        dialog.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton(
                editing ? "Update Product" : "Add Product");

        JButton cancelButton = new JButton("Cancel");

        buttons.add(saveButton);
        buttons.add(cancelButton);
        dialog.add(buttons, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Name cannot be empty.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int quantity = Integer.parseInt(
                        quantityField.getText().trim());

                BigDecimal priceValue = new BigDecimal(
                        priceField.getText().trim());

                double price = priceValue.doubleValue();

                if (quantity < 0
                        || priceValue.signum() <= 0
                        || Double.isInfinite(price)
                        || Double.isNaN(price)) {

                    JOptionPane.showMessageDialog(
                            dialog,
                            "Quantity cannot be negative, and price must be positive.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Integer excludeId = editing
                        ? existingProduct.getId() : null;

                if (dao.productExists(name, excludeId)) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "A product with this name already exists.",
                            "Duplicate Product",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Product product = new Product(
                        editing ? existingProduct.getId() : 0,
                        name,
                        quantity,
                        price,
                        ((SupplierOption) supplierCombo.getSelectedItem()).getId(),
                        editing ? existingProduct.getMinimumQuantity() : 5);

                boolean saved = editing
                        ? dao.updateProduct(product)
                        : dao.addProduct(product);

                if (saved) {
                    showSuccess(
                            dialog,
                            editing
                                    ? "Product updated successfully!"
                                    : "Product added successfully!");

                    dialog.dispose();
                    loadData(true);
                    clear();
                } else {
                    JOptionPane.showMessageDialog(
                            dialog,
                            editing ? "Update failed." : "Failed to add product.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Enter a valid whole-number quantity and positive price.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                showDatabaseError(dialog, ex);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.pack();
        dialog.setMinimumSize(
                new Dimension(360, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void installNumberFilter(
            JTextField field, boolean decimal) {

        ((AbstractDocument) field.getDocument())
                .setDocumentFilter(new DocumentFilter() {

            private boolean isAllowed(String value) {
                return decimal
                        ? value.matches("[0-9]*\\.?[0-9]*")
                        : value.matches("[0-9]*");
            }

            @Override
            public void insertString(
                    FilterBypass fb,
                    int offset,
                    String string,
                    AttributeSet attr)
                    throws BadLocationException {

                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(
                    FilterBypass fb,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attrs)
                    throws BadLocationException {

                String current = fb.getDocument().getText(
                        0, fb.getDocument().getLength());

                String updated = current.substring(0, offset)
                        + (text == null ? "" : text)
                        + current.substring(offset + length);

                if (isAllowed(updated)) {
                    fb.replace(offset, length, text, attrs);
                }
            }
        });
    }

    private void loadData(boolean showStockAlerts) {
        try {
            if (supplierId != -1) {
                products = dao.getProductsForSupplier(supplierId);
            } else {
                products = dao.getAllProducts();
            }

            displayProducts();

            if (showStockAlerts) {
                if (supplierId != -1) {
                    alertForSupplierLowStock();
                } else {
                    alertForOutOfStockProducts();
                }
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

        String query = searchText.toLowerCase(Locale.ROOT);

        for (Product product : products) {
            boolean matchesName = product.getName()
                    .toLowerCase(Locale.ROOT)
                    .contains(query);

            boolean matchesStock = !lowStockOnly
                    || product.getQuantity()
                    <= product.getMinimumQuantity();

            if (matchesName && matchesStock) {
                String status;

                if (product.getQuantity() == 0) {
                    status = "Out of Stock";
                } else if (product.getQuantity()
                        <= product.getMinimumQuantity()) {
                    status = "Low Stock";
                } else {
                    status = "In Stock";
                }

                model.addRow(new Object[]{
                    product.getId(),
                    product.getName(),
                    product.getQuantity(),
                    status,
                    PRICE_FORMAT.format(product.getPrice())
                });
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
            JOptionPane.showMessageDialog(
                    this,
                    "Out of stock: "
                            + String.join(", ", newlyOutOfStock),
                    "Stock Alert",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void alertForSupplierLowStock() {
        if (supplierId == -1) {
            return;
        }

        try {
            List<Product> lowStockProducts =
                    dao.getLowStockProductsForSupplier(supplierId);

            if (!lowStockProducts.isEmpty()) {
                StringBuilder message = new StringBuilder(
                        "The following products need restocking:\n\n");

                for (Product product : lowStockProducts) {
                    message.append(product.getName())
                            .append(" — Stock: ")
                            .append(product.getQuantity())
                            .append(", Minimum: ")
                            .append(product.getMinimumQuantity())
                            .append("\n");
                }

                message.append("\nPlease arrange restocking.");

                JOptionPane.showMessageDialog(
                        this,
                        message.toString(),
                        "Low Stock Alert",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showDatabaseError(SQLException ex) {
        showDatabaseError(this, ex);
    }

    private void showSuccess(String message) {
        showSuccess(this, message);
    }

    private void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDatabaseError(
            Component parent, SQLException ex) {

        JOptionPane.showMessageDialog(
                parent,
                "A database error occurred. Please check the database connection and try again.\n"
                        + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static final class SupplierOption {
        private final int id;
        private final String name;

        private SupplierOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void clear() {
        selectedId = -1;
        table.clearSelection();
    }
}