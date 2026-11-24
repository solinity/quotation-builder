package ui;

import export.QuotePdf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        search.addActionListener(event -> searchQuotes());
        searchField.addActionListener(event -> searchQuotes());
        showAll.addActionListener(event -> showAllQuotes());
        searchButtons.add(search);
        searchButtons.add(showAll);
        searchButtons.add(results);
        searchBar.add(searchButtons, BorderLayout.SOUTH);

        JPanel recordButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        JButton open = new JButton("Open");
        JButton edit = new JButton("Edit");
        JButton export = new JButton("Export PDF");
        JButton delete = new JButton("Delete Quote");
        open.addActionListener(event -> openQuote());
        edit.addActionListener(event -> editQuote());
        export.addActionListener(event -> exportPdf());
        delete.addActionListener(event -> deleteQuote());
        recordButtons.add(open);
        recordButtons.add(edit);
        recordButtons.add(export);
        recordButtons.add(delete);

        JPanel saved = new JPanel(new BorderLayout(0, 5));
        saved.setBorder(BorderFactory.createTitledBorder("Saved quotations"));
        saved.setPreferredSize(new Dimension(355, 400));
        saved.add(searchBar, BorderLayout.NORTH);
        saved.add(new JScrollPane(quoteList), BorderLayout.CENTER);
        saved.add(recordButtons, BorderLayout.SOUTH);
        return saved;
    }

    private JPanel createEditorPanel() {
        FormPanel fields = new FormPanel();
        fields.addField("Client", clientSearch);
        fields.addField("Date (YYYY-MM-DD)", dateField);
        fields.addField("Status", statusChoices);
        fields.addField("Discount (%)", discountField);

        FormPanel itemFields = new FormPanel();
        itemFields.addField("Offering", offeringSearch);
        itemFields.addField("Quantity / hours", quantityField);
        itemFields.addField("Item discount (%)", itemDiscountField);

        JPanel itemButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton add = new JButton("Add Item");
        JButton remove = new JButton("Remove Item");
        add.addActionListener(event -> addItem());
        remove.addActionListener(event -> removeItem());
        itemButtons.add(add);
        itemButtons.add(remove);

        JPanel itemEditor = new JPanel(new BorderLayout(0, 5));
        itemEditor.setBorder(BorderFactory.createTitledBorder("Quotation items"));
        itemEditor.add(itemFields, BorderLayout.NORTH);
        itemEditor.add(new JScrollPane(itemList), BorderLayout.CENTER);
        itemEditor.add(itemButtons, BorderLayout.SOUTH);

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton calculate = new JButton("Calculate");
        calculate.addActionListener(event -> calculate());
        totals.add(calculate);
        totals.add(subtotalLabel);
        totals.add(totalLabel);

        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        JButton create = new JButton("New Quote");
        JButton save = new JButton("Save Quote");
        create.addActionListener(event -> newQuote());
        save.addActionListener(event -> saveQuote());
        editorButtons.add(create);
        editorButtons.add(save);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(editorMode, BorderLayout.NORTH);
        top.add(fields, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(totals, BorderLayout.NORTH);
        bottom.add(editorButtons, BorderLayout.SOUTH);

        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.setBorder(BorderFactory.createTitledBorder("Quotation editor"));
        editor.add(top, BorderLayout.NORTH);
        editor.add(itemEditor, BorderLayout.CENTER);
        editor.add(bottom, BorderLayout.SOUTH);
        return editor;
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
            quantityField.setText("");
            itemDiscountField.setText("0");
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
        Path target = choosePdfFile(number);
        if (target == null) {
            return;
        }
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = target.resolveSibling(target.getFileName() + ".pdf");
            if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                    "Replace the existing PDF file?\n" + target, "Export PDF",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            QuotePdf.write(target, selected, number, dataStore.getCurrencySettings());
            JOptionPane.showMessageDialog(this, "PDF saved to\n" + target, "Export PDF",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException error) {
            showError("Could not write the PDF. " + error.getMessage());
        }
    }

    private Path choosePdfFile(int number) {
        FileDialog dialog = new FileDialog(JOptionPane.getFrameForComponent(this), "Export Quotation as PDF", FileDialog.SAVE);
        try {
            dialog.setFile("quotation-" + number + ".pdf");
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            return files.length == 0 ? null : files[0].toPath().toAbsolutePath().normalize();
        } finally {
            dialog.dispose();
        }
    }

    private void loadQuote(Quote quote) {
        editingQuote = quote;
        draft = new Quote(quote.getClient());
        draft.updateFrom(quote);

        clientSearch.setSelectedRecord(draft.getClient());
        offeringSearch.setSelectedRecord(null);
        dateField.setText(draft.getDate().toString());
        statusChoices.setSelectedItem(draft.getStatus());
        discountField.setText(Double.toString(draft.getDiscountPercent()));
        quantityField.setText("");
        itemDiscountField.setText("0");
        itemModel.clear();
        for (QuoteItem item : draft.getItems()) {
            itemModel.addElement(item);
        }
        refreshEditorMode();
        showTotals();
    }

    private void newQuote() {
        editingQuote = null;
        draft = null;
        quoteList.clearSelection();
        clientSearch.setSelectedRecord(null);
        offeringSearch.setSelectedRecord(null);
        dateField.setText(LocalDate.now().toString());
        statusChoices.setSelectedItem(Quote.Status.DRAFT);
        discountField.setText("0");
        quantityField.setText("");
        itemDiscountField.setText("0");
        itemModel.clear();
        refreshEditorMode();
        showTotals();
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
