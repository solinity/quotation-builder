package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import storage.DataStore;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public MainFrame(DataStore dataStore) {
        this(dataStore, "");
    }

    public MainFrame(DataStore dataStore, String storageNotice) {
        super("Freelance Quotation Builder");

        ClientsPanel clients = new ClientsPanel(dataStore);
        OfferingsPanel offerings = new OfferingsPanel(dataStore);
        QuotesPanel quotes = new QuotesPanel(dataStore);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(SwingStyle.BODY.deriveFont(Font.BOLD));
        tabs.setBackground(SwingStyle.CANVAS);
        tabs.setForeground(SwingStyle.INK);
        tabs.addTab("Clients", clients);
        tabs.addTab("Offerings", offerings);
        tabs.addTab("Quotations", quotes);
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == quotes) {
                quotes.refreshFromStore();
            }
        });
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(SwingStyle.NAVY);
        banner.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JLabel title = new JLabel("Freelance Quotation Builder");
        title.setFont(SwingStyle.BODY.deriveFont(Font.BOLD, 23f));
        title.setForeground(SwingStyle.WHITE);
        banner.add(title, BorderLayout.CENTER);
        if (!storageNotice.isEmpty()) {
            JLabel notice = new JLabel(storageNotice);
            notice.setFont(SwingStyle.BODY.deriveFont(Font.BOLD));
            notice.setForeground(SwingStyle.WHITE);
            banner.add(notice, BorderLayout.EAST);
        }
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(SwingStyle.CANVAS);
        workspace.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
        workspace.add(tabs, BorderLayout.CENTER);
        add(banner, BorderLayout.NORTH);
        add(workspace, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (quotes.confirmDiscard()) {
                    dispose();
                }
            }
        });
        setMinimumSize(new Dimension(920, 615));
        setSize(1160, 790);
        setLocationRelativeTo(null);
    }
}
