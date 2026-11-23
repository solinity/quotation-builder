package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class RecordSearchField<T> extends JTextField {
    private static final long serialVersionUID = 1L;
    private static final int MAX_VISIBLE_RESULTS = 6;

    private final Function<String, List<T>> search;
    private final RecordRenderer renderer;
    private final DefaultListModel<T> matches = new DefaultListModel<>();
    private final JList<T> results = new JList<>(matches);
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

        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { queryChanged(); }
            @Override public void removeUpdate(DocumentEvent event) { queryChanged(); }
            @Override public void changedUpdate(DocumentEvent event) { queryChanged(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (selectedRecord != null) {
                    selectAll();
                } else {
                    showSuggestions();
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
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        moveSelection(1, event);
                        break;
                    case KeyEvent.VK_UP:
                        moveSelection(-1, event);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestions.setVisible(false);
                        break;
                    default:
                        break;
                }
            }
        });
        addActionListener(event -> {
            if (suggestions.isVisible() && results.getSelectedValue() != null) {
                chooseRecord(results.getSelectedValue());
            }
        });
        results.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Point point = event.getPoint();
                int index = results.locationToIndex(point);
                if (SwingUtilities.isLeftMouseButton(event) && index >= 0
                        && results.getCellBounds(index, index).contains(point)) {
                    chooseRecord(matches.get(index));
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
        setCaretPosition(getDocument().getLength());
        moveCaretPosition(0);
    }

    private void queryChanged() {
        if (!settingSelection) {
            selectedRecord = null;
            showSuggestions();
        }
    }

    private void showSuggestions() {
        String query = getText().strip();
        if (query.isEmpty() || !isShowing()) {
            suggestions.setVisible(false);
            return;
        }

        matches.clear();
        matches.addAll(search.apply(query));
        boolean empty = matches.isEmpty();
        resultsPane.setVisible(!empty);
        noMatches.setVisible(empty);
        results.setFixedCellWidth(getWidth());
        int height = empty ? noMatches.getPreferredSize().height
                : Math.min(matches.size(), MAX_VISIBLE_RESULTS) * results.getFixedCellHeight();
        suggestions.setPopupSize(new Dimension(getWidth(),
                height + suggestions.getInsets().top + suggestions.getInsets().bottom));
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
        } else if (!matches.isEmpty()) {
            int index = Math.max(0, Math.min(results.getSelectedIndex() + step, matches.size() - 1));
            results.setSelectedIndex(index);
            results.ensureIndexIsVisible(index);
        }
        if (suggestions.isVisible()) {
            event.consume();
        }
    }
}
