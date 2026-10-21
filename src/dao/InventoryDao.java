package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDao {
    public boolean addProduct(Product p) {
        String query = "INSERT INTO products (name, quantity, price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, p.getName());
            pstmt.setInt(2, p.getQuantity());
            pstmt.setDouble(3, p.getPrice());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String query = "SELECT * FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getDouble("price")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean updateProduct(Product p) {
        String query = "UPDATE products SET name = ?, quantity = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, p.getName());
            pstmt.setInt(2, p.getQuantity());
            pstmt.setDouble(3, p.getPrice());
            pstmt.setInt(4, p.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteProduct(int id) {
        String query = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // Stock-out / Checkout Transaction Logic
    public boolean processCheckout(int productId, String productName, int buyQty, double unitPrice, int currentStock) {
        if (buyQty > currentStock) return false;
        String updateStock = "UPDATE products SET quantity = ? WHERE id = ?";
        String recordSale = "INSERT INTO sales (product_id, product_name, quantity_sold, total_price) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(updateStock);
                 PreparedStatement s2 = conn.prepareStatement(recordSale)) {
                
                s1.setInt(1, currentStock - buyQty);
                s1.setInt(2, productId);
                s1.executeUpdate();

                s2.setInt(1, productId);
                s2.setString(2, productName);
                s2.setInt(3, buyQty);
                s2.setDouble(4, buyQty * unitPrice);
                s2.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // Fetch all sales history records
    public List<model.Sale> getAllSales() {
        List<model.Sale> list = new ArrayList<>();
        String query = "SELECT * FROM sales ORDER BY sale_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new model.Sale(
                    rs.getInt("sale_id"),
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity_sold"),
                    rs.getDouble("total_price"),
                    rs.getString("sale_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Calculate total store revenue earned from sales
    public double getTotalRevenue() {
        double total = 0;
        String query = "SELECT SUM(total_price) AS revenue FROM sales";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                total = rs.getDouble("revenue");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }
} // 