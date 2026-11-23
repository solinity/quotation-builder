package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

final class SwingStyle {
    static final Color NAVY = new Color(22, 39, 61);
    static final Color ACCENT = new Color(31, 87, 112);
    static final Color INK = new Color(31, 48, 69);
    static final Color MUTED = new Color(77, 96, 116);
    static final Color CANVAS = new Color(239, 244, 248);
    static final Color LINE = new Color(210, 221, 231);
    static final Color WHITE = Color.WHITE;
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private SwingStyle() {
    }

    static void page(JPanel panel) {
        panel.setLayout(new BorderLayout(14, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        panel.setBackground(CANVAS);
    }

    static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return panel;
    }

    static JPanel row(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 2));
        panel.setOpaque(false);
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(INK);
        label.setFont(BODY.deriveFont(Font.BOLD, 16f));
        return label;
    }

    static void muted(JLabel label) {
        label.setFont(BODY);
        label.setForeground(MUTED);
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        muted(label);
        return label;
    }

    static void button(JButton button, boolean primary) {
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        buttonColors(button, primary);
        button.addPropertyChangeListener("enabled", event -> buttonColors(button, primary));
    }

    private static void buttonColors(JButton button, boolean primary) {
        boolean highlighted = primary && button.isEnabled();
        button.setForeground(highlighted ? WHITE : NAVY);
        button.setBackground(highlighted ? ACCENT : CANVAS);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(highlighted ? ACCENT : LINE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    static void field(JComponent input) {
        input.setFont(BODY);
        input.setForeground(INK);
        if (input instanceof JTextField) {
            input.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        }
    }

    static JPanel searchBar(String labelText, JTextField field, JButton search, JButton showAll) {
        field(field);
        button(search, false);
        button(showAll, false);
        JLabel label = muted(labelText);
        label.setLabelFor(field);
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(row(search, showAll), BorderLayout.SOUTH);
        return panel;
    }

    static JScrollPane list(JList<?> list, String name, int rowHeight, int rows) {
        list.getAccessibleContext().setAccessibleName(name);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(rowHeight);
        list.setVisibleRowCount(rows);
        list.setSelectionBackground(ACCENT);
        list.setSelectionForeground(WHITE);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setMinimumSize(new Dimension(0, rowHeight + 2));
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        return scroll;
    }
}
