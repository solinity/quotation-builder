package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Quote implements Serializable {

    public enum Status { DRAFT, SENT, ACCEPTED, REJECTED }

    private Client client;
    private LocalDate date;
    private Status status;
    private double discountPercent;
    private List<QuoteItem> items;

    public Quote(Client client) {
        setClient(client);
        this.date = LocalDate.now();
        this.status = Status.DRAFT;
        this.discountPercent = 0;
        this.items = new ArrayList<>();
    }

    // ---- items ----
    public void addItem(QuoteItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item can't be null");
        }
        items.add(item);
    }

    public void removeItem(QuoteItem item) {
        items.remove(item);
    }

    public List<QuoteItem> getItems() {
        return items;
    }

    // ---- calculations ----
    public double getSubtotal() {
        double sum = 0;
        for (QuoteItem item : items) {
            sum += item.getLineTotal();
        }
        return sum;
    }

    public double getTotal() {
        return getSubtotal() * (1 - discountPercent / 100.0);
    }

    // ---- getters ----
    public Client getClient()          { return client; }
    public LocalDate getDate()         { return date; }
    public Status getStatus()          { return status; }
    public double getDiscountPercent() { return discountPercent; }

    // ---- setters with validation ----
    public void setClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("A quote needs a client");
        }
        this.client = client;
    }

    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date can't be empty");
        }
        this.date = date;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status can't be empty");
        }
        this.status = status;
    }

    public void setDiscountPercent(double discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        this.discountPercent = discountPercent;
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %.2f", client.getName(), date, getTotal());
    }
}
