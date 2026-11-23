package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

public class Quote {
    public enum Status {
        Draft, Sent, Accepted, Declined
    }

    private final int number;
    private final LocalDate date;
    private final Client client;
    private final String project;
    private final Status status;
    private final int discount;
    private final String notes;
    private final ArrayList<QuoteItem> items;

    public Quote(int number, LocalDate date, Client client, String project, Status status,
            int discount, String notes, ArrayList<QuoteItem> items) {
        this.project = project.trim();
        if (this.project.isEmpty()) {
            throw new IllegalArgumentException("Enter a project name.");
        }
        if (client == null) {
            throw new IllegalArgumentException("Choose a client.");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one service to the quote.");
        }
        if (discount < 0 || discount > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }

        this.number = number;
        this.date = date;
        this.client = client;
        this.status = status;
        this.discount = discount;
        this.notes = notes.trim();
        this.items = new ArrayList<>(items);
    }

    public int getNumber() {
        return number;
    }

    public String getReference() {
        return String.format(Locale.US, "Q-%03d", number);
    }

    public LocalDate getDate() {
        return date;
    }

    public Client getClient() {
        return client;
    }

    public String getProject() {
        return project;
    }

    public Status getStatus() {
        return status;
    }

    public int getDiscount() {
        return discount;
    }

    public String getNotes() {
        return notes;
    }

    public ArrayList<QuoteItem> getItems() {
        return new ArrayList<>(items);
    }

    public long getSubtotal() {
        return calculateSubtotal(items);
    }

    public long getTotal() {
        return calculateTotal(getSubtotal(), discount);
    }

    public static long calculateSubtotal(ArrayList<QuoteItem> items) {
        long subtotal = 0;
        for (QuoteItem item : items) {
            subtotal += item.getLineTotal();
        }
        return subtotal;
    }

    public static long calculateTotal(long subtotal, int discount) {
        // Round the discounted total to the nearest cent.
        return (subtotal * (100 - discount) + 50) / 100;
    }

    @Override
    public String toString() {
        return getReference() + "  " + project + " (" + status + ")";
    }
}
