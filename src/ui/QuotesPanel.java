package ui;

import static model.Validation.require;
import static storage.DataStore.numberOf;
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
import java.util.Collections;
import java.util.List;
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
import model.CatalogEntry.Client;
import model.CatalogEntry.Offering;
import model.Quote;
import model.Quote.Item;
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
    private final JLabel editorMode = new JLabel();
    private final JLabel subtotalLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final DefaultListModel<Item> itemModel = new DefaultListModel<>();
    private final JList<Item> itemList = new JList<>(itemModel);
    private final RecordList<Quote> quoteList;

    private Quote editingQuote;
    private double appliedDiscount;

    public QuotesPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setLayout(new BorderLayout(12, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        RecordRenderer renderer = new RecordRenderer(dataStore);
        clientSearch = new RecordSearchField<>(query -> dataStore.searchClients(query, true), renderer);
        offeringSearch = new RecordSearchField<>(query -> dataStore.searchOfferings(query, true), renderer);
        configureList(itemList, renderer, "Quotation items");
        quoteList = new RecordList<>("Saved quotations", "No quotations found.", renderer,
                dataStore::searchQuotes);

        add(createSavedQuotesPanel(), BorderLayout.WEST);
        add(createEditorPanel(), BorderLayout.CENTER);
        refreshFromStore();
        loadQuote(null);
    }

    private JPanel createSavedQuotesPanel() {
        JPanel recordButtons = row(3, 3, quoteButton("Open", this::openQuote),
                quoteButton("Edit", () -> loadQuote(selectedQuote("edit"))),
                quoteButton("Export PDF", this::exportPdf), quoteButton("Delete Quote", this::deleteQuote));
        JPanel saved = quoteList.panel("Saved quotations", "Search by client name", recordButtons);
        saved.setPreferredSize(new Dimension(355, 400));
        return saved;
    }

    private JPanel createEditorPanel() {
        JPanel top = borderPanel(null, 6, editorMode, createQuoteFields(), null);
        JPanel totals = row(8, 4, quoteButton("Calculate", this::calculate), subtotalLabel, totalLabel);
        JPanel actions = row(6, 3, button("New Quote", event -> loadQuote(null)), quoteButton("Save Quote", this::saveQuote));
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
            clientSearch.setSelectedRecord(dataStore.getClients().contains(selectedClient) ? selectedClient : null);
        }
        Offering selectedOffering = offeringSearch.getSelectedRecord();
        if (selectedOffering != null) {
            offeringSearch.setSelectedRecord(dataStore.getOfferings().contains(selectedOffering) ? selectedOffering : null);
        }

        quoteList.refresh();
        refreshEditorMode();
        itemList.repaint();
        showTotals();
    }

    private void addItem() {
        Offering offering = offeringSearch.getSelectedRecord();
        require(offering != null, "Select an offering from the search results. Add one in Offerings if needed.");
        double quantity = readNumber(quantityField, "quantity");
        double discount = itemDiscountField.getText().isBlank() ? 0 : readNumber(itemDiscountField, "item discount");
        Item item = new Item(offering, quantity, offering.getUnitPrice(), discount);
        selectedClient();
        List<Item> updated = currentItems();
        updated.add(item);
        Quote.subtotal(updated);
        itemModel.addElement(item);
        clearItemFields();
        showTotals();
    }

    private void removeItem() {
        Item item = itemList.getSelectedValue();
        require(item != null, "Select a quotation item to remove.");
        itemModel.removeElement(item);
        showTotals();
    }

    private void calculate() {
        double discount = readNumber(discountField, "discount");
        selectedClient();
        appliedDiscount = Quote.checkedDiscount(discount, "Discount");
        showTotals();
    }

    private void saveQuote() {
        Quote changes = new Quote(selectedClient(), readDate(), (Quote.Status) statusChoices.getSelectedItem(),
                readNumber(discountField, "discount"), currentItems());
        if (editingQuote == null) {
            dataStore.addQuote(changes);
            editingQuote = changes;
        } else {
            dataStore.updateQuote(editingQuote, changes);
        }
        quoteList.showAll();
        quoteList.setSelectedValue(editingQuote, true);
        loadQuote(editingQuote);
    }

    private void openQuote() {
        Quote selected = selectedQuote("open");
        int number = numberOf(dataStore.getQuotes(), selected);
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
        int number = numberOf(dataStore.getQuotes(), selected);
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
        require(selected != null, "Select a saved quotation to " + action + ".");
        return selected;
    }

    private void loadQuote(Quote quote) {
        editingQuote = quote;
        offeringSearch.setSelectedRecord(null);
        clearItemFields();
        itemModel.clear();

        appliedDiscount = quote == null ? 0 : quote.getDiscountPercent();
        clientSearch.setSelectedRecord(quote == null ? null : quote.getClient());
        dateField.setText((quote == null ? LocalDate.now() : quote.getDate()).toString());
        statusChoices.setSelectedItem(quote == null ? Quote.Status.DRAFT : quote.getStatus());
        discountField.setText(quote == null ? "0" : Double.toString(appliedDiscount));
        if (quote == null) {
            quoteList.clearSelection();
        } else {
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
        loadQuote(null);
        quoteList.showAll();
    }

    private void deleteQuote() {
        Quote selected = selectedQuote("delete");
        require(dataStore.removeQuote(selected), "Select a saved quotation to delete.");
        if (selected == editingQuote) {
            loadQuote(null);
        }
        quoteList.refresh();
        refreshEditorMode();
    }

    private List<Item> currentItems() {
        return Collections.list(itemModel.elements());
    }

    private Client selectedClient() {
        Client client = clientSearch.getSelectedRecord();
        require(client != null, "Select a client from the search results. Add one in Clients if needed.");
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
        double subtotal = Quote.subtotal(currentItems());
        var currency = dataStore.getCurrencySettings();
        subtotalLabel.setText("Subtotal: " + currency.format(subtotal));
        totalLabel.setText("Total: " + currency.format(Quote.discounted(subtotal, appliedDiscount))
                + " (" + appliedDiscount + "% discount)");
    }

    private void refreshEditorMode() {
        editorMode.setText(editingQuote == null ? "New quotation"
                : "Editing quotation #" + numberOf(dataStore.getQuotes(), editingQuote));
    }

    private JButton quoteButton(String text, Runnable action) {
        return formButton(text, this, "Quotation", action);
    }
}
