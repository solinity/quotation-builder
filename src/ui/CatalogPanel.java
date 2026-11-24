package ui;

import static ui.SwingSupport.*;

import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import storage.DataStore;

final class CatalogPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    enum Type { CLIENT, OFFERING }

    private final DataStore store;
    private final boolean offerings;
    private final String title;
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

    private final JTextField search = new JTextField(18);
    private final JLabel status = new JLabel();
    private final DefaultListModel<Object> records = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(records);
    private String query = "";

    CatalogPanel(DataStore store, Type type) {
        super(new BorderLayout(10, 10));
        if (store == null) {
            throw new IllegalArgumentException("DataStore can't be null");
        }
        this.store = store;
        offerings = type == Type.OFFERING;
        title = offerings ? "Offering" : "Client";
        recordName = offerings ? "offering" : "client";
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel editor = borderPanel(title + " details", 5, null, null, row(8, 5,
                formButton("Add " + title, this, title, () -> save(false)),
                formButton("Update", this, title, () -> save(true)),
                formButton("Delete", this, title, this::delete), button("Clear Form", event -> clearForm())));
        editor.add(createForm(), BorderLayout.WEST);
        add(editor, BorderLayout.NORTH);

        String listTitle = "Registered " + recordName + "s";
        configureList(list, new RecordRenderer(store), listTitle);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showRecord(list.getSelectedValue());
            }
        });
        search.addActionListener(event -> search());
        add(borderPanel(listTitle, 5, row(8, 5, labelFor("Search by name", search), search,
                button("Search", event -> search()), button("Show All", event -> showAll())),
                new JScrollPane(list), status), BorderLayout.CENTER);
        refreshList();
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
        Object selected = editing ? selectedRecord("update") : null;
        if (offerings) {
            saveOffering((Offering) selected);
        } else {
            saveClient((Client) selected);
        }
        showAll();
        if (!editing) {
            clearForm();
        }
    }

    private void saveClient(Client selected) {
        Client changes = new Client(name.getText(), email.getText(), phone.getText());
        if (selected == null) {
            store.addClient(changes);
        } else {
            selected.setName(changes.getName());
            selected.setEmail(changes.getEmail());
            selected.setPhone(changes.getPhone());
        }
    }

    private void saveOffering(Offering selected) {
        Offering changes = new Offering(name.getText(), description.getText(), readBasePrice());
        if (selected == null) {
            store.addOffering(changes);
        } else {
            selected.setName(changes.getName());
            selected.setDescription(changes.getDescription());
            selected.setUnitPrice(changes.getUnitPrice());
        }
    }

    private void delete() {
        Object selected = selectedRecord("delete");
        boolean removed = offerings ? store.removeOffering((Offering) selected) : store.removeClient((Client) selected);
        if (!removed) {
            throw new IllegalArgumentException("The selected " + recordName + " could not be found.");
        }
        refreshList();
        clearForm();
    }

    private Object selectedRecord(String action) {
        Object selected = list.getSelectedValue();
        if (selected == null) {
            throw new IllegalArgumentException("Select " + (offerings ? "an " : "a ") + recordName + " to " + action + ".");
        }
        return selected;
    }

    private void showRecord(Object record) {
        if (record instanceof Offering) {
            Offering offering = (Offering) record;
            name.setText(offering.getName());
            description.setText(offering.getDescription());
            description.setCaretPosition(0);
            showPrice(offering.getUnitPrice());
        } else if (record instanceof Client) {
            Client client = (Client) record;
            name.setText(client.getName());
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
        showAll();
        if (offerings) {
            refreshCurrency();
        }
    }

    private void showAll() {
        search.setText("");
        search();
    }

    private void search() {
        query = search.getText().trim();
        refreshList();
    }

    private void refreshList() {
        Object selected = list.getSelectedValue();
        records.clear();
        records.addAll(offerings ? store.searchOfferings(query) : store.searchClients(query));
        list.setSelectedValue(selected, true);
        status.setText(records.isEmpty() ? "No " + recordName + "s found." : records.size() + " shown");
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
