package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SupplierDao {

    public Map<Integer, String> getAllSuppliers() throws SQLException {
        Map<Integer, String> suppliers = new LinkedHashMap<>();

        String sql =
            "SELECT id, supplier_name FROM suppliers ORDER BY supplier_name";

        try (Connection con =
                 DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                suppliers.put(
                    rs.getInt("id"),
                    rs.getString("supplier_name")
                );
            }
        }

        return suppliers;
    }
}