package ui;

import static ui.SwingSupport.button;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import storage.DataStore;
import ui.SwingSupport.FormPanel;
import ui.SwingSupport.RecordRenderer;

abstract class CatalogPanel<T> extends JPanel {
    private static final long serialVersionUID = 1L;

    final DataStore dataStore;
    final FormPanel form = new FormPanel();
    final JTextField nameField = new JTextField(32);

    private final String recordName;
    private final String recordType;
    private final JTextField searchField = new JTextField(18);
    private final JLabel listStatus = new JLabel();
    private final DefaultListModel<T> records = new DefaultListModel<>();
    private final JList<T> recordList = new JList<>(records);
    private String activeSearch = "";

    CatalogPanel(DataStore dataStore, String recordName) {
        super(new BorderLayout(10, 10));
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore can't be null");
        }
        this.dataStore = dataStore;
        this.recordName = recordName;
        recordType = recordName.toLowerCase(Locale.ROOT);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        form.addField("Name", nameField);
    }

    final void buildPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttons.add(button("Add " + recordName, event -> addRecord()));
        buttons.add(button("Update", event -> updateRecord()));
        buttons.add(button("Delete", event -> deleteRecord()));
        buttons.add(button("Clear Form", event -> clearForm()));

        JPanel editor = new JPanel(new BorderLayout(0, 5));
        editor.setBorder(BorderFactory.createTitledBorder(recordName + " details"));
        editor.add(form, BorderLayout.WEST);
        editor.add(buttons, BorderLayout.SOUTH);
        add(editor, BorderLayout.NORTH);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel searchLabel = new JLabel("Search by name");
        searchLabel.setLabelFor(searchField);
        searchField.addActionListener(event -> search());
        searchBar.add(searchLabel);
        searchBar.add(searchField);
        searchBar.add(button("Search", event -> search()));
        searchBar.add(button("Show All", event -> showAll()));

        String listTitle = "Registered " + recordType + "s";
        recordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordList.setCellRenderer(new RecordRenderer(dataStore));
        recordList.getAccessibleContext().setAccessibleName(listTitle);
        recordList.addListSelectionListener(event -> {
            T selected = recordList.getSelectedValue();
            if (!event.getValueIsAdjusting() && selected != null) {
                showRecord(selected);
            }
        });

        JPanel listPanel = new JPanel(new BorderLayout(0, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder(listTitle));
        listPanel.add(searchBar, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(recordList), BorderLayout.CENTER);
        listPanel.add(listStatus, BorderLayout.SOUTH);
        add(listPanel, BorderLayout.CENTER);
        refreshList();
    }

    abstract List<T> findRecords(String name);
    abstract T readForm();
    abstract void showRecord(T record);
    abstract void clearDetails();
    abstract void saveNew(T record);
    abstract void saveChanges(T record, T changes);
    abstract boolean removeFromStore(T record);

    private void addRecord() {
        try {
            saveNew(readForm());
            showAll();
            clearForm();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void updateRecord() {
        T selected = selectedRecord("update");
        if (selected != null) {
            try {
                saveChanges(selected, readForm());
                showAll();
            } catch (IllegalArgumentException error) {
                showError(error.getMessage());
            }
        }
    }

    private void deleteRecord() {
        T selected = selectedRecord("delete");
        if (selected != null) {
            try {
                if (!removeFromStore(selected)) {
                    throw new IllegalArgumentException("The selected " + recordType + " could not be found.");
                }
                refreshList();
                clearForm();
            } catch (IllegalArgumentException error) {
                showError(error.getMessage());
            }
        }
    }

    private T selectedRecord(String action) {
        T selected = recordList.getSelectedValue();
        if (selected == null) {
            String article = recordType.equals("offering") ? "an " : "a ";
            showError("Select " + article + recordType + " to " + action + ".");
        }
        return selected;
    }

    private void clearForm() {
        recordList.clearSelection();
        nameField.setText("");
        clearDetails();
    }

    void resetForSession() {
        clearForm();
        showAll();
    }

    private void showAll() {
        searchField.setText("");
        search();
    }

    private void search() {
        activeSearch = searchField.getText().trim();
        refreshList();
    }

    private void refreshList() {
        T selected = recordList.getSelectedValue();
        records.clear();
        records.addAll(findRecords(activeSearch));
        recordList.setSelectedValue(selected, true);
        listStatus.setText(records.isEmpty() ? "No " + recordType + "s found." : records.size() + " shown");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, recordName, JOptionPane.ERROR_MESSAGE);
    }

    static final class ClientsPanel extends CatalogPanel<Client> {
        private static final long serialVersionUID = 1L;
        private final JTextField emailField = new JTextField(32);
        private final JTextField phoneField = new JTextField(32);

        ClientsPanel(DataStore dataStore) {
            super(dataStore, "Client");
            form.addField("Email (optional)", emailField);
            form.addField("Phone (optional)", phoneField);
            buildPanel();
        }

        @Override
        List<Client> findRecords(String name) {
            return dataStore.searchClients(name);
        }

        @Override
        Client readForm() {
            return new Client(nameField.getText(), emailField.getText(), phoneField.getText());
        }

        @Override
        void showRecord(Client client) {
            nameField.setText(client.getName());
            emailField.setText(client.getEmail());
            phoneField.setText(client.getPhone());
        }

        @Override
        void clearDetails() {
            emailField.setText("");
            phoneField.setText("");
        }

        @Override
        void saveNew(Client client) {
            dataStore.addClient(client);
        }

        @Override
        void saveChanges(Client client, Client changes) {
            client.setName(changes.getName());
            client.setEmail(changes.getEmail());
            client.setPhone(changes.getPhone());
        }

        @Override
        boolean removeFromStore(Client client) {
            return dataStore.removeClient(client);
        }
    }

    static final class OfferingsPanel extends CatalogPanel<Offering> {
        private static final long serialVersionUID = 1L;
        private final JTextArea descriptionField = new JTextArea(4, 32);
        private final JTextField priceField = new JTextField(32);
        private final JLabel priceLabel;
        private CurrencySettings priceSettings;
        private String displayedPrice;
        private double displayedBasePrice;

        OfferingsPanel(DataStore dataStore) {
            super(dataStore, "Offering");
            descriptionField.setLineWrap(true);
            descriptionField.setWrapStyleWord(true);
            descriptionField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            descriptionField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            form.addField("Description (optional)", new JScrollPane(descriptionField)).setLabelFor(descriptionField);
            priceLabel = form.addField("Unit price", priceField);
            refreshCurrency();
            buildPanel();
        }

        @Override
        List<Offering> findRecords(String name) {
            return dataStore.searchOfferings(name);
        }

        @Override
        Offering readForm() {
            return new Offering(nameField.getText(), descriptionField.getText(), readBasePrice());
        }

        @Override
        void showRecord(Offering offering) {
            nameField.setText(offering.getName());
            descriptionField.setText(offering.getDescription());
            descriptionField.setCaretPosition(0);
            showPrice(offering.getUnitPrice());
        }

        @Override
        void clearDetails() {
            descriptionField.setText("");
            priceField.setText("");
            displayedPrice = null;
        }

        @Override
        void saveNew(Offering offering) {
            dataStore.addOffering(offering);
        }

        @Override
        void saveChanges(Offering offering, Offering changes) {
            offering.setName(changes.getName());
            offering.setDescription(changes.getDescription());
            offering.setUnitPrice(changes.getUnitPrice());
        }

        @Override
        boolean removeFromStore(Offering offering) {
            return dataStore.removeOffering(offering);
        }

        private double readBasePrice() {
            String price = priceField.getText().strip();
            if (price.equals(displayedPrice)) {
                return displayedBasePrice;
            }
            try {
                return priceSettings.toBasePrice(new BigDecimal(price));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Enter a finite number for the unit price, such as 50.00.");
            }
        }

        private void showPrice(double basePrice) {
            displayedBasePrice = basePrice;
            displayedPrice = dataStore.getCurrencySettings().displayAmount(basePrice).stripTrailingZeros().toPlainString();
            priceField.setText(displayedPrice);
        }

        @Override
        void resetForSession() {
            super.resetForSession();
            refreshCurrency();
        }

        void refreshCurrency() {
            CurrencySettings updated = dataStore.getCurrencySettings();
            if (priceSettings != null && priceSettings != updated && !priceField.getText().isBlank()) {
                try {
                    showPrice(readBasePrice());
                } catch (IllegalArgumentException error) {
                    // Keep invalid input available for correction.
                    displayedPrice = null;
                }
            }
            priceSettings = updated;
            priceLabel.setText("Unit price (" + updated.getDisplayCurrency().name() + ")");
            repaint();
        }
    }
}
