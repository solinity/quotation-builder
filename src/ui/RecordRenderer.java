package ui;

import java.awt.Component;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

class RecordRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore;

    RecordRenderer(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean selected, boolean hasFocus) {
        return super.getListCellRendererComponent(list, label(value), index, selected, hasFocus);
    }

    String label(Object value) {
        if (value instanceof Client) {
            Client client = (Client) value;
            return "#" + dataStore.getClientNumber(client) + "  " + client.getName()
                    + "  |  " + client.getEmailOrDefault() + "  |  " + client.getPhoneOrDefault();
        }
        if (value instanceof Offering) {
            Offering offering = (Offering) value;
            int number = dataStore.getOfferingNumber(offering);
            String prefix = number > 0 ? "#" + number + "  " : "";
            String description = offering.getDescription().isEmpty() ? "" : "  |  " + offering.getDescription();
            return prefix + offering.getName() + "  |  " + amount(offering.getUnitPrice()) + description;
        }
        if (value instanceof Quote) {
            Quote quote = (Quote) value;
            return "#" + dataStore.getQuoteNumber(quote) + "  " + quote.getClient().getName()
                    + "  |  " + quote.getDate() + "  |  " + quote.getStatus() + "  |  " + amount(quote.getTotal());
        }
        if (value instanceof QuoteItem) {
            QuoteItem item = (QuoteItem) value;
            Offering offering = item.getOffering();
            int number = dataStore.getOfferingNumber(offering);
            String prefix = number > 0 ? "#" + number + "  " : "[Deleted] ";
            double discount = item.getDiscountPercent();
            String discountLabel = discount > 0 ? " (" + discount + "% off)" : "";
            return prefix + offering.getName() + "  |  " + item.getQuantity()
                    + " x " + amount(item.getUnitPrice()) + discountLabel + " = " + amount(item.getLineTotal());
        }
        return "";
    }

    static String amount(double value) {
        String format = value >= 1.0e12 ? "%.2e" : "%.2f";
        return String.format(Locale.ROOT, format, value);
    }
}
