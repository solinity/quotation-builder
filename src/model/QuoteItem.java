package model;

public class QuoteItem {
    private final Offering offering;
    private final int quantity;

    public QuoteItem(Offering offering, int quantity) {
        if (offering == null) {
            throw new IllegalArgumentException("Choose a service.");
        }
        if (quantity < 1 || quantity > 9999) {
            throw new IllegalArgumentException("Quantity must be between 1 and 9999.");
        }
        this.offering = offering;
        this.quantity = quantity;
    }

    public Offering getOffering() {
        return offering;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getLineTotal() {
        return offering.getRate() * quantity;
    }
}
