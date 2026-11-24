package ui;

import static ui.SwingSupport.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import model.CurrencySettings;
import model.CurrencySettings.SessionCurrency;
import storage.DataStore;
import storage.SessionDatabase;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String APP_TITLE = "Freelance Quotation Builder";

    private final DataStore dataStore;
    private final CatalogPanel clients;
    private final CatalogPanel offerings;
    private final QuotesPanel quotes;
    private final JComboBox<SessionCurrency> currencies = new JComboBox<>(SessionCurrency.values());
    private final JTextField rateField = new JTextField(12);
    private boolean refreshingCurrency;
    private Path sessionFile;

    public MainFrame(DataStore dataStore) {
        super(APP_TITLE);

        this.dataStore = dataStore;
        clients = new CatalogPanel(dataStore, CatalogPanel.Type.CLIENT);
        offerings = new CatalogPanel(dataStore, CatalogPanel.Type.OFFERING);
        quotes = new QuotesPanel(dataStore);
        setJMenuBar(createMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Clients", clients);
        tabs.addTab("Offerings", offerings);
        tabs.addTab("Quotations", quotes);
        tabs.addTab("Settings", createSettingsTab());
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == quotes) {
                quotes.refreshFromStore();
            }
        });
        add(tabs);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(865, 555));
        setSize(1030, 660);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenu file = new JMenu("File");
        file.add(menuItem("Save Session", event -> saveSession(false)));
        file.add(menuItem("Save Session As...", event -> saveSession(true)));
        file.addSeparator();
        file.add(menuItem("Load Session...", event -> openSession()));

        JMenu help = new JMenu("Help");
        help.add(menuItem("Show Session File Location", event -> showSessionLocation()));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(file);
        menuBar.add(help);
        return menuBar;
    }

    private JMenuItem menuItem(String label, ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(action);
        return item;
    }

    private JPanel createSettingsTab() {
        FormPanel form = new FormPanel();
        form.addField("Currency", currencies);
        form.addField("Guaran\u00edes per 1 USD", rateField);
        rateField.setToolTipText("Enter a rate such as 7500.50 using a period for decimals");

        JPanel settings = borderPanel("Session settings", 8, null, null,
                row(6, 3, button("Apply", event -> applyCurrencySettings())));
        settings.add(form, BorderLayout.WEST);

        JPanel tab = borderPanel(null, 0, settings, null, null);
        tab.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        refreshCurrencySettings();
        currencies.addActionListener(event -> applyCurrencySettings());
        rateField.addActionListener(event -> applyCurrencySettings());
        return tab;
    }

    private void applyCurrencySettings() {
        if (refreshingCurrency) {
            return;
        }
        CurrencySettings previous = dataStore.getCurrencySettings();
        SessionCurrency selected = (SessionCurrency) currencies.getSelectedItem();
        try {
            String text = rateField.getText().strip();
            BigDecimal rate = text.isEmpty() ? null : new BigDecimal(text);
            if (selected == previous.getDisplayCurrency() && Objects.equals(rate, previous.getGuaraniesPerDollar())) {
                return;
            }
            dataStore.setCurrencySettings(new CurrencySettings(previous.getBaseCurrency(), selected, rate));
        } catch (IllegalArgumentException error) {
            selectCurrency(previous.getDisplayCurrency());
            String message = error instanceof NumberFormatException
                    ? "Enter the exchange rate as a number, such as 7500.50." : error.getMessage();
            JOptionPane.showMessageDialog(this, message, "Currency Settings", JOptionPane.ERROR_MESSAGE);
            return;
        }
        offerings.refreshCurrency();
        quotes.refreshFromStore();
    }

    private void refreshCurrencySettings() {
        CurrencySettings saved = dataStore.getCurrencySettings();
        BigDecimal rate = saved.getGuaraniesPerDollar();
        rateField.setText(rate == null ? "" : rate.toPlainString());
        selectCurrency(saved.getDisplayCurrency());
    }

    private void selectCurrency(SessionCurrency selected) {
        refreshingCurrency = true;
        try {
            currencies.setSelectedItem(selected);
        } finally {
            refreshingCurrency = false;
        }
    }

    private Path sessionDirectory() {
        return sessionFile == null ? null : sessionFile.getParent();
    }

    private String suggestedSaveName() {
        if (sessionFile == null) {
            return "quotation-session" + SessionDatabase.EXTENSION;
        }
        String name = sessionFile.getFileName().toString();
        if (SessionDatabase.isLegacySession(sessionFile)) {
            return name.substring(0, name.lastIndexOf('.')) + SessionDatabase.EXTENSION;
        }
        return name;
    }

    private void saveSession(boolean chooseLocation) {
        Path target = sessionFile;
        if (chooseLocation || target == null || SessionDatabase.isLegacySession(target)) {
            target = SwingSupport.saveAs(this, "Save Session As", sessionDirectory(),
                    suggestedSaveName(), SessionDatabase.EXTENSION);
        }
        if (target == null) {
            return;
        }
        try {
            SessionDatabase.save(target, dataStore);
            setSessionFile(target);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Could Not Save Session", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSession() {
        Path selected = SwingSupport.openFile(this, "Load Session", sessionDirectory());
        if (selected == null) {
            return;
        }
        try {
            DataStore loaded = SessionDatabase.load(selected);
            if ((dataStore.getClientCount() > 0 || dataStore.getOfferingCount() > 0 || dataStore.getQuoteCount() > 0)
                    && JOptionPane.showConfirmDialog(this,
                    "Load this file and replace the records and open forms in this window?", "Load Session",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            dataStore.replaceWith(loaded);
            clients.resetForSession();
            offerings.resetForSession();
            quotes.resetForSession();
            refreshCurrencySettings();
            setSessionFile(selected);
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Could Not Load Session", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setSessionFile(Path selected) {
        sessionFile = selected;
        setTitle(APP_TITLE + " - " + selected.getFileName());
    }

    private void showSessionLocation() {
        Object message = "This session has no save file yet.";
        if (sessionFile != null) {
            JTextField location = new JTextField(sessionFile.toString(), 52);
            location.setEditable(false);
            location.setCaretPosition(0);
            String label = SessionDatabase.isLegacySession(sessionFile) ? "Imported session file" : "Current session database";
            message = new Object[] {label, location};
        }
        JOptionPane.showMessageDialog(this, message, "Session File Location", JOptionPane.INFORMATION_MESSAGE);
    }
}
