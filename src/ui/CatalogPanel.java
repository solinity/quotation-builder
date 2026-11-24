package ui;

import static model.Validation.require;
import static ui.SwingSupport.*;

import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.CatalogEntry;
import model.CatalogEntry.Client;
import model.CurrencySettings;
import model.CatalogEntry.Offering;
import storage.DataStore;

final class CatalogPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    enum Type { CLIENT, OFFERING }

    private final DataStore store;
    private final boolean offerings;
    private final String recordName;
    private final JTextField name = new JTextField(32);
    private final JTextField email = new JTextField(32);
    private final JTextField phone = new JTextField(32);
    private final JTextArea description = new JTextArea(4, 32);
    private final JTextField price = new JTextField(32);
    private JLabel priceLabel;
    private CurrencySettings priceSettings;
    private String displayedPrice;
    private double displayedBasePrice;

    private final RecordList<CatalogEntry> list;

    CatalogPanel(DataStore store, Type type) {
        super(new BorderLayout(10, 10));
        require(store != null, "DataStore can't be null");
        this.store = store;
        offerings = type == Type.OFFERING;
        String title = offerings ? "Offering" : "Client";
        recordName = offerings ? "offering" : "client";
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel editor = borderPanel(title + " details", 5, null, null, row(8, 5,
                formButton("Add " + title, this, title, () -> save(false)),
                formButton("Update", this, title, () -> save(true)),
                formButton("Delete", this, title, this::delete), button("Clear Form", event -> clearForm())));
        editor.add(createForm(), BorderLayout.WEST);
        add(editor, BorderLayout.NORTH);

        String listTitle = "Registered " + recordName + "s";
        list = new RecordList<>(listTitle, "No " + recordName + "s found.", new RecordRenderer(store),
                query -> offerings ? store.searchOfferings(query, false) : store.searchClients(query, false));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showRecord(list.getSelectedValue());
            }
        });
        add(list.panel(listTitle, "Search by name", null), BorderLayout.CENTER);
    }

    private FormPanel createForm() {
        FormPanel form = new FormPanel();
        form.addField("Name", name);
        if (offerings) {
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            description.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            form.addField("Description (optional)", new JScrollPane(description)).setLabelFor(description);
            priceLabel = form.addField("Unit price", price);
            refreshCurrency();
        } else {
            form.addField("Email (optional)", email);
            form.addField("Phone (optional)", phone);
        }
        return form;
    }

    private void save(boolean editing) {
        CatalogEntry selected = editing ? selectedRecord("update") : null;
        if (offerings) {
            saveOffering((Offering) selected);
        } else {
            saveClient((Client) selected);
        }
        list.showAll();
        if (!editing) {
            clearForm();
        }
    }

    private void saveClient(Client selected) {
        if (selected == null) {
            store.addClient(new Client(name.getText(), email.getText(), phone.getText()));
        } else {
            selected.update(name.getText(), email.getText(), phone.getText());
        }
    }

    private void saveOffering(Offering selected) {
        double basePrice = readBasePrice();
        if (selected == null) {
            store.addOffering(new Offering(name.getText(), description.getText(), basePrice));
        } else {
            selected.update(name.getText(), description.getText(), basePrice);
        }
    }

    private void delete() {
        CatalogEntry selected = selectedRecord("delete");
        boolean removed = offerings ? store.removeOffering((Offering) selected) : store.removeClient((Client) selected);
        require(removed, "The selected " + recordName + " could not be found.");
        list.refresh();
        clearForm();
    }

    private CatalogEntry selectedRecord(String action) {
        CatalogEntry selected = list.getSelectedValue();
        require(selected != null, "Select " + (offerings ? "an " : "a ") + recordName + " to " + action + ".");
        return selected;
    }

    private void showRecord(CatalogEntry record) {
        if (record == null) {
            return;
        }
        name.setText(record.getName());
        if (record instanceof Offering) {
            Offering offering = (Offering) record;
            description.setText(offering.getDescription());
            description.setCaretPosition(0);
            showPrice(offering.getUnitPrice());
        } else {
            Client client = (Client) record;
            email.setText(client.getEmail());
            phone.setText(client.getPhone());
        }
    }

    private void clearForm() {
        list.clearSelection();
        name.setText("");
        email.setText("");
        phone.setText("");
        description.setText("");
        price.setText("");
        displayedPrice = null;
    }

    void resetForSession() {
        clearForm();
        list.showAll();
        if (offerings) {
            refreshCurrency();
        }
    }

    private double readBasePrice() {
        String text = price.getText().strip();
        if (text.equals(displayedPrice)) {
            return displayedBasePrice;
        }
        try {
            return priceSettings.toBasePrice(new BigDecimal(text));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter a finite number for the unit price, such as 50.00.");
        }
    }

    private void showPrice(double basePrice) {
        displayedBasePrice = basePrice;
        displayedPrice = store.getCurrencySettings().displayAmount(basePrice).stripTrailingZeros().toPlainString();
        price.setText(displayedPrice);
    }

    void refreshCurrency() {
        CurrencySettings updated = store.getCurrencySettings();
        if (priceSettings != null && priceSettings != updated && !price.getText().isBlank()) {
            try {
                showPrice(readBasePrice());
            } catch (IllegalArgumentException error) {
                displayedPrice = null;
            }
        }
        priceSettings = updated;
        priceLabel.setText("Unit price (" + updated.getDisplayCurrency().name() + ")");
        repaint();
    }
}
