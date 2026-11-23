package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

public class QuotesPanel extends JPanel {
    private final DataStore store;
    private final DefaultListModel<Quote> results = new DefaultListModel<>();
    private final JList<Quote> list = new JList<>(results);
    private final JTextField search = new JTextField();
    private final JTextArea detail = MainFrame.detailArea();
    private final JButton edit = new JButton("Edit");
    private final JButton delete = new JButton("Delete");
    private final JButton copy = new JButton("Copy quote");

    public QuotesPanel(DataStore store) {
        super(new BorderLayout());
        this.store = store;

        JButton find = new JButton("Search");
        JButton clear = new JButton("Clear");
        JPanel quoteList = MainFrame.listPanel("Saved quotes", list, search, find, clear);

        JButton add = MainFrame.primaryButton("New quote");
        JPanel actions = MainFrame.actionRow();
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(copy);
        add(MainFrame.workspace("Quotes", "Prepare prices for a project and track the client's response.",
                quoteList, "Quote preview", detail, actions));

        find.addActionListener(event -> refresh(list.getSelectedValue()));
        search.addActionListener(event -> refresh(list.getSelectedValue()));
        clear.addActionListener(event -> {
            search.setText("");
            refresh(list.getSelectedValue());
        });
        list.addListSelectionListener(event -> showQuote());
        add.addActionListener(event -> editQuote(null));
        edit.addActionListener(event -> editQuote(list.getSelectedValue()));
        delete.addActionListener(event -> deleteQuote());
        copy.addActionListener(event -> {
            detail.selectAll();
            detail.copy();
            detail.setCaretPosition(0);
        });

        refresh(null);
    }

    private void refresh(Quote selection) {
        results.clear();
        ArrayList<Quote> quotes = store.getQuotes();
        for (int i = quotes.size() - 1; i >= 0; i--) {
            Quote quote = quotes.get(i);
            if (MainFrame.matches(quote + " " + quote.getClient().getName(), search.getText())) {
                results.addElement(quote);
            }
        }
        list.setSelectedValue(selection, true);
        showQuote();
    }

    private void showQuote() {
        Quote quote = list.getSelectedValue();
        edit.setEnabled(quote != null);
        delete.setEnabled(quote != null);
        copy.setEnabled(quote != null);

        if (quote != null) {
            detail.setText(quoteText(quote));
        } else if (results.isEmpty() && !search.getText().trim().isEmpty()) {
            detail.setText("No quotes match your search.");
        } else if (results.isEmpty()) {
            detail.setText("Add a client and a service in the other tabs, then choose New quote.");
        } else {
            detail.setText("Select a quote to see what is included.");
        }
        detail.setCaretPosition(0);
    }

    private String quoteText(Quote quote) {
        Client client = quote.getClient();
        String text = "QUOTATION  " + quote.getReference() + "\n"
                + quote.getProject() + "\n\n"
                + "Prepared for " + client.getName() + "\n";
        if (!client.getEmail().isEmpty()) {
            text += client.getEmail() + "\n";
        }
        if (!client.getPhone().isEmpty()) {
            text += client.getPhone() + "\n";
        }
        text += "Issued " + quote.getDate() + "\n\nSERVICES\n";

        for (QuoteItem item : quote.getItems()) {
            text += "\n" + item.getOffering().getName() + "\n"
                    + "    " + item.getQuantity() + " x "
                    + MainFrame.money(item.getOffering().getRate()) + " per "
                    + item.getOffering().getUnit() + " = " + MainFrame.money(item.getLineTotal()) + "\n";
        }

        text += "\nSubtotal: " + MainFrame.money(quote.getSubtotal())
                + "\nDiscount (" + quote.getDiscount() + "%): "
                + MainFrame.money(quote.getSubtotal() - quote.getTotal())
                + "\nTotal (USD): " + MainFrame.money(quote.getTotal()) + "\n";
        if (!quote.getNotes().isEmpty()) {
            text += "\nNotes: " + quote.getNotes() + "\n";
        }
        return text;
    }

    private void editQuote(Quote existing) {
        ArrayList<Offering> services = store.getOfferings();
        JComboBox<Client> client = clientChoices(existing);
        if (existing == null && (client.getItemCount() == 0 || services.isEmpty())) {
            JOptionPane.showMessageDialog(this, "Add a client and a service before creating a quote.",
                    "New quote", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField project = new JTextField(24);
        JTextField notes = new JTextField(24);
        JTextField discount = new JTextField("0", 4);
        JComboBox<Quote.Status> status = new JComboBox<>(Quote.Status.values());
        ArrayList<QuoteItem> items = new ArrayList<>();
        if (existing != null) {
            project.setText(existing.getProject());
            notes.setText(existing.getNotes());
            discount.setText("" + existing.getDiscount());
            status.setSelectedItem(existing.getStatus());
            items = existing.getItems();
        }

        JPanel details = new JPanel(new GridLayout(0, 2, 8, 6));
        details.add(new JLabel("Project name"));
        details.add(project);
        details.add(new JLabel("Client"));
        details.add(client);
        details.add(new JLabel("Status"));
        details.add(status);
        details.add(new JLabel("Notes for the client (optional)"));
        details.add(notes);

        JPanel editor = new JPanel(new BorderLayout(0, 14));
        editor.add(details, BorderLayout.NORTH);
        editor.add(itemEditor(services, items, discount), BorderLayout.CENTER);

        int number = store.getNextQuoteNumber();
        LocalDate date = LocalDate.now();
        String title = "New quote";
        if (existing != null) {
            number = existing.getNumber();
            date = existing.getDate();
            title = "Edit quote " + existing.getReference();
        }

        String[] buttons = {"Save quote", "Cancel"};
        while (JOptionPane.showOptionDialog(this, editor, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[0]) == JOptionPane.OK_OPTION) {
            try {
                int percent = readDiscount(discount.getText());
                Quote quote = new Quote(number, date, (Client) client.getSelectedItem(),
                        project.getText(), (Quote.Status) status.getSelectedItem(), percent,
                        notes.getText(), items);
                ArrayList<Quote> quotes = store.getQuotes();
                if (existing == null) {
                    quotes.add(quote);
                } else {
                    quotes.set(quotes.indexOf(existing), quote);
                }
                store.saveQuotes(quotes);
                search.setText("");
                refresh(quote);
                return;
            } catch (IllegalArgumentException | IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JComboBox<Client> clientChoices(Quote existing) {
        JComboBox<Client> choices = new JComboBox<>();
        choices.setPreferredSize(new Dimension(235, choices.getPreferredSize().height));
        Client selected = null;
        for (Client client : store.getClients()) {
            choices.addItem(client);
            if (existing != null && client.hasSameContact(existing.getClient())) {
                selected = client;
            }
        }

        if (existing != null) {
            if (selected == null) {
                selected = existing.getClient();
                // Keep the saved contact available even if the directory entry changed.
                choices.insertItemAt(selected, 0);
            }
            choices.setSelectedItem(selected);
        }
        return choices;
    }

    private JPanel itemEditor(ArrayList<Offering> services, ArrayList<QuoteItem> items, JTextField discount) {
        JComboBox<Offering> service = new JComboBox<>();
        service.setPreferredSize(new Dimension(216, service.getPreferredSize().height));
        for (Offering offering : services) {
            service.addItem(offering);
        }
        JTextField quantity = new JTextField("1", 4);
        JButton addItem = new JButton("Add service");
        addItem.setEnabled(!services.isEmpty());
        JLabel rate = new JLabel();
        showRate(service, rate);
        service.addActionListener(event -> showRate(service, rate));

        JPanel addRow = MainFrame.actionRow();
        addRow.add(new JLabel("Service"));
        addRow.add(service);
        addRow.add(new JLabel("Qty"));
        addRow.add(quantity);
        addRow.add(addItem);

        JPanel servicePicker = new JPanel(new BorderLayout(0, 5));
        servicePicker.add(addRow, BorderLayout.NORTH);
        servicePicker.add(rate, BorderLayout.SOUTH);

        DefaultListModel<String> lines = new DefaultListModel<>();
        for (QuoteItem item : items) {
            lines.addElement(lineText(item));
        }
        JList<String> itemList = new JList<>(lines);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFixedCellHeight(24);
        JScrollPane itemScroll = new JScrollPane(itemList);
        itemScroll.setPreferredSize(new Dimension(485, 140));

        JButton removeItem = new JButton("Remove selected");
        removeItem.setEnabled(false);
        itemList.addListSelectionListener(event -> removeItem.setEnabled(itemList.getSelectedIndex() >= 0));

        JButton calculate = new JButton("Calculate");
        JLabel totals = MainFrame.heading("", 13);
        JPanel controls = MainFrame.actionRow();
        controls.add(removeItem);
        controls.add(new JLabel("Discount (%)"));
        controls.add(discount);
        controls.add(calculate);

        JPanel summary = new JPanel(new BorderLayout(0, 7));
        summary.add(controls, BorderLayout.NORTH);
        summary.add(totals, BorderLayout.SOUTH);

        addItem.addActionListener(event -> {
            try {
                int units = Integer.parseInt(quantity.getText().trim());
                QuoteItem item = new QuoteItem((Offering) service.getSelectedItem(), units);
                items.add(item);
                lines.addElement(lineText(item));
                itemList.setSelectedIndex(lines.size() - 1);
                itemList.ensureIndexIsVisible(lines.size() - 1);
                quantity.setText("1");
                showTotals(items, discount, totals);
            } catch (NumberFormatException error) {
                JOptionPane.showMessageDialog(itemScroll, "Enter a whole number for the quantity.",
                        "Add service", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(itemScroll, error.getMessage(),
                        "Add service", JOptionPane.ERROR_MESSAGE);
            }
        });
        removeItem.addActionListener(event -> {
            int index = itemList.getSelectedIndex();
            items.remove(index);
            lines.remove(index);
            showTotals(items, discount, totals);
        });
        calculate.addActionListener(event -> showTotals(items, discount, totals));
        discount.addActionListener(event -> showTotals(items, discount, totals));
        showTotals(items, discount, totals);

        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.add(servicePicker, BorderLayout.NORTH);
        panel.add(itemScroll, BorderLayout.CENTER);
        panel.add(summary, BorderLayout.SOUTH);
        return panel;
    }

    private void showRate(JComboBox<Offering> service, JLabel rate) {
        Offering selected = (Offering) service.getSelectedItem();
        if (selected == null) {
            rate.setText("Add a service in the Services tab to include new items.");
        } else {
            rate.setText(MainFrame.money(selected.getRate()) + " per " + selected.getUnit());
        }
    }

    private String lineText(QuoteItem item) {
        return item.getOffering().getName() + "   /   " + item.getQuantity() + " x "
                + MainFrame.money(item.getOffering().getRate()) + "   =   "
                + MainFrame.money(item.getLineTotal());
    }

    private int readDiscount(String text) {
        try {
            int discount = Integer.parseInt(text.trim());
            if (discount >= 0 && discount <= 100) {
                return discount;
            }
        } catch (NumberFormatException error) {
            // Show the same message for a non-integer and a number outside the range.
        }
        throw new IllegalArgumentException("Enter a whole-number discount from 0 to 100.");
    }

    private void showTotals(ArrayList<QuoteItem> items, JTextField discount, JLabel totals) {
        try {
            int percent = readDiscount(discount.getText());
            long subtotal = Quote.calculateSubtotal(items);
            totals.setText("Subtotal " + MainFrame.money(subtotal)
                    + "     |     Total " + MainFrame.money(Quote.calculateTotal(subtotal, percent)) + " USD");
        } catch (IllegalArgumentException error) {
            totals.setText(error.getMessage());
        }
    }

    private void deleteQuote() {
        Quote quote = list.getSelectedValue();
        int answer = JOptionPane.showConfirmDialog(this, "Delete quote " + quote.getReference() + "?",
                "Delete quote", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                ArrayList<Quote> quotes = store.getQuotes();
                quotes.remove(quote);
                store.saveQuotes(quotes);
                refresh(null);
            } catch (IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(),
                        "Delete quote", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
