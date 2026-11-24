package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.CurrencySettings;
import model.SessionCurrency;
import storage.DataStore;

final class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final Runnable currencyChanged;
    private final JComboBox<SessionCurrency> currencies = new JComboBox<>(SessionCurrency.values());
    private final JTextField rateField = new JTextField(12);
    private boolean refreshing;

    SettingsPanel(DataStore dataStore, Runnable currencyChanged) {
        super(new BorderLayout());
        this.dataStore = dataStore;
        this.currencyChanged = currencyChanged;
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        FormPanel form = new FormPanel();
        form.addField("Currency", currencies);
        form.addField("Guaran\u00edes per 1 USD", rateField);
        rateField.setToolTipText("Enter a rate such as 7500.50 using a period for decimals");

        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> applySettings());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        buttons.add(apply);

        JPanel settings = new JPanel(new BorderLayout(0, 8));
        settings.setBorder(BorderFactory.createTitledBorder("Session settings"));
        settings.add(form, BorderLayout.WEST);
        settings.add(buttons, BorderLayout.SOUTH);
        add(settings, BorderLayout.NORTH);

        refreshFromStore();
        currencies.addActionListener(event -> applySettings());
        rateField.addActionListener(event -> applySettings());
    }

    void refreshFromStore() {
        CurrencySettings saved = dataStore.getCurrencySettings();
        BigDecimal rate = saved.getGuaraniesPerDollar();
        rateField.setText(rate == null ? "" : rate.toPlainString());
        showCurrency(saved.getDisplayCurrency());
    }

    private void applySettings() {
        if (refreshing) {
            return;
        }
        CurrencySettings previous = dataStore.getCurrencySettings();
        SessionCurrency selected = (SessionCurrency) currencies.getSelectedItem();
        CurrencySettings updated;
        try {
            BigDecimal rate = readRate();
            if (selected == previous.getDisplayCurrency() && Objects.equals(rate, previous.getGuaraniesPerDollar())) {
                return;
            }
            updated = new CurrencySettings(previous.getBaseCurrency(), selected, rate);
        } catch (IllegalArgumentException error) {
            showCurrency(previous.getDisplayCurrency());
            JOptionPane.showMessageDialog(this, error.getMessage(), "Currency Settings", JOptionPane.ERROR_MESSAGE);
            return;
        }
        dataStore.setCurrencySettings(updated);
        currencyChanged.run();
    }

    private BigDecimal readRate() {
        String rate = rateField.getText().strip();
        try {
            return rate.isEmpty() ? null : new BigDecimal(rate);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter the exchange rate as a number, such as 7500.50.");
        }
    }

    private void showCurrency(SessionCurrency selected) {
        refreshing = true;
        try {
            currencies.setSelectedItem(selected);
        } finally {
            refreshing = false;
        }
    }
}
