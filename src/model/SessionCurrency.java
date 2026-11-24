package model;

import java.math.BigDecimal;
import java.util.Locale;

public enum SessionCurrency {
    USD("US dollars (USD)", Locale.US, "%,.2f"),
    PYG("Paraguayan guaran\u00edes (PYG)", Locale.forLanguageTag("es-PY"), "%,.0f");

    private static final BigDecimal SCIENTIFIC_NOTATION_THRESHOLD = new BigDecimal("1e12");
    private final String label;
    private final Locale locale;
    private final String amountFormat;

    SessionCurrency(String label, Locale locale, String amountFormat) {
        this.label = label;
        this.locale = locale;
        this.amountFormat = amountFormat;
    }

    String format(BigDecimal amount) {
        String pattern = amount.compareTo(SCIENTIFIC_NOTATION_THRESHOLD) >= 0 ? "%.2e" : amountFormat;
        return name() + " " + String.format(locale, pattern, amount);
    }

    @Override
    public String toString() {
        return label;
    }
}
