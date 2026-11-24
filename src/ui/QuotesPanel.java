package ui;

import static ui.SwingSupport.button;

import export.QuotePdf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;
import ui.SwingSupport.FormPanel;
import ui.SwingSupport.RecordRenderer;

public class QuotesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final RecordSearchField<Client> clientSearch;
    private final RecordSearchField<Offering> offeringSearch;
    private final JComboBox<Quote.Status> statusChoices = new JComboBox<>(Quote.Status.values());
    private final JTextField dateField = new JTextField(12);
    private final JTextField discountField = new JTextField(8);
    private final JTextField quantityField = new JTextField(8);
    private final JTextField itemDiscountField = new JTextField(8);
    private final JTextField searchField = new JTextField(12);
    private final JLabel editorMode = new JLabel();
    private final JLabel results = new JLabel();
    private final JLabel subtotalLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final DefaultListModel<QuoteItem> itemModel = new DefaultListModel<>();
    private final JList<QuoteItem> itemList = new JList<>(itemModel);
    private final DefaultListModel<Quote> quoteModel = new DefaultListModel<>();
    private final JList<Quote> quoteList = new JList<>(quoteModel);

    private String activeSearch = "";
    private Quote editingQuote;
    private Quote draft;

    public QuotesPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setLayout(new BorderLayout(12, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        RecordRenderer renderer = new RecordRenderer(dataStore);
        clientSearch = new RecordSearchField<>(dataStore::findClientsByName, renderer);
        offeringSearch = new RecordSearchField<>(dataStore::findOfferingsByName, renderer);
        itemList.setCellRenderer(renderer);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.getAccessibleContext().setAccessibleName("Quotation items");
        quoteList.setCellRenderer(renderer);
        quoteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        quoteList.getAccessibleContext().setAccessibleName("Saved quotations");

        add(createSavedQuotesPanel(), BorderLayout.WEST);
        add(createEditorPanel(), BorderLayout.CENTER);
        refreshFromStore();
        newQuote();
    }

    private JPanel createSavedQuotesPanel() {
        JPanel searchBar = new JPanel(new BorderLayout(0, 3));
        JLabel searchLabel = new JLabel("Search by client name");
        searchLabel.setLabelFor(searchField);
        searchBar.add(searchLabel, BorderLayout.NORTH);
        searchBar.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        searchField.addActionListener(event -> searchQuotes());
        searchButtons.add(button("Search", event -> searchQuotes()));
        searchButtons.add(button("Show All", event -> showAllQuotes()));
        searchButtons.add(results);
        searchBar.add(searchButtons, BorderLayout.SOUTH);

        JPanel recordButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        recordButtons.add(button("Open", event -> openQuote()));
        recordButtons.add(button("Edit", event -> editQuote()));
        recordButtons.add(button("Export PDF", event -> exportPdf()));
        recordButtons.add(button("Delete Quote", event -> deleteQuote()));

        JPanel saved = new JPanel(new BorderLayout(0, 5));
        saved.setBorder(BorderFactory.createTitledBorder("Saved quotations"));
        saved.setPreferredSize(new Dimension(355, 400));
        saved.add(searchBar, BorderLayout.NORTH);
        saved.add(new JScrollPane(quoteList), BorderLayout.CENTER);
        saved.add(recordButtons, BorderLayout.SOUTH);
        return saved;
    }

    private JPanel createEditorPanel() {
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(editorMode, BorderLayout.NORTH);
        top.add(createQuoteFields(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(createTotalsPanel(), BorderLayout.NORTH);
        bottom.add(createEditorButtons(), BorderLayout.SOUTH);

        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.setBorder(BorderFactory.createTitledBorder("Quotation editor"));
        editor.add(top, BorderLayout.NORTH);
        editor.add(createItemsPanel(), BorderLayout.CENTER);
        editor.add(bottom, BorderLayout.SOUTH);
        return editor;
    }

    private FormPanel createQuoteFields() {
        FormPanel fields = new FormPanel();
        fields.addField("Client", clientSearch);
        fields.addField("Date (YYYY-MM-DD)", dateField);
        fields.addField("Status", statusChoices);
        fields.addField("Discount (%)", discountField);
        return fields;
    }

    private JPanel createItemsPanel() {
        FormPanel itemFields = new FormPanel();
        itemFields.addField("Offering", offeringSearch);
        itemFields.addField("Quantity / hours", quantityField);
        itemFields.addField("Item discount (%)", itemDiscountField);

        JPanel itemButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        itemButtons.add(button("Add Item", event -> addItem()));
        itemButtons.add(button("Remove Item", event -> removeItem()));

        JPanel itemEditor = new JPanel(new BorderLayout(0, 5));
        itemEditor.setBorder(BorderFactory.createTitledBorder("Quotation items"));
        itemEditor.add(itemFields, BorderLayout.NORTH);
        itemEditor.add(new JScrollPane(itemList), BorderLayout.CENTER);
        itemEditor.add(itemButtons, BorderLayout.SOUTH);
        return itemEditor;
    }

    private JPanel createTotalsPanel() {
        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        totals.add(button("Calculate", event -> calculate()));
        totals.add(subtotalLabel);
        totals.add(totalLabel);
        return totals;
    }

    private JPanel createEditorButtons() {
        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        editorButtons.add(button("New Quote", event -> newQuote()));
        editorButtons.add(button("Save Quote", event -> saveQuote()));
        return editorButtons;
    }

    void refreshFromStore() {
        Client selectedClient = clientSearch.getSelectedRecord();
        if (selectedClient != null) {
            clientSearch.setSelectedRecord(dataStore.hasClient(selectedClient) ? selectedClient : null);
        }
        Offering selectedOffering = offeringSearch.getSelectedRecord();
        if (selectedOffering != null) {
            offeringSearch.setSelectedRecord(dataStore.hasOffering(selectedOffering) ? selectedOffering : null);
        }

        refreshQuoteList();
        refreshEditorMode();
        itemList.repaint();
        showTotals();
    }

    private void addItem() {
        try {
            Offering offering = offeringSearch.getSelectedRecord();
            if (offering == null) {
                throw new IllegalArgumentException("Select an offering from the search results. Add one in Offerings if needed.");
            }
            double quantity = readNumber(quantityField, "quantity");
            double discount = itemDiscountField.getText().isBlank() ? 0 : readNumber(itemDiscountField, "item discount");
            QuoteItem item = new QuoteItem(offering, quantity, offering.getUnitPrice(), discount);
            getDraft().addItem(item);
            itemModel.addElement(item);
            clearItemFields();
            showTotals();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void removeItem() {
        QuoteItem item = itemList.getSelectedValue();
        if (item == null) {
            showError("Select a quotation item to remove.");
            return;
        }
        draft.removeItem(item);
        itemModel.removeElement(item);
        showTotals();
    }

    private void calculate() {
        try {
            double discount = readNumber(discountField, "discount");
            getDraft().setDiscountPercent(discount);
            showTotals();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void saveQuote() {
        try {
            Client client = selectedClient();
            Quote changes = new Quote(client);
            if (draft != null) {
                changes.updateFrom(draft);
                changes.setClient(client);
            }
            changes.setDate(readDate());
            changes.setStatus((Quote.Status) statusChoices.getSelectedItem());
            changes.setDiscountPercent(readNumber(discountField, "discount"));

            if (editingQuote == null) {
                dataStore.addQuote(changes);
                editingQuote = changes;
            } else {
                dataStore.updateQuote(editingQuote, changes);
            }
            showAllQuotes();
            quoteList.setSelectedValue(editingQuote, true);
            loadQuote(editingQuote);
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void openQuote() {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null) {
            showError("Select a saved quotation to open.");
            return;
        }
        QuoteDialog dialog = new QuoteDialog(SwingUtilities.getWindowAncestor(this),
                dataStore.getQuoteNumber(selected), selected, dataStore.getCurrencySettings());
        dialog.setVisible(true);
    }

    private void editQuote() {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null) {
            showError("Select a saved quotation to edit.");
            return;
        }
        loadQuote(selected);
    }

    private void exportPdf() {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null) {
            showError("Select a saved quotation to export.");
            return;
        }
        int number = dataStore.getQuoteNumber(selected);
        Path target = SwingSupport.saveAs(this, "Export Quotation as PDF", null, "quotation-" + number + ".pdf", ".pdf");
        if (target == null) {
            return;
        }

        try {
            QuotePdf.write(target, selected, number, dataStore.getCurrencySettings());
            JOptionPane.showMessageDialog(this, "PDF saved to\n" + target, "Export PDF",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException error) {
            showError("Could not write the PDF. " + error.getMessage());
        }
    }

    private void loadQuote(Quote quote) {
        editingQuote = quote;
        draft = new Quote(quote.getClient());
        draft.updateFrom(quote);

        populateEditor();
    }

    private void newQuote() {
        editingQuote = null;
        draft = null;
        quoteList.clearSelection();
        populateEditor();
    }

    private void populateEditor() {
        offeringSearch.setSelectedRecord(null);
        clearItemFields();
        itemModel.clear();

        if (draft == null) {
            clientSearch.setSelectedRecord(null);
            dateField.setText(LocalDate.now().toString());
            statusChoices.setSelectedItem(Quote.Status.DRAFT);
            discountField.setText("0");
        } else {
            clientSearch.setSelectedRecord(draft.getClient());
            dateField.setText(draft.getDate().toString());
            statusChoices.setSelectedItem(draft.getStatus());
            discountField.setText(Double.toString(draft.getDiscountPercent()));
            for (QuoteItem item : draft.getItems()) {
                itemModel.addElement(item);
            }
        }
        refreshEditorMode();
        showTotals();
    }

    private void clearItemFields() {
        quantityField.setText("");
        itemDiscountField.setText("0");
    }

    void resetForSession() {
        newQuote();
        showAllQuotes();
    }

    private void deleteQuote() {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null || !dataStore.removeQuote(selected)) {
            showError("Select a saved quotation to delete.");
            return;
        }
        if (selected == editingQuote) {
            newQuote();
        }
        refreshQuoteList();
        refreshEditorMode();
    }

    private Quote getDraft() {
        Client client = selectedClient();
        if (draft == null) {
            draft = new Quote(client);
        } else {
            draft.setClient(client);
        }
        return draft;
    }

    private Client selectedClient() {
        Client client = clientSearch.getSelectedRecord();
        if (client == null) {
            throw new IllegalArgumentException("Select a client from the search results. Add one in Clients if needed.");
        }
        return client;
    }

    private double readNumber(JTextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter a numerical " + label + ", such as 10 or 2.5.");
        }
    }

    private LocalDate readDate() {
        try {
            return LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Enter a valid date in YYYY-MM-DD format.");
        }
    }

    private void showTotals() {
        double subtotal = draft == null ? 0 : draft.getSubtotal();
        double total = draft == null ? 0 : draft.getTotal();
        double discount = draft == null ? 0 : draft.getDiscountPercent();
        subtotalLabel.setText("Subtotal: " + dataStore.getCurrencySettings().format(subtotal));
        totalLabel.setText("Total: " + dataStore.getCurrencySettings().format(total) + " (" + discount + "% discount)");
    }

    private void searchQuotes() {
        activeSearch = searchField.getText().trim();
        refreshQuoteList();
    }

    private void showAllQuotes() {
        searchField.setText("");
        searchQuotes();
    }

    private void refreshQuoteList() {
        Quote selected = quoteList.getSelectedValue();
        quoteModel.clear();
        for (Quote quote : dataStore.searchQuotes(activeSearch)) {
            quoteModel.addElement(quote);
        }
        quoteList.setSelectedValue(selected, true);
        results.setText(quoteModel.isEmpty() ? "No quotations found." : quoteModel.size() + " shown");
    }

    private void refreshEditorMode() {
        editorMode.setText(editingQuote == null ? "New quotation"
                : "Editing quotation #" + dataStore.getQuoteNumber(editingQuote));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Quotation", JOptionPane.ERROR_MESSAGE);
    }
}
