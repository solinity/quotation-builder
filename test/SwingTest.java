import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import model.Client;
import model.Offering;
import model.Quote;
import storage.DataStore;
import ui.MainFrame;

public class SwingTest {
    private final DataStore store = new DataStore();
    private final MainFrame frame = new MainFrame(store);
    private final JTabbedPane tabs = find(frame, JTabbedPane.class, tabPane -> true);
    private final Path screenshots;

    private SwingTest(Path screenshots) {
        this.screenshots = screenshots;
    }

    public static void main(String[] args) throws Exception {
        Path screenshots = args.length == 0 ? null : Paths.get(args[0]);
        SwingUtilities.invokeAndWait(() -> {
            SwingTest test = new SwingTest(screenshots);
            try {
                test.run();
            } finally {
                test.frame.dispose();
            }
        });
        System.out.println("Swing integration tests passed (4 groups).");
    }

    private void run() {
        frame.setVisible(true);
        ModelTest.check(frame.isShowing() && tabs.getTabCount() == 3, "main window and all tabs");
        testClientActions();
        testOfferingActions();
        testQuoteActions();
        testRelationshipChanges();
    }

    private void testClientActions() {
        Component clients = tab("Clients");
        JList<?> list = list(clients, "Registered clients");
        expectError(clients, "Update", "Select a client to update");
        expectError(clients, "Delete", "Select a client to delete");
        expectError(clients, "Add Client", "Name can't be empty");

        addClient(clients, "Alex Rivera", "north@example.com", "555-0101");
        addClient(clients, "Alex Rivera", "studio@example.com", "555-0102");
        addClient(clients, "Sam Ortiz", "", "");
        ModelTest.check(list.getModel().getSize() == 3 && field(clients, "Name").getText().isEmpty(),
                "clients displayed and successful add clears form");

        field(clients, "Search by name").setText(" aLeX rIvErA ");
        click(clients, "Search");
        ModelTest.check(list.getModel().getSize() == 2 && list.getModel().getElementAt(1) == store.getClient(1),
                "client search retains both matching objects");
        list.setSelectedIndex(1);
        ModelTest.check(field(clients, "Email (optional)").getText().equals("studio@example.com"),
                "selection populates the exact client");
        Client client = store.getClient(1);
        field(clients, "Name").setText("Unsaved name");
        field(clients, "Email (optional)").setText("invalid-email");
        expectError(clients, "Update", "Email must contain '@'");
        ModelTest.check(client.getName().equals("Alex Rivera"), "failed client edit leaves original name");
        field(clients, "Name").setText("Alex Rivera");
        field(clients, "Email (optional)").setText("studio@example.com");
        field(clients, "Phone (optional)").setText("555-0199");
        click(clients, "Update");
        ModelTest.check(store.getClient(1) == client && client.getPhone().equals("555-0199"), "update exact client");
        click(clients, "Clear Form");
        ModelTest.check(list.isSelectionEmpty() && field(clients, "Name").getText().isEmpty(), "clear client form");

        search(clients, "Search by name", "Nobody");
        ModelTest.check(list.getModel().getSize() == 0, "client no-match search");
        click(clients, "Show All");
        list.setSelectedIndex(2);
        click(clients, "Delete");
        ModelTest.check(list.getModel().getSize() == 2 && store.getClient(1) == client, "delete selected client");
        list.setSelectedIndex(1);
        capture("clients.png");
        System.out.println("PASS Swing client buttons, search, duplicate selection and validation");
    }

    private void testOfferingActions() {
        Component offerings = tab("Offerings");
        JList<?> list = list(offerings, "Registered offerings");
        expectError(offerings, "Update", "Select an offering to update");
        expectError(offerings, "Delete", "Select an offering to delete");
        for (String invalid : new String[] {"abc", "-1", "NaN", "Infinity", "-Infinity"}) {
            fillOffering(offerings, "Design", "Example", invalid);
            expectError(offerings, "Add Offering", invalid.equals("abc") ? "numerical unit price" : "finite number");
        }
        ModelTest.check(store.getOfferingCount() == 0, "invalid offerings were never added");
        fillOffering(offerings, "", "", "15");
        expectError(offerings, "Add Offering", "Enter an offering name");

        addOffering(offerings, "Design", "Basic layout", "75");
        addOffering(offerings, "Design", "Brand package", "120");
        addOffering(offerings, "Consultation", "Project review", "30");
        addOffering(offerings, "Free discovery", "", "0");
        ModelTest.check(list.getModel().getSize() == 4, "offerings displayed after add");
        search(offerings, "Search by name", " design ");
        ModelTest.check(list.getModel().getSize() == 2, "offering search returns duplicate matches");
        list.setSelectedIndex(1);
        ModelTest.check(field(offerings, "Description (optional)").getText().equals("Brand package"),
                "selection populates the exact offering");
        Offering offering = store.getOffering(1);
        fillOffering(offerings, "Unsaved name", "Invalid change", "-2");
        expectError(offerings, "Update", "finite number");
        ModelTest.check(offering.getName().equals("Design") && offering.getUnitPrice() == 120, "offering edit is atomic");
        fillOffering(offerings, "Design", "Brand package and review", "120");
        click(offerings, "Update");
        ModelTest.check(store.getOffering(1) == offering && offering.getDescription().contains("review"),
                "update exact offering");
        click(offerings, "Clear Form");
        ModelTest.check(list.isSelectionEmpty() && field(offerings, "Unit price").getText().isEmpty(), "clear offering form");
        search(offerings, "Search by name", "Missing");
        ModelTest.check(list.getModel().getSize() == 0, "offering no-match search");
        click(offerings, "Show All");
        list.setSelectedIndex(3);
        click(offerings, "Delete");
        ModelTest.check(store.getOfferingCount() == 3 && store.getOffering(1) == offering, "delete selected offering");
        list.setSelectedIndex(1);
        capture("offerings.png");
        System.out.println("PASS Swing offering buttons, search, duplicate selection and validation");
    }

    private void testQuoteActions() {
        Component quotes = tab("Quotations");
        JList<?> saved = list(quotes, "Saved quotations");
        JList<?> items = list(quotes, "Quotation items");
        JComboBox<?> clients = combo(quotes, "Client");
        JComboBox<?> offerings = combo(quotes, "Offering");
        JComboBox<?> statuses = combo(quotes, "Status");
        ModelTest.check(clients.getItemAt(1) == store.getClient(1) && offerings.getItemAt(1) == store.getOffering(1),
                "quote choices come from the other two tabs");
        expectError(quotes, "Open", "Select a saved quotation");
        expectError(quotes, "Delete Quote", "Select a saved quotation");
        expectError(quotes, "Remove Item", "Select a quotation item");
        expectError(quotes, "Add Item", "Select an offering");
        expectError(quotes, "Save Quote", "Select a client");
        clients.setSelectedIndex(1);
        click(quotes, "Calculate");
        label(quotes, "Subtotal: 0.00");
        expectError(quotes, "Save Quote", "Add at least one item");

        offerings.setSelectedIndex(1);
        for (String invalid : new String[] {"abc", "0", "-1", "NaN", "Infinity", "1e308"}) {
            field(quotes, "Quantity / hours").setText(invalid);
            String message = invalid.equals("abc") ? "numerical quantity" : "finite number greater than zero";
            expectError(quotes, "Add Item", invalid.equals("1e308") ? "line total is too large" : message);
        }
        ModelTest.check(items.getModel().getSize() == 0, "invalid quantities never create items");
        addItem(quotes, 1, "2");
        addItem(quotes, 2, "2");
        addItem(quotes, 0, "1");
        ModelTest.check(items.getModel().getSize() == 3, "add three items");
        items.setSelectedIndex(2);
        click(quotes, "Remove Item");
        label(quotes, "Subtotal: 300.00");

        JTextField discount = field(quotes, "Discount (%)");
        for (String invalid : new String[] {"abc", "-1", "101", "NaN", "Infinity"}) {
            discount.setText(invalid);
            expectError(quotes, "Calculate", invalid.equals("abc") ? "numerical discount" : "between 0 and 100");
        }
        label(quotes, "Total: 300.00 (0.0% discount)");
        discount.setText("100");
        click(quotes, "Calculate");
        label(quotes, "Total: 0.00 (100.0% discount)");
        discount.setText("10");
        click(quotes, "Calculate");
        label(quotes, "Total: 270.00 (10.0% discount)");

        JTextField date = field(quotes, "Date (YYYY-MM-DD)");
        date.setText("2026-02-30");
        expectError(quotes, "Save Quote", "Enter a valid date");
        date.setText("2026-11-23");
        statuses.setSelectedItem(null);
        expectError(quotes, "Save Quote", "Status can't be empty");
        statuses.setSelectedItem(Quote.Status.SENT);
        click(quotes, "Save Quote");
        Quote quote = store.getQuote(0);
        ModelTest.check(quote.getClient() == store.getClient(1) && quote.getItems().get(0).getOffering() == store.getOffering(1),
                "saved quotation uses the selected client and offering objects");
        ModelTest.check(quote.getStatus() == Quote.Status.SENT && quote.getTotal() == 270, "saved quote totals and status");

        click(quotes, "New Quote");
        ModelTest.check(items.getModel().getSize() == 0 && clients.getSelectedIndex() == -1, "new quote clears editor");
        clients.setSelectedIndex(0);
        addItem(quotes, 0, "1.5");
        click(quotes, "Save Quote");
        search(quotes, "Search by client name", "missing");
        ModelTest.check(saved.getModel().getSize() == 0, "saved quotes no-match search");
        click(quotes, "Show All");
        search(quotes, "Search by client name", " ALEX RIVERA ");
        ModelTest.check(saved.getModel().getSize() == 2, "saved quotes search returns both duplicate client names");
        saved.setSelectedIndex(0);
        click(quotes, "Open");
        ModelTest.check(items.getModel().getSize() == 2 && clients.getSelectedItem() == quote.getClient(),
                "open restores the selected quotation");
        ModelTest.check(date.getText().equals("2026-11-23") && statuses.getSelectedItem() == Quote.Status.SENT
                && discount.getText().equals("10.0"), "open restores date, status and discount");
        click(quotes, "Show All");
        capture("quotations.png");

        date.setText("2026-11-24");
        discount.setText("25");
        statuses.setSelectedItem(Quote.Status.ACCEPTED);
        items.setSelectedIndex(1);
        click(quotes, "Remove Item");
        ModelTest.check(quote.getItems().size() == 2 && quote.getStatus() == Quote.Status.SENT,
                "draft changes stay separate from saved quote");
        click(quotes, "Save Quote");
        ModelTest.check(store.getQuote(0) == quote && quote.getTotal() == 180 && quote.getItems().size() == 1
                && quote.getStatus() == Quote.Status.ACCEPTED && quote.getDate().equals(LocalDate.of(2026, 11, 24)),
                "save updates the same quotation");
        saved.setSelectedIndex(1);
        click(quotes, "Delete Quote");
        ModelTest.check(store.getQuoteCount() == 1 && store.getQuote(0) == quote, "delete selected saved quote");
        System.out.println("PASS Swing quotation buttons, totals, reopen, update, and invalid input");
    }

    private void testRelationshipChanges() {
        Quote quote = store.getQuote(0);
        Component clients = tab("Clients");
        JList<?> clientList = list(clients, "Registered clients");
        clientList.setSelectedIndex(1);
        expectError(clients, "Delete", "used by a saved quotation");
        field(clients, "Name").setText("Alex Rivera Studio");
        click(clients, "Update");
        ModelTest.check(quote.getClient() == store.getClient(1) && quote.getClient().getName().equals("Alex Rivera Studio"),
                "client edit is visible in existing quotation");

        Component offerings = tab("Offerings");
        JList<?> offeringList = list(offerings, "Registered offerings");
        offeringList.setSelectedIndex(1);
        expectError(offerings, "Delete", "used by a saved quotation");
        field(offerings, "Unit price").setText("160");
        click(offerings, "Update");
        ModelTest.check(quote.getItems().get(0).getUnitPrice() == 120 && quote.getSubtotal() == 240,
                "stored item keeps its original price after offering update");

        Component quotes = tab("Quotations");
        search(quotes, "Search by client name", "Alex Rivera Studio");
        JList<?> saved = list(quotes, "Saved quotations");
        ModelTest.check(saved.getModel().getSize() == 1 && saved.getModel().getElementAt(0) == quote,
                "quotation search sees updated client on tab return");
        addItem(quotes, 1, "1");
        label(quotes, "Subtotal: 400.00");
        ModelTest.check(quote.getItems().size() == 1, "new draft line uses new price without changing saved quote");

        // A draft alone does not prevent another tab from deleting its offering.
        addItem(quotes, 2, "1");
        tab("Offerings");
        offeringList.setSelectedIndex(2);
        click(offerings, "Delete");
        tab("Quotations");
        expectError(quotes, "Save Quote", "offering that was deleted");
        ModelTest.check(quote.getItems().size() == 1, "stale draft cannot replace saved quote");
        saved.setSelectedIndex(0);
        click(quotes, "Open");
        label(quotes, "Subtotal: 240.00");
        click(quotes, "Delete Quote");
        ModelTest.check(store.getQuoteCount() == 0, "delete opened quotation");
        tab("Offerings");
        offeringList.setSelectedIndex(1);
        click(offerings, "Delete");
        tab("Clients");
        clientList.setSelectedIndex(1);
        click(clients, "Delete");
        ModelTest.check(store.getOfferingCount() == 1 && store.getClientCount() == 1,
                "deleting quotation releases client and offering");
        System.out.println("PASS cross-tab refresh, shared identities, price history and reference protection");
    }

    private Component tab(String title) {
        int index = tabs.indexOfTab(title);
        ModelTest.check(index >= 0, "tab " + title);
        tabs.setSelectedIndex(index);
        return tabs.getSelectedComponent();
    }

    private static void addClient(Component panel, String name, String email, String phone) {
        field(panel, "Name").setText(name);
        field(panel, "Email (optional)").setText(email);
        field(panel, "Phone (optional)").setText(phone);
        click(panel, "Add Client");
    }

    private static void addOffering(Component panel, String name, String description, String price) {
        fillOffering(panel, name, description, price);
        click(panel, "Add Offering");
    }

    private static void fillOffering(Component panel, String name, String description, String price) {
        field(panel, "Name").setText(name);
        field(panel, "Description (optional)").setText(description);
        field(panel, "Unit price").setText(price);
    }

    private static void addItem(Component panel, int offeringIndex, String quantity) {
        combo(panel, "Offering").setSelectedIndex(offeringIndex);
        field(panel, "Quantity / hours").setText(quantity);
        click(panel, "Add Item");
    }

    private static void search(Component panel, String label, String query) {
        field(panel, label).setText(query);
        click(panel, "Search");
    }

    private static void click(Component panel, String button) {
        find(panel, JButton.class, found -> button.equals(found.getText())).doClick(0);
    }

    private static void expectError(Component panel, String button, String expectedMessage) {
        String[] message = {null};
        Timer closeDialog = new Timer(50, event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()) {
                    JOptionPane dialog = search(window, JOptionPane.class, pane -> true);
                    if (dialog != null) {
                        message[0] = String.valueOf(dialog.getMessage());
                        dialog.setValue(JOptionPane.OK_OPTION);
                    }
                }
            }
        });
        closeDialog.start();
        try {
            click(panel, button);
        } finally {
            closeDialog.stop();
        }
        ModelTest.check(message[0] != null && message[0].contains(expectedMessage),
                button + " should show " + expectedMessage + ", got " + message[0]);
    }

    private static JTextField field(Component panel, String name) {
        return (JTextField) label(panel, name).getLabelFor();
    }

    private static JComboBox<?> combo(Component panel, String name) {
        return (JComboBox<?>) label(panel, name).getLabelFor();
    }

    private static JLabel label(Component panel, String text) {
        JLabel label = search(panel, JLabel.class, found -> text.equals(found.getText()));
        ModelTest.check(label != null, "visible label " + text);
        return label;
    }

    private static JList<?> list(Component panel, String name) {
        return find(panel, JList.class, found -> name.equals(found.getAccessibleContext().getAccessibleName()));
    }

    private static <T extends Component> T find(Component root, Class<T> type, Predicate<T> matches) {
        T component = search(root, type, matches);
        ModelTest.check(component != null, "find " + type.getSimpleName());
        return component;
    }

    private static <T extends Component> T search(Component root, Class<T> type, Predicate<T> matches) {
        if (type.isInstance(root) && matches.test(type.cast(root))) {
            return type.cast(root);
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                T found = search(child, type, matches);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void capture(String filename) {
        if (screenshots == null) {
            return;
        }
        frame.validate();
        Container content = frame.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            content.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        try {
            Files.createDirectories(screenshots);
            ImageIO.write(image, "png", screenshots.resolve(filename).toFile());
        } catch (IOException error) {
            throw new AssertionError("Could not write screenshot " + filename, error);
        }
    }
}
