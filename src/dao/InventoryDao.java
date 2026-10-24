package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDao {

    public boolean productExists(String name, Integer excludeId) throws SQLException {
        String query = "SELECT 1 FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))" +
                (excludeId != null ? " AND id != ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
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

    public boolean addProduct(Product p) throws SQLException {
        requireNonNegativeQuantity(p);
        String query = "INSERT INTO products (name, quantity, price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, p.getName());
            pstmt.setInt(2, p.getQuantity());
            pstmt.setDouble(3, p.getPrice());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String query = "SELECT * FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price")
                ));
            }
        }
        return list;
    }

    public boolean updateProduct(Product p) throws SQLException {
        requireNonNegativeQuantity(p);
        String query = "UPDATE products SET name = ?, quantity = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, p.getName());
            pstmt.setInt(2, p.getQuantity());
            pstmt.setDouble(3, p.getPrice());
            pstmt.setInt(4, p.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteProduct(int id) throws SQLException {
        String query = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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

}