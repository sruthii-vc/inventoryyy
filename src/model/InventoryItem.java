package model;

public abstract class InventoryItem {
    protected int id;
    protected String name;

    public InventoryItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    public abstract String getItemDetails();
}