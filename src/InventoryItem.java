public abstract class InventoryItem {
    protected int id;
    protected String name;

    public InventoryItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract String getItemDetails();
}