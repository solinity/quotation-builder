package ui;

import static storage.DataStore.numberOf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import model.CatalogEntry.Client;
import model.CatalogEntry.Offering;
import model.Quote;
import model.Quote.Item;
import storage.DataStore;

final class SwingSupport {
    private SwingSupport() {
    }

    static JButton button(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }

    static JButton formButton(String text, Component owner, String title, Runnable action) {
        return button(text, event -> {
            try {
                action.run();
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(owner, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    static JPanel row(int horizontalGap, int verticalGap, Component... controls) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, horizontalGap, verticalGap));
        for (Component control : controls) {
            panel.add(control);
        }
        return panel;
    }

    static JPanel borderPanel(String title, int gap, Component top, Component center, Component bottom) {
        JPanel panel = new JPanel(new BorderLayout(0, gap));
        if (title != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }
        if (top != null) {
            panel.add(top, BorderLayout.NORTH);
        }
        if (center != null) {
            panel.add(center, BorderLayout.CENTER);
        }
        if (bottom != null) {
            panel.add(bottom, BorderLayout.SOUTH);
        }
        return panel;
    }

    static JLabel labelFor(String text, JComponent input) {
        JLabel label = new JLabel(text);
        label.setLabelFor(input);
        return label;
    }

    static void configureList(JList<?> list, RecordRenderer renderer, String accessibleName) {
        list.setCellRenderer(renderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.getAccessibleContext().setAccessibleName(accessibleName);
    }

    static final class RecordList<T> extends JList<T> {
        private static final long serialVersionUID = 1L;
        private final JTextField search = new JTextField(18);
        private final JLabel status = new JLabel();
        private final Function<String, List<? extends T>> find;
        private final String emptyMessage;
        private String query = "";

        RecordList(String name, String emptyMessage, RecordRenderer renderer, Function<String, List<? extends T>> find) {
            this.find = find;
            this.emptyMessage = emptyMessage;
            configureList(this, renderer, name);
            search.addActionListener(event -> search());
            refresh();
        }

        JPanel panel(String title, String label, JPanel actions) {
            JLabel searchLabel = labelFor(label, search);
            JButton findButton = button("Search", event -> search());
            JButton showAllButton = button("Show All", event -> showAll());
            JScrollPane results = new JScrollPane(this);
            if (actions == null) {
                return borderPanel(title, 5, row(8, 5, searchLabel, search, findButton, showAllButton), results, status);
            }
            JPanel searchBar = borderPanel(null, 3, searchLabel, search, row(6, 3, findButton, showAllButton, status));
            return borderPanel(title, 5, searchBar, results, actions);
        }

        void showAll() {
            search.setText("");
            search();
        }

        private void search() {
            query = search.getText().trim();
            refresh();
        }

        void refresh() {
            T selected = getSelectedValue();
            List<? extends T> records = find.apply(query);
            setListData(new Vector<>(records));
            setSelectedValue(selected, true);
            status.setText(records.isEmpty() ? emptyMessage : records.size() + " shown");
        }
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
            dialog.setDirectory(directory == null ? null : directory.toString());
            dialog.setFile(filename);
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
            JLabel label = labelFor(text, input);
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
                return "#" + numberOf(dataStore.getClients(), client) + "  " + client.getName()
                        + "  |  " + client.getEmailOrDefault() + "  |  " + client.getPhoneOrDefault();
            }
            if (value instanceof Offering) {
                Offering offering = (Offering) value;
                int number = numberOf(dataStore.getOfferings(), offering);
                String prefix = number > 0 ? "#" + number + "  " : "";
                String description = offering.getDescription().replace('\n', ' ').replace('\r', ' ');
                String suffix = description.isEmpty() ? "" : "  |  " + description;
                return prefix + offering.getName() + "  |  " + amount(offering.getUnitPrice()) + suffix;
            }
            if (value instanceof Quote) {
                Quote quote = (Quote) value;
                return "#" + numberOf(dataStore.getQuotes(), quote) + "  " + quote.getClient().getName()
                        + "  |  " + quote.getDate() + "  |  " + quote.getStatus() + "  |  " + amount(quote.getTotal());
            }
            if (value instanceof Item) {
                Item item = (Item) value;
                Offering offering = item.getOffering();
                int number = numberOf(dataStore.getOfferings(), offering);
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

    static final class RecordSearchField<T> extends JTextField {
        private static final long serialVersionUID = 1L;
        private static final int MAX_VISIBLE_RESULTS = 6;

        private final Function<String, List<T>> search;
        private final RecordRenderer renderer;
        private final JList<T> results = new JList<>();
        private final JScrollPane resultsPane = new JScrollPane(results);
        private final JLabel noMatches = new JLabel("No matching names");
        private final JPopupMenu suggestions = new JPopupMenu();

        private T selectedRecord;
        private boolean settingSelection;

        RecordSearchField(Function<String, List<T>> search, RecordRenderer renderer) {
            super(12);
            this.search = search;
            this.renderer = renderer;
            setToolTipText("Type a name to search");
            results.setCellRenderer(renderer);
            results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            results.setFixedCellHeight(results.getFontMetrics(results.getFont()).getHeight() + 4);
            results.setFocusable(false);
            resultsPane.setFocusable(false);
            resultsPane.getVerticalScrollBar().setFocusable(false);
            resultsPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            resultsPane.setBorder(BorderFactory.createEmptyBorder());
            noMatches.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            suggestions.setFocusable(false);
            suggestions.setLayout(new BorderLayout());
            suggestions.add(resultsPane, BorderLayout.CENTER);
            suggestions.add(noMatches, BorderLayout.SOUTH);
            registerListeners();
        }

        private void registerListeners() {
            getDocument().addUndoableEditListener(event -> {
                if (!settingSelection) {
                    selectedRecord = null;
                    showSuggestions();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    if (selectedRecord == null) {
                        showSuggestions();
                    } else {
                        selectAll();
                    }
                }

                @Override
                public void focusLost(FocusEvent event) {
                    suggestions.setVisible(false);
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    int key = event.getKeyCode();
                    if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
                        moveSelection(key == KeyEvent.VK_UP ? -1 : 1, event);
                    } else if (key == KeyEvent.VK_ESCAPE) {
                        suggestions.setVisible(false);
                    }
                }
            });
            addActionListener(event -> {
                T record = results.getSelectedValue();
                if (suggestions.isVisible() && record != null) {
                    chooseRecord(record);
                }
            });
            results.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    int index = results.locationToIndex(event.getPoint());
                    if (SwingUtilities.isLeftMouseButton(event) && index >= 0
                            && results.getCellBounds(index, index).contains(event.getPoint())) {
                        chooseRecord(results.getModel().getElementAt(index));
                    }
                }
            });
        }

        T getSelectedRecord() {
            return selectedRecord;
        }

        void setSelectedRecord(T record) {
            suggestions.setVisible(false);
            selectedRecord = record;
            settingSelection = true;
            try {
                setText(record == null ? "" : renderer.label(record));
                setCaretPosition(0);
            } finally {
                settingSelection = false;
            }
        }

        private void chooseRecord(T record) {
            setSelectedRecord(record);
            selectAll();
        }

        private void showSuggestions() {
            String query = getText().strip();
            if (query.isEmpty() || !isShowing()) {
                suggestions.setVisible(false);
                return;
            }

            List<T> matches = search.apply(query);
            results.setListData(new Vector<>(matches));
            boolean empty = matches.isEmpty();
            resultsPane.setVisible(!empty);
            noMatches.setVisible(empty);
            results.setFixedCellWidth(getWidth());
            int height = empty ? noMatches.getPreferredSize().height
                    : Math.min(matches.size(), MAX_VISIBLE_RESULTS) * results.getFixedCellHeight();
            suggestions.setPopupSize(getWidth(), height + suggestions.getInsets().top + suggestions.getInsets().bottom);
            if (!empty) {
                results.setSelectedIndex(0);
                results.ensureIndexIsVisible(0);
            }
            if (!suggestions.isVisible()) {
                suggestions.show(this, 0, getHeight());
            }
        }

        private void moveSelection(int step, KeyEvent event) {
            if (!suggestions.isVisible()) {
                if (selectedRecord != null) {
                    return;
                }
                showSuggestions();
            } else if (results.getModel().getSize() > 0) {
                int index = Math.max(0, Math.min(results.getSelectedIndex() + step, results.getModel().getSize() - 1));
                results.setSelectedIndex(index);
                results.ensureIndexIsVisible(index);
            }
            if (suggestions.isVisible()) {
                event.consume();
            }
        }
    }
}
