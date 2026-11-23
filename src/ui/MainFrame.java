package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import storage.DataStore;

public class MainFrame extends JFrame {
    private static final Color INK = new Color(32, 50, 58);
    private static final Color MUTED = new Color(76, 97, 104);
    private static final Color ACCENT = new Color(33, 107, 99);
    private static final Color PAPER = new Color(247, 250, 249);

    public MainFrame(DataStore store) {
        super("Freelance Quotation Builder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setMinimumSize(new Dimension(830, 520));
        setLocationRelativeTo(null);

        JLabel appTitle = new JLabel("Freelance Quotation Builder");
        appTitle.setForeground(Color.WHITE);
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(INK);
        header.setBorder(BorderFactory.createEmptyBorder(17, 22, 17, 22));
        header.add(appTitle);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Quotes", new QuotesPanel(store));
        tabs.addTab("Clients", new ClientsPanel(store));
        tabs.addTab("Services", new OfferingsPanel(store));
        add(tabs, BorderLayout.CENTER);
    }

    static JPanel workspace(String title, String description, JPanel listPanel,
            String detailTitle, JTextArea detail, JPanel actions) {
        JPanel panel = new JPanel(new BorderLayout(16, 14));
        panel.setBackground(PAPER);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(heading(title, 20));
        JLabel hint = new JLabel(description);
        hint.setForeground(MUTED);
        header.add(hint);
        panel.add(header, BorderLayout.NORTH);

        JPanel details = new JPanel(new BorderLayout(0, 9));
        details.setOpaque(false);
        details.add(heading(detailTitle, 14), BorderLayout.NORTH);
        details.add(new JScrollPane(detail), BorderLayout.CENTER);
        details.add(actions, BorderLayout.SOUTH);

        panel.add(listPanel, BorderLayout.WEST);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    static JPanel listPanel(String title, JList<?> list, JTextField search,
            JButton searchButton, JButton clearButton) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(27);

        JPanel searchBar = new JPanel(new BorderLayout(5, 0));
        searchBar.setOpaque(false);
        JPanel searchActions = actionRow();
        searchActions.add(searchButton);
        searchActions.add(clearButton);
        searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(searchActions, BorderLayout.EAST);
        search.getAccessibleContext().setAccessibleName("Search " + title.toLowerCase(Locale.ROOT));
        list.getAccessibleContext().setAccessibleName(title);

        JPanel top = new JPanel(new BorderLayout(0, 7));
        top.setOpaque(false);
        top.add(heading(title, 14), BorderLayout.NORTH);
        top.add(searchBar, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(320, 340));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    static JPanel actionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        return row;
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        return button;
    }

    static JLabel heading(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        label.setForeground(INK);
        return label;
    }

    static JTextArea detailArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(INK);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        return area;
    }

    static boolean matches(String value, String search) {
        return value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    static String money(long cents) {
        return String.format(Locale.US, "$%,.2f", cents / 100.0);
    }

    static String rateInput(long cents) {
        return String.format(Locale.US, "%.2f", cents / 100.0);
    }
}
