package model;

public class Offering {
    private final String name;
    private final String unit;
    private final long rate;

    public Offering(String name, String unit, long rate) {
        this.name = name.trim();
        this.unit = unit;
        this.rate = rate;

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Enter the service name.");
        }
        if (unit.isEmpty()) {
            throw new IllegalArgumentException("Choose a billing unit.");
        }
        if (rate < 0 || rate > 100000000) {
            throw new IllegalArgumentException("The rate must be between $0 and $1,000,000.");
        }
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public long getRate() {
        return rate;
    }

    // Store rates in cents so adding line amounts does not introduce rounding errors.
    public static long readRate(String text) {
        String amount = text.trim();
        int point = amount.indexOf('.');
        if (point >= 0 && amount.length() - point > 3) {
            throw new IllegalArgumentException("Enter a rate with up to two decimal places.");
        }
        for (int i = 0; i < amount.length(); i++) {
            char character = amount.charAt(i);
            if (character != '.' && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Enter the rate as a number, for example 45.50.");
            }
        }
        double dollars = Double.parseDouble(amount);
        return Math.round(dollars * 100);
    }

    @Override
    public String toString() {
        return name + " / " + unit;
    }
}
