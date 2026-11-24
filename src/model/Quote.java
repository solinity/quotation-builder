package model;

import static model.Validation.require;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.CatalogEntry.Client;
import model.CatalogEntry.Offering;

public final class Quote implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        DRAFT, SENT, ACCEPTED, REJECTED
    }

    private Client client;
    private LocalDate date;
    private Status status;
    private double discountPercent;
    private ArrayList<Item> items;

    public Quote(Client client) {
        this(client, LocalDate.now(), Status.DRAFT, 0);
    }

    public Quote(Client client, LocalDate date, Status status, double discountPercent) {
        this(client, date, status, discountPercent, List.of());
    }

    public Quote(Client client, LocalDate date, Status status, double discountPercent, List<Item> items) {
        update(client, date, status, discountPercent);
        this.items = new ArrayList<>(items);
        subtotal(this.items);
    }

    public void addItem(Item item) {
        require(item != null, "Select a quotation item.");
        require(Double.isFinite(getSubtotal() + item.getLineTotal()), "The quotation subtotal is too large.");
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public double getSubtotal() {
        return subtotal(items);
    }

    public static double subtotal(List<Item> items) {
        double sum = 0;
        for (Item item : items) {
            require(item != null, "Select a quotation item.");
            sum += item.getLineTotal();
        }
        require(Double.isFinite(sum), "The quotation subtotal is too large.");
        return sum;
    }

    public double getTotal() {
        return discounted(getSubtotal(), discountPercent);
    }

    public static double discounted(double amount, double discountPercent) {
        return amount * (1 - discountPercent / 100.0);
    }

    public Client getClient() {
        return client;
    }

    public LocalDate getDate() {
        return date;
    }

    public Status getStatus() {
        return status;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void update(Client client, LocalDate date, Status status, double discountPercent) {
        require(client != null, "A quote needs a client");
        require(date != null, "Date can't be empty");
        require(status != null, "Status can't be empty");
        this.discountPercent = checkedDiscount(discountPercent, "Discount");
        this.client = client;
        this.date = date;
        this.status = status;
    }

    public static double checkedDiscount(double discount, String label) {
        require(Double.isFinite(discount) && discount >= 0 && discount <= 100,
                label + " must be a finite number between 0 and 100.");
        return discount == 0 ? 0 : discount;
    }

    public void updateFrom(Quote source) {
        require(source != null, "A quotation is required.");
        update(source.client, source.date, source.status, source.discountPercent);
        items = new ArrayList<>(source.items);
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %.2f", client.getName(), date, getTotal());
    }

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Offering offering;
        private final double quantity;
        private final double unitPrice;
        private final double discountPercent;

        public Item(Offering offering, double quantity, double unitPrice) {
            this(offering, quantity, unitPrice, 0);
        }

        public Item(Offering offering, double quantity, double unitPrice, double discountPercent) {
            require(offering != null, "Select an offering.");
            require(Double.isFinite(quantity) && quantity > 0, "Quantity must be a finite number greater than zero.");
            this.unitPrice = Offering.checkedPrice(unitPrice);
            require(Double.isFinite(quantity * unitPrice), "The line total is too large.");
            this.discountPercent = Quote.checkedDiscount(discountPercent, "Item discount");
            this.offering = offering;
            this.quantity = quantity;
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
            return Quote.discounted(quantity * unitPrice, discountPercent);
        }
    }
}
