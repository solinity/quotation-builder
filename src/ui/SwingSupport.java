package ui;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

final class SwingSupport {
    private SwingSupport() {
    }

    static JButton button(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }

    static Path openFile(Component owner, String title, Path directory) {
        return chooseFile(owner, title, FileDialog.LOAD, directory, null);
    }

    static Path saveAs(Component owner, String title, Path directory, String filename, String extension) {
        Path file = chooseFile(owner, title, FileDialog.SAVE, directory, filename);
        if (file == null || file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
            return file;
        }

        // The file dialog only checked the name the user entered.
        Path target = file.resolveSibling(file.getFileName() + extension);
        if (Files.exists(target) && JOptionPane.showConfirmDialog(owner,
                "Replace the existing file?\n" + target, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
        }
        return target;
    }

    private static Path chooseFile(Component owner, String title, int mode, Path directory, String filename) {
        FileDialog dialog = new FileDialog(JOptionPane.getFrameForComponent(owner), title, mode);
        try {
            if (directory != null) {
                dialog.setDirectory(directory.toString());
            }
            if (filename != null) {
                dialog.setFile(filename);
            }
            dialog.setVisible(true);
            File[] files = dialog.getFiles();
            return files.length == 0 ? null : files[0].toPath().toAbsolutePath().normalize();
        } finally {
            dialog.dispose();
        }
    }

    static final class FormPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private int row;

        FormPanel() {
            super(new GridBagLayout());
        }

        JLabel addField(String text, JComponent input) {
            JLabel label = new JLabel(text);
            label.setLabelFor(input);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = row;
            constraints.anchor = GridBagConstraints.BASELINE_LEADING;
            int topGap = row == 0 ? 0 : 6;
            constraints.insets = new Insets(topGap, 0, 0, 16);
            add(label, constraints);

            constraints.gridx = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(topGap, 0, 0, 0);
            add(input, constraints);
            row++;
            return label;
        }
    }

    static final class RecordRenderer extends DefaultListCellRenderer {
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
                String description = offering.getDescription().replace('\n', ' ').replace('\r', ' ');
                String suffix = description.isEmpty() ? "" : "  |  " + description;
                return prefix + offering.getName() + "  |  " + amount(offering.getUnitPrice()) + suffix;
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

        private String amount(double value) {
            return dataStore.getCurrencySettings().format(value);
        }
    }
}
