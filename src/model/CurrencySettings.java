package model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public final class CurrencySettings {
    private final SessionCurrency baseCurrency;
    private final SessionCurrency displayCurrency;
    private final BigDecimal guaraniesPerDollar;

    public CurrencySettings(SessionCurrency baseCurrency, SessionCurrency displayCurrency, BigDecimal guaraniesPerDollar) {
        this.baseCurrency = Objects.requireNonNull(baseCurrency);
        this.displayCurrency = Objects.requireNonNull(displayCurrency);
        if (guaraniesPerDollar != null) {
            double rate = guaraniesPerDollar.doubleValue();
            if (!Double.isFinite(rate) || rate <= 0) {
                throw new IllegalArgumentException("Enter a positive exchange rate within the supported numeric range.");
            }
        } else if (displayCurrency != baseCurrency) {
            throw new IllegalArgumentException("Enter the exchange rate before switching currencies.");
        }
        this.guaraniesPerDollar = guaraniesPerDollar;
    }

    public SessionCurrency getBaseCurrency() {
        return baseCurrency;
    }

    public SessionCurrency getDisplayCurrency() {
        return displayCurrency;
    }

    public BigDecimal getGuaraniesPerDollar() {
        return guaraniesPerDollar;
    }

    public BigDecimal displayAmount(double amount) {
        return convert(BigDecimal.valueOf(amount), baseCurrency, displayCurrency);
    }

    public String format(double amount) {
        return displayCurrency.format(displayAmount(amount));
    }

    public double toBasePrice(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Unit price must be a finite number of zero or more.");
        }
        double result;
        try {
            result = convert(amount, displayCurrency, baseCurrency).doubleValue();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("The converted unit price is outside the supported numeric range.", error);
        }
        if (!Double.isFinite(result) || (result == 0 && amount.signum() > 0)) {
            throw new IllegalArgumentException("The converted unit price is outside the supported numeric range.");
        }
        return result;
    }

    private BigDecimal convert(BigDecimal amount, SessionCurrency from, SessionCurrency to) {
        if (from == to) {
            return amount;
        }
        if (from == SessionCurrency.USD) {
            return amount.multiply(guaraniesPerDollar, MathContext.DECIMAL128);
        }
        return amount.divide(guaraniesPerDollar, MathContext.DECIMAL128);
    }
}
