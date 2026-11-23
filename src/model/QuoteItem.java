package model;

import java.io.Serializable;

public class QuoteItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Offering offering;
    private final double quantity;
    private final double unitPrice;
    private final double discountPercent;

    public QuoteItem(Offering offering, double quantity, double unitPrice) {
        this(offering, quantity, unitPrice, 0);
    }

    public QuoteItem(Offering offering, double quantity, double unitPrice, double discountPercent) {
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
        if (!Double.isFinite(discountPercent) || discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Item discount must be a finite number between 0 and 100.");
        }

        this.offering = offering;
        this.quantity = quantity;
        this.unitPrice = unitPrice == 0 ? 0 : unitPrice;
        this.discountPercent = discountPercent == 0 ? 0 : discountPercent;
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

    public double getDiscountPercent() {
        return discountPercent;
    }

    public double getLineTotal() {
        return quantity * unitPrice * (1 - discountPercent / 100.0);
    }
}
