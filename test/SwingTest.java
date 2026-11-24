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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import model.CatalogEntry.Client;
import model.CatalogEntry.Offering;
import model.Quote;
import model.CurrencySettings.SessionCurrency;
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
        System.out.println("Swing integration tests passed (5 groups).");
    }

    private void run() {
        frame.setVisible(true);
        ModelTest.check(frame.isShowing() && tabs.getTabCount() == 4, "main window and all tabs");
        testClientActions();
        testOfferingActions();
        testQuoteActions();
        testCurrencySettings();
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

        search(clients, "Search by name", " aLeX rIvErA ");
        ModelTest.check(list.getModel().getSize() == 2 && list.getModel().getElementAt(1) == store.getClients().get(1),
                "client search retains both matching objects");
        list.setSelectedIndex(1);
        ModelTest.check(field(clients, "Email (optional)").getText().equals("studio@example.com"),
                "selection populates the exact client");
        Client client = store.getClients().get(1);
        field(clients, "Name").setText("Unsaved name");
        field(clients, "Email (optional)").setText("invalid-email");
        expectError(clients, "Update", "Email must contain '@'");
        ModelTest.check(client.getName().equals("Alex Rivera"), "failed client edit leaves original name");
        field(clients, "Name").setText("Alex Rivera");
        field(clients, "Email (optional)").setText("studio@example.com");
        field(clients, "Phone (optional)").setText("555-0199");
        click(clients, "Update");
        ModelTest.check(store.getClients().get(1) == client && client.getPhone().equals("555-0199"), "update exact client");
        click(clients, "Clear Form");
        ModelTest.check(list.isSelectionEmpty() && field(clients, "Name").getText().isEmpty(), "clear client form");

        search(clients, "Search by name", "Nobody");
        ModelTest.check(list.getModel().getSize() == 0, "client no-match search");
        click(clients, "Show All");
        clickRow(clients, list, 2, "Delete");
        ModelTest.check(list.getModel().getSize() == 2 && store.getClients().get(1) == client, "delete selected client");
        list.setSelectedIndex(1);
        capture("clients.png");
        System.out.println("PASS Swing client buttons, search, duplicate selection and validation");
    }

    private void testOfferingActions() {
        Component offerings = tab("Offerings");
        JList<?> list = list(offerings, "Registered offerings");
        JTextArea description = descriptionField(offerings);
        expectError(offerings, "Update", "Select an offering to update");
        expectError(offerings, "Delete", "Select an offering to delete");
        for (String invalid : new String[] {"abc", "-1", "NaN", "Infinity", "-Infinity"}) {
            fillOffering(offerings, "Design", "Example", invalid);
            expectError(offerings, "Add Offering", "finite number");
        }
        ModelTest.check(store.getOfferings().size() == 0, "invalid offerings were never added");
        fillOffering(offerings, "", "", "15");
        expectError(offerings, "Add Offering", "Enter an offering name");

        addOffering(offerings, "Design", "Basic layout", "75");
        String originalDescription = "Brand package\nIncludes logo guidelines";
        addOffering(offerings, "Design", originalDescription, "120");
        addOffering(offerings, "Consultation", "Project review", "30");
        addOffering(offerings, "Free discovery", "", "0");
        ModelTest.check(list.getModel().getSize() == 4, "offerings displayed after add");
        search(offerings, "Search by name", " design ");
        ModelTest.check(list.getModel().getSize() == 2, "offering search returns duplicate matches");
        list.setSelectedIndex(1);
        ModelTest.check(description.getText().equals(originalDescription),
                "selecting the exact offering restores its line breaks");
        Offering offering = store.getOfferings().get(1);
        fillOffering(offerings, "Unsaved name", "Invalid change", "-2");
        expectError(offerings, "Update", "finite number");
        ModelTest.check(offering.getName().equals("Design") && offering.getUnitPrice() == 120, "offering edit is atomic");
        String updatedDescription = "Brand package\nIncludes a review of the logo, typography, and color palette.";
        fillOffering(offerings, "Design", updatedDescription, "120");
        click(offerings, "Update");
        ModelTest.check(store.getOfferings().get(1) == offering && offering.getDescription().equals(updatedDescription),
                "updating the exact offering saves its line breaks");
        click(offerings, "Clear Form");
        ModelTest.check(list.isSelectionEmpty() && description.getText().isEmpty()
                && field(offerings, "Unit price (USD)").getText().isEmpty(), "clear offering form");
        search(offerings, "Search by name", "Missing");
        ModelTest.check(list.getModel().getSize() == 0, "offering no-match search");
        click(offerings, "Show All");
        clickRow(offerings, list, 3, "Delete");
        ModelTest.check(store.getOfferings().size() == 3 && store.getOfferings().get(1) == offering, "delete selected offering");
        list.setSelectedIndex(1);
        ModelTest.check(description.getText().equals(updatedDescription), "reselection restores the edited description");
        capture("offerings.png");
        System.out.println("PASS Swing offering buttons, search, duplicate selection and validation");
    }

    private void testQuoteActions() {
        Component quotes = tab("Quotations");
        JList<?> saved = list(quotes, "Saved quotations");
        JList<?> items = list(quotes, "Quotation items");
        JTextField clients = field(quotes, "Client");
        JTextField offerings = field(quotes, "Offering");
        JComboBox<?> statuses = combo(quotes, "Status");
        expectError(quotes, "Open", "Select a saved quotation");
        expectError(quotes, "Edit", "Select a saved quotation");
        expectError(quotes, "Export PDF", "Select a saved quotation");
        expectError(quotes, "Delete Quote", "Select a saved quotation");
        expectError(quotes, "Remove Item", "Select a quotation item");
        expectError(quotes, "Add Item", "Select an offering");
        expectError(quotes, "Save Quote", "Select a client");
        clients.setText("Missing");
        label(suggestions(clients), "No matching names");
        expectError(quotes, "Save Quote", "Select a client");
        clients.setText(" rIv ");
        ModelTest.check(matches(clients).getModel().getSize() == 2, "client search matches partial duplicate names");
        choose(clients, " rIv ", store.getClients().get(1));
        ModelTest.check(clients.getText().contains("studio@example.com"), "selection displays the exact client");
        click(quotes, "Calculate");
        label(quotes, "Subtotal: USD 0.00");
        expectError(quotes, "Save Quote", "Add at least one item");

        offerings.setText("eSIG");
        ModelTest.check(matches(offerings).getModel().getSize() == 2, "offering search matches partial duplicate names");
        expectError(quotes, "Add Item", "Select an offering");
        choose(offerings, "eSIG", store.getOfferings().get(1));
        ModelTest.check(offerings.getText().contains("Brand package Includes a review"),
                "selection displays the exact offering with a single-line description preview");
        for (String invalid : new String[] {"abc", "0", "-1", "NaN", "Infinity", "1e308"}) {
            field(quotes, "Quantity / hours").setText(invalid);
            String message = invalid.equals("abc") ? "numerical quantity" : "finite number greater than zero";
            expectError(quotes, "Add Item", invalid.equals("1e308") ? "line total is too large" : message);
        }
        ModelTest.check(items.getModel().getSize() == 0, "invalid quantities never create items");
        JTextField itemDiscount = field(quotes, "Item discount (%)");
        field(quotes, "Quantity / hours").setText("2");
        for (String invalid : new String[] {"abc", "-1", "101", "NaN", "Infinity"}) {
            itemDiscount.setText(invalid);
            expectError(quotes, "Add Item", invalid.equals("abc") ? "numerical item discount" : "between 0 and 100");
        }
        ModelTest.check(items.getModel().getSize() == 0, "invalid item discounts never create items");
        itemDiscount.setText("");
        addItem(quotes, 1, "2");
        itemDiscount.setText("12.5");
        addItem(quotes, 2, "2");
        ModelTest.check(itemDiscount.getText().equals("0"), "next item defaults to zero discount");
        addItem(quotes, 0, "1");
        ModelTest.check(items.getModel().getSize() == 3, "add three items");
        items.setSelectedIndex(2);
        click(quotes, "Remove Item");
        label(quotes, "Subtotal: USD 292.50");

        JTextField discount = field(quotes, "Discount (%)");
        for (String invalid : new String[] {"abc", "-1", "101", "NaN", "Infinity"}) {
            discount.setText(invalid);
            expectError(quotes, "Calculate", invalid.equals("abc") ? "numerical discount" : "between 0 and 100");
        }
        label(quotes, "Total: USD 292.50 (0.0% discount)");
        discount.setText("100");
        click(quotes, "Calculate");
        label(quotes, "Total: USD 0.00 (100.0% discount)");
        discount.setText("10");
        click(quotes, "Calculate");
        label(quotes, "Total: USD 263.25 (10.0% discount)");

        JTextField date = field(quotes, "Date (YYYY-MM-DD)");
        date.setText("2026-02-30");
        expectError(quotes, "Save Quote", "Enter a valid date");
        date.setText("2026-11-23");
        statuses.setSelectedItem(null);
        expectError(quotes, "Save Quote", "Status can't be empty");
        statuses.setSelectedItem(Quote.Status.SENT);
        click(quotes, "Save Quote");
        Quote quote = store.getQuotes().get(0);
        ModelTest.check(quote.getClient() == store.getClients().get(1) && quote.getItems().get(0).getOffering() == store.getOfferings().get(1),
                "saved quotation uses the selected client and offering objects");
        ModelTest.check(quote.getStatus() == Quote.Status.SENT && quote.getTotal() == 263.25
                && quote.getItems().get(1).getDiscountPercent() == 12.5, "saved quote totals, status and item discount");

        click(quotes, "New Quote");
        ModelTest.check(items.getModel().getSize() == 0 && clients.getText().isEmpty() && offerings.getText().isEmpty(),
                "new quote clears editor");
        choose(clients, "Alex", store.getClients().get(0));
        addItem(quotes, 0, "1.5");
        click(quotes, "Save Quote");
        search(quotes, "Search by client name", "missing");
        ModelTest.check(saved.getModel().getSize() == 0, "saved quotes no-match search");
        click(quotes, "Show All");
        search(quotes, "Search by client name", " ALEX RIVERA ");
        ModelTest.check(saved.getModel().getSize() == 2, "saved quotes search returns both duplicate client names");
        saved.setSelectedIndex(0);
        date.setText("2026-12-01");
        String preview = openQuotationPreview(quotes, "quotation-details.png");
        for (String detail : new String[] {"Quotation #1", "2026-11-23", "SENT", "Alex Rivera",
                "studio@example.com", "555-0199", "Design", "Includes a review", "Consultation", "USD 120.00",
                "12.5%", "USD 52.50", "Subtotal", "USD 292.50", "Quote discount (10%)", "USD 29.25", "USD 263.25"}) {
            ModelTest.check(preview.contains(detail), "quotation preview shows " + detail);
        }
        ModelTest.check(date.getText().equals("2026-12-01") && items.getModel().getSize() == 1,
                "preview keeps the open editor and unsaved edits");
        click(quotes, "Edit");
        ModelTest.check(items.getModel().getSize() == 2 && clients.getText().contains(quote.getClient().getEmail()),
                "edit restores the selected quotation");
        ModelTest.check(date.getText().equals("2026-11-23") && statuses.getSelectedItem() == Quote.Status.SENT
                && discount.getText().equals("10.0"), "edit restores date, status and discount");
        label(quotes, "Total: USD 263.25 (10.0% discount)");
        click(quotes, "Show All");
        capture("quotations.png");

        clients.setText("Alex");
        expectError(quotes, "Save Quote", "Select a client");
        click(quotes, "Edit");
        date.setText("2026-11-24");
        discount.setText("25");
        statuses.setSelectedItem(Quote.Status.ACCEPTED);
        items.setSelectedIndex(1);
        click(quotes, "Remove Item");
        ModelTest.check(quote.getItems().size() == 2 && quote.getStatus() == Quote.Status.SENT,
                "draft changes stay separate from saved quote");
        click(quotes, "Save Quote");
        ModelTest.check(store.getQuotes().get(0) == quote && quote.getTotal() == 180 && quote.getItems().size() == 1
                && quote.getStatus() == Quote.Status.ACCEPTED && quote.getDate().equals(LocalDate.of(2026, 11, 24)),
                "save updates the same quotation");
        clickRow(quotes, saved, 1, "Delete Quote");
        ModelTest.check(store.getQuotes().size() == 1 && store.getQuotes().get(0) == quote, "delete selected saved quote");
        System.out.println("PASS Swing quotation buttons, totals, reopen, update, and invalid input");
    }

    private void testCurrencySettings() {
        Component quotes = tab("Quotations");
        JTextField selectedOffering = field(quotes, "Offering");
        choose(selectedOffering, "Design", store.getOfferings().get(1));
        Component settings = tab("Settings");
        JComboBox<?> currency = combo(settings, "Currency");
        JTextField rate = field(settings, "Guaran\u00edes per 1 USD");
        ModelTest.check(currency.getSelectedItem() == SessionCurrency.USD, "USD is the default setting");
        rate.setText("zero");
        expectError(settings, "Apply", "Enter the exchange rate as a number");
        for (String invalid : new String[] {"0", "-1", "1e400"}) {
            rate.setText(invalid);
            expectError(settings, "Apply", "positive exchange rate");
        }
        rate.setText("7500");
        currency.setSelectedItem(SessionCurrency.PYG);
        capture("settings.png");

        Component offerings = tab("Offerings");
        JTextField guaraniesPrice = field(offerings, "Unit price (PYG)");
        JList<?> offeringList = list(offerings, "Registered offerings");
        offeringList.setSelectedIndex(1);
        ModelTest.check(guaraniesPrice.getText().equals("900000")
                && rowText(offeringList, 1).contains("PYG 900.000"), "catalog and form use the entered exchange rate");
        tab("Quotations");
        label(quotes, "Subtotal: PYG 1.800.000");
        label(quotes, "Total: PYG 1.350.000 (25.0% discount)");
        ModelTest.check(selectedOffering.getText().contains("PYG 900.000")
                && rowText(list(quotes, "Saved quotations"), 0).contains("PYG 1.350.000")
                && rowText(list(quotes, "Quotation items"), 0).contains("PYG 900.000 = PYG 1.800.000"),
                "selected offering, saved quote and line prices convert");
        ModelTest.check(store.getQuotes().get(0).getTotal() == 180 && store.getOfferings().get(1).getUnitPrice() == 120,
                "changing currency retains all numeric amounts");
        capture("quotations-pyg.png");

        tab("Offerings");
        guaraniesPrice.setText("937500");
        tab("Settings");
        rate.setText("8000");
        click(settings, "Apply");
        tab("Quotations");
        label(quotes, "Total: PYG 1.440.000 (25.0% discount)");
        tab("Offerings");
        ModelTest.check(guaraniesPrice.getText().equals("1000000") && store.getOfferings().get(1).getUnitPrice() == 120,
                "rate changes convert pending price edits without saving them");
        click(offerings, "Update");
        ModelTest.check(store.getOfferings().get(1).getUnitPrice() == 125 && store.getQuotes().get(0).getTotal() == 180,
                "updating in PYG stores the equivalent USD and preserves quoted prices");
        tab("Quotations");
        list(quotes, "Saved quotations").setSelectedIndex(0);
        String preview = openQuotationPreview(quotes, "quotation-details-pyg.png");
        ModelTest.check(preview.contains("PYG 960.000") && preview.contains("PYG 1.440.000"),
                "preview converts the original quoted price and total using the current exchange rate");

        tab("Settings");
        currency.setSelectedItem(SessionCurrency.USD);
        tab("Quotations");
        label(quotes, "Total: USD 180.00 (25.0% discount)");
        System.out.println("PASS Settings rates convert catalog prices, quotation totals and line items");
    }

    private void testRelationshipChanges() {
        Quote quote = store.getQuotes().get(0);
        Component clients = tab("Clients");
        JList<?> clientList = list(clients, "Registered clients");
        clientList.setSelectedIndex(1);
        expectError(clients, "Delete", "used by a saved quotation");
        field(clients, "Name").setText("Alex Rivera Studio");
        click(clients, "Update");
        ModelTest.check(quote.getClient() == store.getClients().get(1) && quote.getClient().getName().equals("Alex Rivera Studio"),
                "client edit is visible in existing quotation");

        Component offerings = tab("Offerings");
        JList<?> offeringList = list(offerings, "Registered offerings");
        offeringList.setSelectedIndex(1);
        expectError(offerings, "Delete", "used by a saved quotation");
        field(offerings, "Unit price (USD)").setText("160");
        click(offerings, "Update");
        ModelTest.check(quote.getItems().get(0).getUnitPrice() == 120 && quote.getSubtotal() == 240,
                "stored item keeps its original price after offering update");

        Component quotes = tab("Quotations");
        search(quotes, "Search by client name", "Alex Rivera Studio");
        JList<?> saved = list(quotes, "Saved quotations");
        ModelTest.check(saved.getModel().getSize() == 1 && saved.getModel().getElementAt(0) == quote,
                "quotation search sees updated client on tab return");
        ModelTest.check(field(quotes, "Client").getText().contains("Alex Rivera Studio"),
                "selected client label reflects cross-tab changes");
        addItem(quotes, 1, "1");
        label(quotes, "Subtotal: USD 400.00");
        ModelTest.check(quote.getItems().size() == 1, "new draft line uses new price without changing saved quote");

        // A draft alone does not prevent another tab from deleting its offering.
        addItem(quotes, 2, "1");
        tab("Offerings");
        clickRow(offerings, offeringList, 2, "Delete");
        tab("Quotations");
        ModelTest.check(field(quotes, "Offering").getText().isEmpty(), "deleted offering is no longer selected");
        expectError(quotes, "Save Quote", "offering that was deleted");
        ModelTest.check(quote.getItems().size() == 1, "stale draft cannot replace saved quote");
        clickRow(quotes, saved, 0, "Edit");
        label(quotes, "Subtotal: USD 240.00");
        click(quotes, "Delete Quote");
        ModelTest.check(store.getQuotes().size() == 0, "delete opened quotation");
        tab("Offerings");
        clickRow(offerings, offeringList, 1, "Delete");
        tab("Clients");
        clickRow(clients, clientList, 1, "Delete");
        ModelTest.check(store.getOfferings().size() == 1 && store.getClients().size() == 1,
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
        descriptionField(panel).setText(description);
        field(panel, "Unit price (USD)").setText(price);
    }

    private static JTextArea descriptionField(Component panel) {
        return (JTextArea) label(panel, "Description (optional)").getLabelFor();
    }

    private void addItem(Component panel, int offeringIndex, String quantity) {
        Offering offering = store.getOfferings().get(offeringIndex);
        choose(field(panel, "Offering"), offering.getName(), offering);
        field(panel, "Quantity / hours").setText(quantity);
        click(panel, "Add Item");
    }

    private static void choose(JTextField field, String query, Object record) {
        field.setText(query);
        JList<?> results = matches(field);
        results.setSelectedValue(record, true);
        ModelTest.check(results.getSelectedValue() == record, "search contains the intended record");
        field.postActionEvent();
    }

    private static JList<?> matches(JTextField field) {
        return find(suggestions(field), JList.class, Component::isShowing);
    }

    private static JPopupMenu suggestions(JTextField field) {
        return find(SwingUtilities.getRootPane(field), JPopupMenu.class,
                popup -> popup.isShowing() && popup.getInvoker() == field);
    }

    private static void search(Component panel, String label, String query) {
        field(panel, label).setText(query);
        click(panel, "Search");
    }

    private static void click(Component panel, String button) {
        find(panel, JButton.class, found -> button.equals(found.getText())).doClick(0);
    }

    private static void clickRow(Component panel, JList<?> list, int index, String button) {
        list.setSelectedIndex(index);
        click(panel, button);
    }

    private static void expectError(Component panel, String button, String expectedMessage) {
        String message = inspectDialog(panel, button,
                dialog -> String.valueOf(find(dialog, JOptionPane.class, pane -> true).getMessage()));
        ModelTest.check(message.contains(expectedMessage), button + " should show " + expectedMessage + ", got " + message);
    }

    private String openQuotationPreview(Component quotes, String screenshot) {
        return inspectDialog(quotes, "Open", dialog -> {
            JEditorPane document = find(dialog, JEditorPane.class, pane -> true);
            ModelTest.check(!document.isEditable(), "Open shows a read-only quote window");
            String text = document.getText().replaceAll("\\s+", " ");
            capture(dialog.getContentPane(), screenshot);
            click(dialog, "Close");
            return text;
        });
    }

    private static <T> T inspectDialog(Component panel, String button, Function<JDialog, T> inspect) {
        List<T> result = new ArrayList<>();
        Timer timer = new Timer(50, event -> {
            for (Window window : SwingUtilities.getWindowAncestor(panel).getOwnedWindows()) {
                if (window instanceof JDialog && window.isShowing()) {
                    try {
                        result.add(inspect.apply((JDialog) window));
                    } finally {
                        window.dispose();
                    }
                    return;
                }
            }
        });
        timer.start();
        try {
            click(panel, button);
        } finally {
            timer.stop();
        }
        ModelTest.check(!result.isEmpty(), button + " opens the expected dialog");
        return result.get(0);
    }

    private static JTextField field(Component panel, String name) {
        return (JTextField) label(panel, name).getLabelFor();
    }

    private static JComboBox<?> combo(Component panel, String name) {
        return (JComboBox<?>) label(panel, name).getLabelFor();
    }

    private static JLabel label(Component panel, String text) {
        return find(panel, JLabel.class, found -> text.equals(found.getText()));
    }

    private static JList<?> list(Component panel, String name) {
        return find(panel, JList.class, found -> name.equals(found.getAccessibleContext().getAccessibleName()));
    }

    private static <T> String rowText(JList<T> list, int row) {
        T value = list.getModel().getElementAt(row);
        return ((JLabel) list.getCellRenderer().getListCellRendererComponent(list, value, row, false, false)).getText();
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
        frame.validate();
        capture(frame.getContentPane(), filename);
    }

    private void capture(Container content, String filename) {
        if (screenshots == null) {
            return;
        }
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
