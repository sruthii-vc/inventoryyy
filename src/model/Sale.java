package model;

public class Sale {
    private int saleId;
    private int productId;
    private String productName;
    private int quantitySold;
    private double totalPrice;
    private String saleDate;

    public Sale(int saleId, int productId, String productName, int quantitySold, double totalPrice, String saleDate) {
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.totalPrice = totalPrice;
        this.saleDate = saleDate;
    }

    public int getSaleId() { return saleId; }
    public String getProductName() { return productName; }
    public int getQuantitySold() { return quantitySold; }
    public double getTotalPrice() { return totalPrice; }
    public String getSaleDate() { return saleDate; }
}