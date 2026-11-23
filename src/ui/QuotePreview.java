package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import model.Client;
import model.Quote;
import model.QuoteItem;

final class QuotePreview extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String RULE = "----------------------------------------------------------------";

    private final JTextArea document = new JTextArea(24, 68);
    private final JLabel exportStatus = SwingStyle.muted(" ");
    private final String filename;

    QuotePreview(Component owner, Quote quote, String reference) {
        super(SwingUtilities.getWindowAncestor(owner), "Quotation Preview", Dialog.ModalityType.APPLICATION_MODAL);
        filename = reference.isEmpty() ? "quotation-draft.txt" : reference + ".txt";

        document.setText(format(quote, reference));
        document.setEditable(false);
        document.setLineWrap(true);
        document.setWrapStyleWord(true);
        document.setCaretPosition(0);
        document.setFont(SwingStyle.MONO.deriveFont(13f));
        document.setForeground(SwingStyle.NAVY);
        document.setBorder(BorderFactory.createEmptyBorder(14, 17, 14, 17));
        document.getAccessibleContext().setAccessibleName("Quotation document");

        JScrollPane page = new JScrollPane(document);
        page.setBorder(BorderFactory.createLineBorder(SwingStyle.LINE));

        JButton export = new JButton("Export as Text");
        JButton close = new JButton("Close");
        SwingStyle.button(export, true);
        SwingStyle.button(close, false);
        export.addActionListener(event -> exportText());
        close.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(exportStatus, BorderLayout.CENTER);
        footer.add(SwingStyle.row(close, export), BorderLayout.EAST);

        JPanel content = new JPanel();
        SwingStyle.page(content);
        content.add(SwingStyle.heading("Quotation preview"), BorderLayout.NORTH);
        content.add(page, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(620, 375));
        setLocationRelativeTo(owner);
    }

    private static String format(Quote quote, String reference) {
        StringBuilder text = new StringBuilder();
        text.append("FREELANCE QUOTATION\n");
        text.append(RULE).append('\n');
        text.append("Quotation No.   ").append(reference.isEmpty() ? "Pending save" : reference).append('\n');
        text.append("Date            ").append(quote.getDate()).append('\n');
        text.append("Status          ").append(quote.getStatus()).append("\n\n");
        if (!quote.getPreparedBy().isEmpty()) {
            text.append("FROM\n").append(quote.getPreparedBy()).append("\n\n");
        }

        Client client = quote.getClient();
        text.append("CLIENT\n").append(client.getName()).append('\n');
        if (!client.getEmail().isEmpty()) {
            text.append(client.getEmail()).append('\n');
        }
        if (!client.getPhone().isEmpty()) {
            text.append(client.getPhone()).append('\n');
        }

        text.append('\n').append(RecordRenderer.columns("SERVICE / PRODUCT", "QTY / HOURS", "UNIT PRICE", "LINE TOTAL"));
        text.append('\n').append(RULE).append('\n');
        for (QuoteItem item : quote.getItems()) {
            String name = RecordRenderer.oneLine(item.getOffering().getName());
            if (name.length() > 23) {
                text.append(name).append('\n');
                name = "";
            }
            text.append(RecordRenderer.columns(name, RecordRenderer.quantity(item.getQuantity()),
                    RecordRenderer.amount(item.getUnitPrice()), RecordRenderer.amount(item.getLineTotal())));
            text.append('\n');
        }
        text.append(RULE).append('\n');
        appendAmount(text, "Subtotal", RecordRenderer.amount(quote.getSubtotal()));
        String discount = RecordRenderer.amount(quote.getDiscountAmount());
        appendAmount(text, "Discount (" + quote.getDiscountPercent() + "%)",
                quote.getDiscountAmount() == 0 ? discount : "-" + discount);
        appendAmount(text, "TOTAL", RecordRenderer.amount(quote.getTotal()));
        text.append("\nThank you for considering this quotation.\n");
        return text.toString();
    }

    private static void appendAmount(StringBuilder text, String label, String amount) {
        text.append(String.format(Locale.ROOT, "%50s %13s", label, amount)).append('\n');
    }

    private void exportText() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export quotation as text");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(new File(filename));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            file = new File(file.getPath() + ".txt");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(this, "Replace " + file.getName() + "?",
                "Replace file", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(document.getText());
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, "Could not export the quotation. Choose a writable location.",
                    "Export quotation", JOptionPane.ERROR_MESSAGE);
            return;
        }
        exportStatus.setText("Exported " + file.getName());
    }
}
