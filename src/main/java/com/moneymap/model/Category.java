package com.moneymap.model;

public class Category {
    private int categoryId;
    private String name;
    private String type;
    private String icon;
    private boolean isActive;

    public Category() {}

    public Category(int categoryId, String name, String type, String icon, boolean isActive) {
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.isActive = isActive;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
