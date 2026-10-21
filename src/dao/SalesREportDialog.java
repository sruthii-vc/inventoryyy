package view;

import dao.InventoryDao;
import model.Sale;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SalesReportDialog extends JDialog {
    public SalesReportDialog(JFrame parent) {
        super(parent, "Sales & Revenue Report", true);
        setSize(700, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        InventoryDao dao = new InventoryDao();
        double totalRevenue = dao.getTotalRevenue();

        // Top Revenue Banner
        JLabel revenueLabel = new JLabel(" 💰 Total Store Revenue: $" + String.format("%.2f", totalRevenue), JLabel.CENTER);
        revenueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        revenueLabel.setForeground(new Color(0, 128, 0));
        revenueLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(revenueLabel, BorderLayout.NORTH);

        // Sales Table
        DefaultTableModel model = new DefaultTableModel(new String[]{"Sale ID", "Product Name", "Qty Sold", "Total Price ($)", "Date & Time"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Populate Sales Data
        List<Sale> sales = dao.getAllSales();
        for (Sale s : sales) {
            model.addRow(new Object[]{
                s.getSaleId(),
                s.getProductName(),
                s.getQuantitySold(),
                String.format("%.2f", s.getTotalPrice()),
                s.getSaleDate()
            });
        }

        // Bottom Close Button
        JButton closeBtn = new JButton("Close Report");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}