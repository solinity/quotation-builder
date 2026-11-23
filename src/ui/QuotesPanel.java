package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

public class QuotesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final JComboBox<Client> clientChoices = new JComboBox<>();
    private final JComboBox<Offering> offeringChoices = new JComboBox<>();
    private final JComboBox<Quote.Status> statusChoices = new JComboBox<>(Quote.Status.values());
    private final JTextField dateField = new JTextField(10);
    private final JTextField preparedByField = new JTextField(18);
    private final JTextField discountField = new JTextField(5);
    private final JTextField quantityField = new JTextField(5);
    private final JTextField searchField = new JTextField(12);
    private final JLabel editorMode = SwingStyle.heading("New quotation");
    private final JLabel editorStatus = SwingStyle.muted("");
    private final JLabel results = SwingStyle.muted("");
    private final JLabel itemStatus = SwingStyle.muted("");
    private final JLabel calculationStatus = SwingStyle.muted("Showing the last valid discount.");
    private final JLabel subtotalLabel = new JLabel();
    private final JLabel discountLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final JButton openButton = new JButton("Open");
    private final JButton deleteButton = new JButton("Delete Quote");
    private final JButton removeButton = new JButton("Remove Item");
    private final DefaultListModel<QuoteItem> itemModel = new DefaultListModel<>();
    private final JList<QuoteItem> itemList = new JList<>(itemModel);
    private final DefaultListModel<Quote> quoteModel = new DefaultListModel<>();
    private final JList<Quote> quoteList = new JList<>(quoteModel);

    private String activeSearch = "";
    private String initialDate;
    private Quote editingQuote;
    private Quote draft;

    public QuotesPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        SwingStyle.page(this);

        RecordRenderer pickerRenderer = new RecordRenderer(dataStore, true);
        clientChoices.setRenderer(pickerRenderer);
        offeringChoices.setRenderer(pickerRenderer);
        clientChoices.setPreferredSize(new Dimension(145, 30));
        offeringChoices.setPreferredSize(new Dimension(145, 30));
        clientChoices.setToolTipText("Add missing clients in the Clients tab.");
        offeringChoices.setToolTipText("Add missing services and products in the Offerings tab.");
        itemList.setCellRenderer(new RecordRenderer(dataStore));
        quoteList.setCellRenderer(new RecordRenderer(dataStore));
        quoteList.addListSelectionListener(event -> {
            openButton.setEnabled(!quoteList.isSelectionEmpty());
            deleteButton.setEnabled(!quoteList.isSelectionEmpty());
        });
        itemList.addListSelectionListener(event -> removeButton.setEnabled(!itemList.isSelectionEmpty()));
        openButton.setEnabled(false);
        deleteButton.setEnabled(false);
        removeButton.setEnabled(false);

        add(createSavedQuotesPanel(), BorderLayout.WEST);
        add(createEditorPanel(), BorderLayout.CENTER);
        resetEditor();
        refreshFromStore();
        watchEditorFields();
    }

    private void watchEditorFields() {
        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshEditorMode();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshEditorMode();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshEditorMode();
            }
        };
        dateField.getDocument().addDocumentListener(changes);
        discountField.getDocument().addDocumentListener(changes);
        preparedByField.getDocument().addDocumentListener(changes);
        clientChoices.addActionListener(event -> refreshEditorMode());
        statusChoices.addActionListener(event -> refreshEditorMode());
    }

    private JPanel createSavedQuotesPanel() {
        JButton search = new JButton("Search");
        JButton showAll = new JButton("Show All");
        search.addActionListener(event -> searchQuotes());
        searchField.addActionListener(event -> searchQuotes());
        showAll.addActionListener(event -> showAllQuotes());
        SwingStyle.button(openButton, false);
        SwingStyle.button(deleteButton, false);
        openButton.addActionListener(event -> openQuote());
        deleteButton.addActionListener(event -> deleteQuote());

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(SwingStyle.heading("Saved quotations"), BorderLayout.NORTH);
        header.add(SwingStyle.searchBar("Search by client or quote number", searchField, search, showAll),
                BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(results, BorderLayout.CENTER);
        footer.add(SwingStyle.row(openButton, deleteButton), BorderLayout.SOUTH);

        JPanel saved = SwingStyle.card();
        saved.setPreferredSize(new Dimension(270, 400));
        saved.add(header, BorderLayout.NORTH);
        saved.add(SwingStyle.list(quoteList, "Saved quotations", 55, 5), BorderLayout.CENTER);
        saved.add(footer, BorderLayout.SOUTH);
        return saved;
    }

    private JPanel createEditorPanel() {
        JPanel sections = new EditorSections();
        sections.setBackground(SwingStyle.CANVAS);
        sections.add(createDetailsPanel(), BorderLayout.NORTH);
        sections.add(createItemsPanel(), BorderLayout.CENTER);
        sections.add(createSummaryPanel(), BorderLayout.SOUTH);

        JScrollPane scroller = new JScrollPane(sections);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel editor = new JPanel(new BorderLayout(0, 10));
        editor.setOpaque(false);
        editor.add(scroller, BorderLayout.CENTER);
        editor.add(createEditorActions(), BorderLayout.SOUTH);
        return editor;
    }

    // Match the editor width to the scrollbar's viewport as the window changes size.
    private static class EditorSections extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        EditorSections() {
            super(new BorderLayout(0, 10));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(18, visible.height - 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }
    }

    private JPanel createDetailsPanel() {
        FormPanel fields = new FormPanel();
        fields.addField("Client", clientChoices);
        fields.addField("Prepared by (optional)", preparedByField);

        FormPanel dateAndStatus = new FormPanel();
        dateAndStatus.addField("Status", statusChoices);
        dateAndStatus.addField("Date (YYYY-MM-DD)", dateField);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(fields, BorderLayout.CENTER);
        body.add(dateAndStatus, BorderLayout.EAST);

        JPanel panel = SwingStyle.card();
        panel.add(SwingStyle.heading("Quotation details"), BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createItemsPanel() {
        FormPanel itemFields = new FormPanel();
        itemFields.addField("Offering", offeringChoices);
        FormPanel quantity = new FormPanel();
        quantity.addField("Quantity / hours", quantityField);
        JPanel itemInputs = new JPanel(new BorderLayout(14, 0));
        itemInputs.setOpaque(false);
        itemInputs.add(itemFields, BorderLayout.CENTER);

        JButton add = new JButton("Add Item");
        SwingStyle.button(add, true);
        SwingStyle.button(removeButton, false);
        add.addActionListener(event -> addItem());
        quantityField.addActionListener(event -> addItem());
        removeButton.addActionListener(event -> removeItem());
        itemInputs.add(SwingStyle.row(quantity, add), BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(SwingStyle.heading("Services and products"), BorderLayout.NORTH);
        header.add(itemInputs, BorderLayout.CENTER);

        JScrollPane lines = SwingStyle.list(itemList, "Quotation items", 31, 3);
        lines.setColumnHeaderView(RecordRenderer.itemHeader());

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(itemStatus, BorderLayout.CENTER);
        footer.add(removeButton, BorderLayout.EAST);

        JPanel panel = SwingStyle.card();
        panel.add(header, BorderLayout.NORTH);
        panel.add(lines, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSummaryPanel() {
        FormPanel discount = new FormPanel();
        discount.addField("Discount (%)", discountField);
        JButton calculate = new JButton("Calculate");
        SwingStyle.button(calculate, false);
        calculate.addActionListener(event -> calculate());
        discountField.addActionListener(event -> calculate());

        JPanel settings = new JPanel(new BorderLayout(8, 0));
        settings.setOpaque(false);
        settings.add(SwingStyle.heading("Quotation summary"), BorderLayout.WEST);
        settings.add(SwingStyle.row(discount, calculate), BorderLayout.EAST);
        calculationStatus.setVisible(false);

        JPanel totals = new JPanel(new GridLayout(1, 3, 8, 0));
        totals.setOpaque(false);
        totals.add(totalMetric("Subtotal", subtotalLabel, false));
        totals.add(totalMetric("Discount amount", discountLabel, false));
        totals.add(totalMetric("Total", totalLabel, true));

        JPanel panel = SwingStyle.card();
        panel.add(settings, BorderLayout.NORTH);
        panel.add(totals, BorderLayout.CENTER);
        panel.add(calculationStatus, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel totalMetric(String title, JLabel value, boolean emphasis) {
        JPanel metric = new JPanel(new BorderLayout(0, 1));
        JLabel caption = SwingStyle.muted(title);
        metric.setBackground(emphasis ? SwingStyle.NAVY : SwingStyle.CANVAS);
        metric.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        if (emphasis) {
            caption.setForeground(SwingStyle.WHITE);
        }
        value.setForeground(emphasis ? SwingStyle.WHITE : SwingStyle.INK);
        value.setFont(SwingStyle.BODY.deriveFont(Font.BOLD, 17f));
        value.setName(title);
        value.getAccessibleContext().setAccessibleName(title);
        metric.add(caption, BorderLayout.NORTH);
        metric.add(value, BorderLayout.CENTER);
        return metric;
    }

    private JPanel createEditorActions() {
        JButton create = new JButton("New Quote");
        JButton preview = new JButton("Preview Quote");
        JButton save = new JButton("Save Quote");
        SwingStyle.button(create, false);
        SwingStyle.button(preview, false);
        SwingStyle.button(save, true);
        create.addActionListener(event -> {
            if (confirmDiscard()) {
                resetEditor();
            }
        });
        preview.addActionListener(event -> previewQuote());
        save.addActionListener(event -> saveQuote());

        JPanel mode = new JPanel(new BorderLayout(0, 3));
        mode.setOpaque(false);
        mode.add(editorMode, BorderLayout.NORTH);
        mode.add(editorStatus, BorderLayout.CENTER);

        JPanel panel = SwingStyle.card();
        panel.add(mode, BorderLayout.WEST);
        panel.add(SwingStyle.row(create, preview, save), BorderLayout.EAST);
        return panel;
    }

    void refreshFromStore() {
        Client selectedClient = (Client) clientChoices.getSelectedItem();
        Offering selectedOffering = (Offering) offeringChoices.getSelectedItem();

        clientChoices.removeAllItems();
        for (int i = 0; i < dataStore.getClientCount(); i++) {
            clientChoices.addItem(dataStore.getClient(i));
        }
        clientChoices.setSelectedItem(dataStore.hasClient(selectedClient) ? selectedClient : null);

        offeringChoices.removeAllItems();
        for (int i = 0; i < dataStore.getOfferingCount(); i++) {
            offeringChoices.addItem(dataStore.getOffering(i));
        }
        offeringChoices.setSelectedItem(dataStore.hasOffering(selectedOffering) ? selectedOffering : null);

        refreshQuoteList();
        refreshEditorMode();
        itemList.repaint();
    }

    private void addItem() {
        try {
            Offering offering = (Offering) offeringChoices.getSelectedItem();
            if (offering == null) {
                throw new IllegalArgumentException("Select an offering. Add one in Offerings if needed.");
            }
            double quantity = readNumber(quantityField, "quantity");
            QuoteItem item = new QuoteItem(offering, quantity, offering.getUnitPrice());
            getDraft().addItem(item);
            itemModel.addElement(item);
            quantityField.setText("");
            refreshItemTotals();
            refreshEditorMode();
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
        refreshItemTotals();
        refreshEditorMode();
    }

    private void calculate() {
        try {
            double discount = readNumber(discountField, "discount");
            getDraft().setDiscountPercent(discount);
            calculationStatus.setVisible(false);
            showTotals();
        } catch (IllegalArgumentException error) {
            showError(error.getMessage());
        }
    }

    private void saveQuote() {
        try {
            Quote changes = readQuote();
            if (editingQuote == null) {
                dataStore.addQuote(changes);
                editingQuote = changes;
            } else {
                dataStore.updateQuote(editingQuote, changes);
            }
            showAllQuotes();
            quoteList.setSelectedValue(editingQuote, true);
            loadQuote(editingQuote);
        } catch (IllegalArgumentException | IllegalStateException error) {
            showError(error.getMessage());
        }
    }

    private Quote readQuote() {
        Client client = selectedClient();
        Quote changes = new Quote(client);
        if (draft != null) {
            changes.updateFrom(draft);
            changes.setClient(client);
        }
        changes.setDate(readDate());
        changes.setStatus((Quote.Status) statusChoices.getSelectedItem());
        changes.setPreparedBy(preparedByField.getText());
        changes.setDiscountPercent(readNumber(discountField, "discount"));
        return changes;
    }

    private void previewQuote() {
        try {
            Quote changes = readQuote();
            dataStore.validateQuote(changes);
            String reference = dataStore.getQuoteReference(editingQuote);
            new QuotePreview(this, changes, reference).setVisible(true);
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
        if (confirmDiscard()) {
            loadQuote(selected);
        }
    }

    private void loadQuote(Quote quote) {
        editingQuote = quote;
        draft = new Quote(quote.getClient());
        draft.updateFrom(quote);

        clientChoices.setSelectedItem(draft.getClient());
        offeringChoices.setSelectedItem(null);
        dateField.setText(draft.getDate().toString());
        preparedByField.setText(draft.getPreparedBy());
        statusChoices.setSelectedItem(draft.getStatus());
        discountField.setText(Double.toString(draft.getDiscountPercent()));
        quantityField.setText("");
        itemModel.clear();
        for (QuoteItem item : draft.getItems()) {
            itemModel.addElement(item);
        }
        calculationStatus.setVisible(false);
        refreshEditorMode();
        showTotals();
    }

    private void resetEditor() {
        editingQuote = null;
        draft = null;
        initialDate = LocalDate.now().toString();
        quoteList.clearSelection();
        clientChoices.setSelectedItem(null);
        offeringChoices.setSelectedItem(null);
        dateField.setText(initialDate);
        preparedByField.setText("");
        statusChoices.setSelectedItem(Quote.Status.DRAFT);
        discountField.setText("0");
        quantityField.setText("");
        itemModel.clear();
        calculationStatus.setVisible(false);
        refreshEditorMode();
        showTotals();
    }

    boolean confirmDiscard() {
        return !hasUnsavedChanges() || JOptionPane.showConfirmDialog(this,
                "Discard the unsaved changes to this quotation?", "Unsaved quotation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private boolean hasUnsavedChanges() {
        if (editingQuote == null) {
            return clientChoices.getSelectedItem() != null || !itemModel.isEmpty()
                    || !initialDate.equals(dateField.getText().trim())
                    || !preparedByField.getText().trim().isEmpty()
                    || statusChoices.getSelectedItem() != Quote.Status.DRAFT
                    || !discountMatches(0);
        }
        return clientChoices.getSelectedItem() != editingQuote.getClient()
                || statusChoices.getSelectedItem() != editingQuote.getStatus()
                || !editingQuote.getDate().toString().equals(dateField.getText().trim())
                || !editingQuote.getPreparedBy().equals(preparedByField.getText().trim())
                || !discountMatches(editingQuote.getDiscountPercent())
                || !draft.getItems().equals(editingQuote.getItems());
    }

    private boolean discountMatches(double percent) {
        try {
            return readNumber(discountField, "discount") == percent;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private void deleteQuote() {
        Quote selected = quoteList.getSelectedValue();
        if (selected == null) {
            showError("Select a saved quotation to delete.");
            return;
        }
        String message = "Delete " + dataStore.getQuoteReference(selected) + "?";
        if (selected == editingQuote && hasUnsavedChanges()) {
            message += " Your unsaved changes will also be discarded.";
        }
        if (JOptionPane.showConfirmDialog(this, message, "Delete quotation",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (!dataStore.removeQuote(selected)) {
                showError("The selected quotation could not be found.");
                return;
            }
        } catch (IllegalStateException error) {
            showError(error.getMessage());
            return;
        }
        if (selected == editingQuote) {
            resetEditor();
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
        Client client = (Client) clientChoices.getSelectedItem();
        if (client == null) {
            throw new IllegalArgumentException("Select a client. Add one in Clients if needed.");
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
        double discount = draft == null ? 0 : draft.getDiscountAmount();
        subtotalLabel.setText(RecordRenderer.amount(subtotal));
        discountLabel.setText(discount == 0 ? RecordRenderer.amount(0) : "-" + RecordRenderer.amount(discount));
        totalLabel.setText(RecordRenderer.amount(total));
        itemStatus.setText(itemModel.isEmpty() ? "No items yet. Choose an offering and enter a quantity."
                : itemModel.size() + (itemModel.size() == 1 ? " item" : " items"));
    }

    private void refreshItemTotals() {
        try {
            draft.setDiscountPercent(readNumber(discountField, "discount"));
            calculationStatus.setVisible(false);
        } catch (IllegalArgumentException error) {
            calculationStatus.setVisible(true);
        }
        showTotals();
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
        results.setText(quoteModel.isEmpty()
                ? (activeSearch.isEmpty() ? "No saved quotations yet." : "No quotations match your search.")
                : quoteModel.size() + (quoteModel.size() == 1 ? " quotation shown" : " quotations shown"));
    }

    private void refreshEditorMode() {
        editorMode.setText(editingQuote == null ? "New quotation"
                : "Editing " + dataStore.getQuoteReference(editingQuote));
        if (hasUnsavedChanges()) {
            editorStatus.setText("Unsaved changes");
        } else {
            editorStatus.setText(editingQuote == null ? "Number assigned on save" : "All changes saved");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Quotation", JOptionPane.ERROR_MESSAGE);
    }
}
