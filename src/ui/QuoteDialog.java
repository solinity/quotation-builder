package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import model.Quote;
import model.QuoteItem;

final class QuoteDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    QuoteDialog(Window owner, int number, Quote quote, CurrencySettings currency) {
        super(owner, "Quotation #" + number, ModalityType.DOCUMENT_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JEditorPane document = new JEditorPane("text/html", render(number, quote, currency));
        document.setEditable(false);
        document.getAccessibleContext().setAccessibleName("Quotation details");
        document.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        document.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(document);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 7));
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setMinimumSize(new Dimension(620, 360));
        setSize(820, 560);
        setLocationRelativeTo(owner);
    }

    private static String render(int number, Quote quote, CurrencySettings currency) {
        Client client = quote.getClient();
        StringBuilder html = new StringBuilder("<html><head><style>"
                + "body { font-family: sans-serif; font-size: 12pt; color: #192534; }"
                + "h1 { font-size: 25pt; color: #17365a; }"
                + "h2 { font-size: 11pt; color: #17365a; }"
                + ".description { color: #43556a; font-size: 10pt; }"
                + "</style></head><body><h1>Quotation #");
        html.append(number).append("</h1>")
                .append("<table width='100%' cellpadding='4' cellspacing='0'><tr>")
                .append("<td width='60%' valign='top'><h2>Client</h2><b>")
                .append(escape(client.getName())).append("</b><br>Email: ")
                .append(escape(client.getEmailOrDefault())).append("<br>Phone: ")
                .append(escape(client.getPhoneOrDefault())).append("</td>")
                .append("<td valign='top' align='right'><h2>Quote details</h2>Date: ")
                .append(quote.getDate()).append("<br>Status: ").append(quote.getStatus()).append("</td>")
                .append("</tr></table><br>");

        appendItems(html, quote, currency);
        appendTotals(html, quote, currency);
        return html.append("</body></html>").toString();
    }

    private static void appendItems(StringBuilder html, Quote quote, CurrencySettings currency) {
        html.append("<table width='100%' cellpadding='7' cellspacing='0'>")
                .append("<tr bgcolor='#dfeaf5'><th align='left' width='40%'>Offering</th>")
                .append("<th align='right'>Qty / hours</th><th align='right'>Unit price</th>")
                .append("<th align='right'>Discount</th><th align='right'>Line total</th></tr>");
        boolean shaded = false;
        for (QuoteItem item : quote.getItems()) {
            Offering offering = item.getOffering();
            html.append(shaded ? "<tr bgcolor='#f2f6fa'>" : "<tr>");
            html.append("<td valign='top'><b>").append(escape(offering.getName())).append("</b>");
            if (!offering.getDescription().isEmpty()) {
                html.append("<div class='description'>").append(escape(offering.getDescription())).append("</div>");
            }
            html.append("</td><td align='right' valign='top'>").append(number(item.getQuantity()))
                    .append("</td><td align='right' valign='top'>").append(currency.format(item.getUnitPrice()))
                    .append("</td><td align='right' valign='top'>").append(number(item.getDiscountPercent())).append("%")
                    .append("</td><td align='right' valign='top'>").append(currency.format(item.getLineTotal()))
                    .append("</td></tr>");
            shaded = !shaded;
        }
        html.append("</table>");
    }

    private static void appendTotals(StringBuilder html, Quote quote, CurrencySettings currency) {
        double subtotal = quote.getSubtotal();
        double total = quote.getTotal();
        html.append("<br><table width='100%' cellspacing='0' cellpadding='0'><tr><td width='49%'>&nbsp;</td><td>")
                .append("<table width='100%' cellpadding='7' cellspacing='0'>")
                .append("<tr><td>Subtotal</td><td align='right'>").append(currency.format(subtotal)).append("</td></tr>")
                .append("<tr><td>Quote discount (").append(number(quote.getDiscountPercent()))
                .append("%)</td><td align='right'>").append(currency.format(subtotal - total)).append("</td></tr>")
                .append("<tr bgcolor='#dfeaf5'><td><b>Total</b></td><td align='right'><b>")
                .append(currency.format(total)).append("</b></td></tr></table></td></tr></table>");
    }

    private static String number(double value) {
        String text = Double.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>");
    }
}
