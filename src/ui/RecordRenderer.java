package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

class RecordRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;
    private final boolean compact;
    private final JPanel record = new JPanel(new GridLayout(2, 1, 0, 2));
    private final JLabel title = new JLabel();
    private final JLabel detail = new JLabel();
    private final JLabel serviceName = new JLabel();
    private final JLabel lineQuantity = new JLabel();
    private final JLabel unitPrice = new JLabel();
    private final JLabel lineTotal = new JLabel();
    private final JPanel itemRow = itemColumns(serviceName, lineQuantity, unitPrice, lineTotal);

    RecordRenderer(DataStore dataStore) {
        this(dataStore, false);
    }

    RecordRenderer(DataStore dataStore, boolean compact) {
        this.dataStore = dataStore;
        this.compact = compact;
        record.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        title.setFont(SwingStyle.BODY.deriveFont(Font.BOLD));
        detail.setFont(SwingStyle.BODY);
        record.add(title);
        record.add(detail);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean selected, boolean hasFocus) {
        Color background = selected ? list.getSelectionBackground()
                : (index % 2 == 0 ? SwingStyle.WHITE : new Color(245, 248, 251));
        Color foreground = selected ? list.getSelectionForeground() : SwingStyle.INK;
        if (value instanceof QuoteItem) {
            QuoteItem item = (QuoteItem) value;
            Offering offering = item.getOffering();
            String prefix = dataStore.hasOffering(offering) ? "" : "[Deleted] ";
            serviceName.setText(prefix + oneLine(offering.getName()));
            lineQuantity.setText(quantity(item.getQuantity()));
            unitPrice.setText(amount(item.getUnitPrice()));
            lineTotal.setText(amount(item.getLineTotal()));
            itemRow.setBackground(background);
            for (JLabel label : new JLabel[] { serviceName, lineQuantity, unitPrice, lineTotal }) {
                label.setForeground(foreground);
            }
            return itemRow;
        }
        String name = title(value);
        String notes = detail(value);
        if (compact) {
            String text = notes.isEmpty() ? name : name + "  |  " + notes;
            super.getListCellRendererComponent(list, text, index, selected, hasFocus);
            setFont(SwingStyle.BODY);
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            return this;
        }

        record.setBackground(background);
        title.setForeground(foreground);
        detail.setForeground(selected ? list.getSelectionForeground() : SwingStyle.MUTED);
        title.setText(name);
        detail.setText(notes);
        return record;
    }

    private String title(Object value) {
        if (value instanceof Client) {
            Client client = (Client) value;
            return "#" + dataStore.getClientNumber(client) + "  " + oneLine(client.getName());
        }
        if (value instanceof Offering) {
            Offering offering = (Offering) value;
            int number = dataStore.getOfferingNumber(offering);
            String prefix = number > 0 ? "#" + number + "  " : "";
            return prefix + oneLine(offering.getName());
        }
        if (value instanceof Quote) {
            Quote quote = (Quote) value;
            return dataStore.getQuoteReference(quote) + "  |  " + oneLine(quote.getClient().getName());
        }
        return "";
    }

    private String detail(Object value) {
        if (value instanceof Client) {
            Client client = (Client) value;
            if (compact) {
                return oneLine(client.getEmail().isEmpty() ? client.getPhone() : client.getEmail());
            }
            return oneLine(client.getEmailOrDefault() + "  |  " + client.getPhoneOrDefault());
        }
        if (value instanceof Offering) {
            Offering offering = (Offering) value;
            String price = amount(offering.getUnitPrice()) + " per unit";
            return compact || offering.getDescription().isEmpty() ? price
                    : price + "  |  " + oneLine(offering.getDescription());
        }
        if (value instanceof Quote) {
            Quote quote = (Quote) value;
            return quote.getDate() + "  |  " + quote.getStatus() + "  |  " + amount(quote.getTotal());
        }
        return "";
    }

    static JPanel itemHeader() {
        JLabel service = new JLabel("Service / product");
        JLabel qty = new JLabel("Qty / hours");
        JLabel price = new JLabel("Unit price");
        JLabel total = new JLabel("Line total");
        JPanel header = itemColumns(service, qty, price, total);
        header.setBackground(SwingStyle.CANVAS);
        for (JLabel label : new JLabel[] { service, qty, price, total }) {
            label.setFont(SwingStyle.BODY.deriveFont(Font.BOLD));
            label.setForeground(SwingStyle.NAVY);
        }
        return header;
    }

    private static JPanel itemColumns(JLabel service, JLabel qty, JLabel price, JLabel total) {
        JPanel numbers = new JPanel(new GridLayout(1, 3, 10, 0));
        numbers.setOpaque(false);
        numbers.setPreferredSize(new Dimension(315, 18));
        service.setFont(SwingStyle.BODY);
        for (JLabel label : new JLabel[] { qty, price, total }) {
            label.setFont(SwingStyle.BODY);
            label.setHorizontalAlignment(JLabel.RIGHT);
            numbers.add(label);
        }
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        row.add(service, BorderLayout.CENTER);
        row.add(numbers, BorderLayout.EAST);
        return row;
    }

    static String columns(String service, String quantity, String price, String total) {
        return String.format(Locale.ROOT, "%-23s %12s %13s %13s", service, quantity, price, total);
    }

    static String oneLine(String text) {
        return text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
    }

    static String quantity(double value) {
        String exact = Double.toString(value);
        if (exact.endsWith(".0")) {
            exact = exact.substring(0, exact.length() - 2);
        }
        return exact.length() <= 12 ? exact : String.format(Locale.ROOT, "%.4e", value);
    }

    static String amount(double value) {
        String format = value >= 1.0e12 ? "%.2e" : "%.2f";
        return String.format(Locale.ROOT, format, value);
    }
}
