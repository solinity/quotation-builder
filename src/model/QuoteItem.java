package model;

import java.io.Serializable;

public class QuoteItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Offering offering;
    private final double quantity;
    private final double unitPrice;

    public QuoteItem(Offering offering, double quantity, double unitPrice) {
        if (offering == null) {
            throw new IllegalArgumentException("Select an offering.");
        }
        if (!Double.isFinite(quantity) || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a finite number greater than zero.");
        }
        if (!Double.isFinite(unitPrice) || unitPrice < 0) {
            throw new IllegalArgumentException("Unit price must be a finite number of zero or more.");
        }
        if (!Double.isFinite(quantity * unitPrice)) {
            throw new IllegalArgumentException("The line total is too large.");
        }

        this.offering = offering;
        this.quantity = quantity;
        this.unitPrice = unitPrice == 0 ? 0 : unitPrice;
    }

    public Offering getOffering() {
        return offering;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getLineTotal() {
        return quantity * unitPrice;
    }
}
