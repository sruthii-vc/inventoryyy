package model;

public class Product extends InventoryItem implements Trackable {

    private int quantity;
    private double price;
    private int supplierId;
    private int minimumQuantity;

    // Existing constructor — keeps current code working.
    public Product(int id, String name, int quantity, double price) {
        this(id, name, quantity, price, -1, 5);
    }

    public Product(int id, String name, int quantity, double price,
                   int supplierId, int minimumQuantity) {
        super(id, name);
        this.quantity = quantity;
        this.price = price;
        this.supplierId = supplierId;
        this.minimumQuantity = minimumQuantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }

    @Override
    public String getTrackingDetails() {
        return getItemDetails();
    }

    @Override
    public String getItemDetails() {
        return "ID: " + id + " | Product: " + name
            + " | Stock: " + quantity + " | Price: ₹" + price;
    }
}