package ui;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import storage.DataStore;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public MainFrame(DataStore dataStore) {
        super("Freelance Quotation Builder");

        ClientsPanel clients = new ClientsPanel(dataStore);
        OfferingsPanel offerings = new OfferingsPanel(dataStore);
        QuotesPanel quotes = new QuotesPanel(dataStore);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Clients", clients);
        tabs.addTab("Offerings", offerings);
        tabs.addTab("Quotations", quotes);
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == quotes) {
                quotes.refreshFromStore();
            }
        });
        add(tabs);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(865, 555));
        setSize(1030, 660);
        setLocationRelativeTo(null);
    }
}
