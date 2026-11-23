package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Quote implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { DRAFT, SENT, ACCEPTED, REJECTED }

    private Client client;
    private LocalDate date;
    private Status status;
    private String preparedBy = "";
    private double discountPercent;
    private ArrayList<QuoteItem> items;

    public Quote(Client client) {
        setClient(client);
        this.date = LocalDate.now();
        this.status = Status.DRAFT;
        this.discountPercent = 0;
        this.items = new ArrayList<>();
    }

    public void addItem(QuoteItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Select a quotation item.");
        }
        if (!Double.isFinite(getSubtotal() + item.getLineTotal())) {
            throw new IllegalArgumentException("The quotation subtotal is too large.");
        }
        items.add(item);
    }

    public void removeItem(QuoteItem item) {
        items.remove(item);
    }

    public List<QuoteItem> getItems() {
        return new ArrayList<>(items);
    }

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

    public double getDiscountAmount() {
        return getSubtotal() * (discountPercent / 100.0);
    }

    public Client getClient()          { return client; }
    public LocalDate getDate()         { return date; }
    public Status getStatus()          { return status; }
    public double getDiscountPercent() { return discountPercent; }
    public String getPreparedBy()     { return preparedBy; }

    public void setPreparedBy(String preparedBy) {
        this.preparedBy = preparedBy == null ? "" : preparedBy.trim();
    }

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
        if (!Double.isFinite(discountPercent) || discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be a finite number between 0 and 100.");
        }
        this.discountPercent = discountPercent == 0 ? 0 : discountPercent;
    }

    public void updateFrom(Quote source) {
        if (source == null) {
            throw new IllegalArgumentException("A quotation is required.");
        }
        client = source.client;
        date = source.date;
        status = source.status;
        preparedBy = source.preparedBy;
        discountPercent = source.discountPercent;
        // QuoteItems are immutable. Each quote owns its own list.
        items = new ArrayList<>(source.items);
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %.2f", client.getName(), date, getTotal());
    }
}
