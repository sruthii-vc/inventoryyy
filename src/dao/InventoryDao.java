package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDao {

    public boolean productExists(String name, Integer excludeId) throws SQLException {
        String query = "SELECT 1 FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))" +
                (excludeId != null ? " AND id != ?" : "");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            if (excludeId != null) {
                pstmt.setInt(2, excludeId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }



    public List<Product> getAllProducts() throws SQLException {
    List<Product> list = new ArrayList<>();

    String query = "SELECT id, name, quantity, price, "
                 + "supplier_id, minimum_quantity FROM products";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

        while (rs.next()) {
            int supplierId = rs.getInt("supplier_id");

            if (rs.wasNull()) {
                supplierId = -1;
            }

            list.add(new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("quantity"),
                rs.getDouble("price"),
                supplierId,
                rs.getInt("minimum_quantity")
            ));
        }
    }

    return list;
}
    public boolean addProduct(Product p) throws SQLException {
    requireNonNegativeQuantity(p);

    String query = "INSERT INTO products "
                 + "(name, quantity, price, supplier_id, minimum_quantity) "
                 + "VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setString(1, p.getName());
        pstmt.setInt(2, p.getQuantity());
        pstmt.setDouble(3, p.getPrice());

        if (p.getSupplierId() > 0) {
            pstmt.setInt(4, p.getSupplierId());
        } else {
            pstmt.setNull(4, java.sql.Types.INTEGER);
        }

        pstmt.setInt(5, p.getMinimumQuantity());

        return pstmt.executeUpdate() > 0;
    }
}

public boolean updateProduct(Product p) throws SQLException {
    requireNonNegativeQuantity(p);

    String query = "UPDATE products "
                 + "SET name = ?, quantity = ?, price = ?, "
                 + "supplier_id = ?, minimum_quantity = ? "
                 + "WHERE id = ?";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setString(1, p.getName());
        pstmt.setInt(2, p.getQuantity());
        pstmt.setDouble(3, p.getPrice());

        if (p.getSupplierId() > 0) {
            pstmt.setInt(4, p.getSupplierId());
        } else {
            pstmt.setNull(4, java.sql.Types.INTEGER);
        }

        pstmt.setInt(5, p.getMinimumQuantity());
        pstmt.setInt(6, p.getId());

        return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteProduct(int id) throws SQLException {
        String query = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void requireNonNegativeQuantity(Product product) {
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative.");
        }
    }

    public List<Product> getProductsForSupplier(int supplierId)
        throws SQLException {
    List<Product> list = new ArrayList<>();

    String query = """
        SELECT id, name, quantity, price, supplier_id, minimum_quantity
        FROM products
        WHERE supplier_id = ?
        ORDER BY name
        """;

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setInt(1, supplierId);

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getInt("supplier_id"),
                    rs.getInt("minimum_quantity")
                ));
            }
        }
    }

    return list;
}

public List<Product> getLowStockProductsForSupplier(int supplierId)
        throws SQLException {
    List<Product> list = new ArrayList<>();

    String query = """
        SELECT id, name, quantity, price, supplier_id, minimum_quantity
        FROM products
        WHERE supplier_id = ?
          AND quantity <= minimum_quantity
        ORDER BY quantity ASC
        """;

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setInt(1, supplierId);

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getInt("supplier_id"),
                    rs.getInt("minimum_quantity")
                ));
            }
        }
    }

    return list;
}

public int getSupplierIdForUser(int userId) throws SQLException {
    String sql = "SELECT id FROM suppliers WHERE user_id = ?";

    try (Connection con = DatabaseConnection.getInstance().getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, userId);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
    }

    return -1;
}

}