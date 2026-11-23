package model;

import java.io.Serializable;

public class Offering implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private double unitPrice;

    public Offering(String name, String description, double unitPrice) {
        setName(name);
        setDescription(description);
        setUnitPrice(unitPrice);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter an offering name.");
        }
        this.name = name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void setUnitPrice(double unitPrice) {
        if (!Double.isFinite(unitPrice) || unitPrice < 0) {
            throw new IllegalArgumentException("Unit price must be a finite number of zero or more.");
        }
        this.unitPrice = unitPrice == 0 ? 0 : unitPrice;
    }

    @Override
    public String toString() {
        return name;
    }
}
