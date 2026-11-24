package ui;

import static ui.SwingSupport.*;

import export.QuotePdf;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

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
        configureList(itemList, renderer, "Quotation items");
        configureList(quoteList, renderer, "Saved quotations");

        add(createSavedQuotesPanel(), BorderLayout.WEST);
        add(createEditorPanel(), BorderLayout.CENTER);
        refreshFromStore();
        newQuote();
    }

    private JPanel createSavedQuotesPanel() {
        searchField.addActionListener(event -> searchQuotes());
        JPanel searchBar = borderPanel(null, 3, labelFor("Search by client name", searchField), searchField,
                row(6, 3, button("Search", event -> searchQuotes()), button("Show All", event -> showAllQuotes()), results));
        JPanel recordButtons = row(3, 3, quoteButton("Open", this::openQuote),
                quoteButton("Edit", () -> loadQuote(selectedQuote("edit"))),
                quoteButton("Export PDF", this::exportPdf), quoteButton("Delete Quote", this::deleteQuote));
        JPanel saved = borderPanel("Saved quotations", 5, searchBar, new JScrollPane(quoteList), recordButtons);
        saved.setPreferredSize(new Dimension(355, 400));
        return saved;
    }

    private JPanel createEditorPanel() {
        JPanel top = borderPanel(null, 6, editorMode, createQuoteFields(), null);
        JPanel totals = row(8, 4, quoteButton("Calculate", this::calculate), subtotalLabel, totalLabel);
        JPanel actions = row(6, 3, button("New Quote", event -> newQuote()), quoteButton("Save Quote", this::saveQuote));
        return borderPanel("Quotation editor", 8, top, createItemsPanel(), borderPanel(null, 0, totals, null, actions));
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

        JPanel itemButtons = row(6, 3, quoteButton("Add Item", this::addItem), quoteButton("Remove Item", this::removeItem));
        return borderPanel("Quotation items", 5, itemFields, new JScrollPane(itemList), itemButtons);
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
    }

    private void removeItem() {
        QuoteItem item = itemList.getSelectedValue();
        if (item == null) {
            throw new IllegalArgumentException("Select a quotation item to remove.");
        }
        draft.removeItem(item);
        itemModel.removeElement(item);
        showTotals();
    }

    private void calculate() {
        double discount = readNumber(discountField, "discount");
        getDraft().setDiscountPercent(discount);
        showTotals();
    }

    private void saveQuote() {
        Client client = selectedClient();
        Quote changes = draft == null ? new Quote(client) : draft.copy();
        changes.setClient(client);
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
    }

    private void openQuote() {
        Quote selected = selectedQuote("open");
        int number = dataStore.getQuoteNumber(selected);
        JEditorPane document = new JEditorPane("text/plain",
                QuotePdf.preview(selected, number, dataStore.getCurrencySettings()));
        document.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        document.setEditable(false);
        document.getAccessibleContext().setAccessibleName("Quotation details");
        document.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        document.setCaretPosition(0);

        JOptionPane preview = new JOptionPane(new JScrollPane(document), JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new String[] {"Close"}, "Close");
        JDialog dialog = preview.createDialog(this, "Quotation #" + number);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setMinimumSize(new Dimension(620, 360));
        dialog.setSize(820, 560);
        dialog.setLocationRelativeTo(this);
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
    }

    private void exportPdf() {
        Quote selected = selectedQuote("export");
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
            JOptionPane.showMessageDialog(this, "Could not write the PDF. " + error.getMessage(), "Quotation",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Quote selectedQuote(String action) {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null) {
            throw new IllegalArgumentException("Select a saved quotation to " + action + ".");
        }
        return selected;
    }

    private void newQuote() {
        quoteList.clearSelection();
        loadQuote(null);
    }

    private void loadQuote(Quote quote) {
        editingQuote = quote;
        offeringSearch.setSelectedRecord(null);
        clearItemFields();
        itemModel.clear();

        if (quote == null) {
            draft = null;
            clientSearch.setSelectedRecord(null);
            dateField.setText(LocalDate.now().toString());
            statusChoices.setSelectedItem(Quote.Status.DRAFT);
            discountField.setText("0");
        } else {
            draft = quote.copy();
            clientSearch.setSelectedRecord(quote.getClient());
            dateField.setText(quote.getDate().toString());
            statusChoices.setSelectedItem(quote.getStatus());
            discountField.setText(Double.toString(quote.getDiscountPercent()));
            itemModel.addAll(quote.getItems());
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
        Quote selected = selectedQuote("delete");
        if (!dataStore.removeQuote(selected)) {
            throw new IllegalArgumentException("Select a saved quotation to delete.");
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
        quoteModel.addAll(dataStore.searchQuotes(activeSearch));
        quoteList.setSelectedValue(selected, true);
        results.setText(quoteModel.isEmpty() ? "No quotations found." : quoteModel.size() + " shown");
    }

    private void refreshEditorMode() {
        editorMode.setText(editingQuote == null ? "New quotation"
                : "Editing quotation #" + dataStore.getQuoteNumber(editingQuote));
    }

    private JButton quoteButton(String text, Runnable action) {
        return formButton(text, this, "Quotation", action);
    }
}
