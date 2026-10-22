public class Product extends InventoryItem {
    private int quantity;
    private double price;

    public Product(int id, String name, int quantity, double price) {
        super(id, name);
        this.quantity = quantity;
        this.price = price;
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public String getItemDetails() {
        return "ID: " + id + " | Name: " + name + " | Qty: " + quantity + " | Price: $" + price;
    }
}