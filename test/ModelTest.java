import export.QuotePdf;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import model.CatalogEntry.Client;
import model.CurrencySettings;
import model.CatalogEntry.Offering;
import model.Quote;
import model.Quote.Item;
import model.CurrencySettings.SessionCurrency;
import storage.DataStore;
import storage.SessionDatabase;

public class ModelTest {
    public static void main(String[] args) throws IOException, SQLException {
        testClients();
        testOfferings();
        testCalculations();
        testSavedQuotes();
        testSessionDatabase();
        testQuotePdf();
        System.out.println("Model, data store, and export tests passed (6 groups).");
    }

    private static void testClients() {
        DataStore store = new DataStore();
        Client first = new Client(" Alex ", "first@example.com", " 123 ");
        Client second = new Client("Alex", null, null);
        store.addClient(first);
        store.addClient(second);
        check(store.getClients().size() == 2 && store.getClients().get(1) == second, "client add and display");
        check(store.searchClients(" aLeX ", false).size() == 2, "case-insensitive client search with duplicates");
        check(store.searchClients("Al", false).isEmpty() && store.searchClients("", false).size() == 2,
                "client search and show all");
        check(first.getPhone().equals("123") && second.getEmail().isEmpty(), "optional client fields and trimming");
        rejects(() -> new Client(" ", "", ""), "blank client name");
        rejects(() -> first.update("Changed", "missing-at-sign", "456"), "invalid email");
        check(first.getEmail().equals("first@example.com"), "email retained after validation failure");
        check(store.removeClient(second) && store.getClients().get(0) == first, "delete exact duplicate client");
        check(!store.removeClient(second), "missing client removal");
        System.out.println("PASS client records, validation and duplicate selection");
    }

    private static void testOfferings() {
        DataStore store = new DataStore();
        Offering first = new Offering("Editing", "Copy edit", 45.5);
        Offering second = new Offering("Editing", null, 0);
        store.addOffering(first);
        store.addOffering(second);
        check(store.getOfferings().size() == 2 && store.getOfferings().get(1) == second, "offering add and display");
        check(store.searchOfferings(" editing ", false).size() == 2 && store.searchOfferings("", false).size() == 2,
                "offering search and show all");
        check(second.getDescription().isEmpty() && second.getUnitPrice() == 0, "optional description and zero price");
        rejects(() -> new Offering("", "", 0), "blank offering name");
        for (double invalid : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects(() -> first.update("Changed", "Other description", invalid), "invalid offering price");
        }
        check(first.getUnitPrice() == 45.5, "offering retains valid price");
        check(store.removeOffering(second) && store.getOfferings().get(0) == first, "delete exact duplicate offering");
        System.out.println("PASS offering records, validation and duplicate selection");
    }

    private static void testCalculations() {
        Client client = new Client("Alex", "", "");
        Offering first = new Offering("Design", "", 80);
        Offering second = new Offering("Review", "", 20);
        Quote quote = new Quote(client);
        check(quote.getStatus() == Quote.Status.DRAFT && quote.getSubtotal() == 0 && quote.getTotal() == 0,
                "new quote defaults and empty totals");

        Item item = new Item(first, 1.5, first.getUnitPrice());
        check(item.getOffering() == first && item.getLineTotal() == 120, "fractional quantity and offering identity");
        quote.addItem(item);
        check(quote.getSubtotal() == 120 && quote.getTotal() == 120, "single item and zero discount");
        Item discounted = new Item(second, 2, 20, 25);
        check(discounted.getDiscountPercent() == 25 && discounted.getLineTotal() == 30,
                "item discount applies to the line total");
        check(new Item(second, 2, 20, 100).getLineTotal() == 0, "100 percent item discount");
        quote.addItem(discounted);
        quote.update(client, quote.getDate(), Quote.Status.DRAFT, 25);
        check(quote.getSubtotal() == 150 && quote.getTotal() == 112.5,
                "quote discount applies after item discounts");
        quote.update(client, quote.getDate(), Quote.Status.DRAFT, 100);
        check(quote.getTotal() == 0, "100 percent discount");
        quote.removeItem(item);
        check(quote.getSubtotal() == 30, "remove one of multiple items");
        quote.getItems().clear();
        check(quote.getItems().size() == 1, "external list changes do not affect quote");

        rejects(() -> new Quote(null), "missing client");
        rejects(() -> quote.update(client, null, Quote.Status.DRAFT, 0), "missing date");
        rejects(() -> quote.update(client, quote.getDate(), null, 0), "missing status");
        rejects(() -> quote.addItem(null), "missing item");
        rejects(() -> new Item(null, 1, 1), "missing offering");
        rejects(() -> new Item(first, 2, Double.MAX_VALUE), "line total overflow");
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> new Item(first, invalid, 10), "invalid quantity");
        }
        for (double invalid : new double[] {-1, Double.NaN, Double.NEGATIVE_INFINITY}) {
            rejects(() -> new Item(first, 1, invalid), "invalid item price");
        }
        for (double invalid : new double[] {-1, 100.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> quote.update(client, quote.getDate(), Quote.Status.DRAFT, invalid), "invalid discount");
            rejects(() -> new Item(first, 1, 10, invalid), "invalid item discount");
        }
        check(quote.getDiscountPercent() == 100 && new Item(first, 1, 0).getLineTotal() == 0,
                "invalid discount has no effect and zero-priced item is valid");

        Quote large = new Quote(client);
        Item largeItem = new Item(first, 1, Double.MAX_VALUE);
        large.addItem(largeItem);
        rejects(() -> large.addItem(largeItem), "subtotal overflow");
        check(large.getItems().size() == 1 && large.getSubtotal() == Double.MAX_VALUE, "subtotal remains valid");
        System.out.println("PASS quote calculations, boundaries and invalid input");
    }

    private static void testSavedQuotes() {
        DataStore store = new DataStore();
        Client client = new Client("Alex", "", "");
        Client otherClient = new Client("Alex", "", "");
        Offering offering = new Offering("Design", "", 100);
        Offering otherOffering = new Offering("Design", "", 120);
        store.addClient(client);
        store.addClient(otherClient);
        store.addOffering(offering);
        store.addOffering(otherOffering);

        Quote quote = new Quote(client);
        rejects(() -> store.addQuote(quote), "empty quote cannot be saved");
        quote.addItem(new Item(offering, 3, offering.getUnitPrice()));
        store.addQuote(quote);
        Quote duplicateName = new Quote(otherClient);
        duplicateName.addItem(new Item(otherOffering, 1, otherOffering.getUnitPrice()));
        store.addQuote(duplicateName);
        check(store.getQuotes().size() == 2 && store.searchQuotes("alex").size() == 2, "save and find duplicate names");
        check(store.getQuotes().get(0) == quote && quote.getClient() == client, "saved quote and client identity");
        rejects(() -> store.removeClient(client), "protect referenced client");
        rejects(() -> store.removeOffering(offering), "protect referenced offering");
        rejects(() -> store.addQuote(quote), "cannot save same object twice");

        client.update("Alex Rivera", "", "");
        offering.update("Design", "", 180);
        check(store.searchQuotes("aLeX rIvErA").get(0) == quote, "client update visible on original quote");
        check(quote.getItems().get(0).getOffering() == offering && quote.getSubtotal() == 300,
                "existing items keep agreed price");
        LocalDate date = LocalDate.of(2026, 11, 20);
        Quote changes = new Quote(new Client("Unregistered", "", ""), date, Quote.Status.SENT, 10, quote.getItems());
        rejects(() -> store.updateQuote(quote, changes), "cannot update to unregistered client");
        check(quote.getStatus() == Quote.Status.DRAFT && quote.getClient() == client, "invalid update is atomic");
        changes.update(otherClient, date, Quote.Status.SENT, 10);
        Item unavailable = new Item(new Offering("Unregistered", "", 1), 1, 1);
        changes.addItem(unavailable);
        rejects(() -> store.updateQuote(quote, changes), "cannot save an unregistered offering");
        changes.removeItem(unavailable);
        store.updateQuote(quote, changes);
        check(store.getQuotes().get(0) == quote && quote.getClient() == otherClient, "quote update preserves identity");
        check(quote.getStatus() == Quote.Status.SENT && quote.getTotal() == 270
                && quote.getDate().equals(LocalDate.of(2026, 11, 20)), "updated fields and recalculated total");
        check(store.removeClient(client), "old client released after quote update");
        check(store.removeQuote(quote) && !store.removeQuote(quote) && store.removeOffering(offering),
                "delete exact quote and release offering");
        check(store.getQuotes().get(0) == duplicateName, "other saved quotation survives deletion");
        System.out.println("PASS shared relationships, saved quote updates and protected deletions");
    }

    private static void testSessionDatabase() throws IOException, SQLException {
        Path directory = Files.createTempDirectory("quotation-session-test-");
        Path file = directory.resolve("Studio's session #1.sqlite");
        Path legacy = directory.resolve("previous.qbs");
        Path unrelated = directory.resolve("unrelated.sqlite");
        try {
            DataStore store = new DataStore();
            SessionDatabase.save(file, store);
            DataStore empty = SessionDatabase.load(file);
            check(empty.getQuotes().size() == 0
                    && empty.getCurrencySettings().getDisplayCurrency() == SessionCurrency.USD, "empty session defaults");
            rejects(() -> new CurrencySettings(SessionCurrency.USD, SessionCurrency.PYG, null), "switching without a rate");

            Client client = new Client("Alex", "studio@example.com", "123");
            store.addClient(new Client("Alex", "other@example.com", ""));
            store.addClient(client);
            String description = "Dise\u00f1o y revisi\u00f3n\n" + "Notes ".repeat(15_000) + "End";
            Offering offering = new Offering("Design", description, 120);
            store.addOffering(new Offering("Design", "", 75));
            store.addOffering(offering);

            Quote quote = new Quote(client, LocalDate.of(2026, 11, 23), Quote.Status.ACCEPTED, 10);
            quote.addItem(new Item(offering, 2, 100, 25));
            store.addQuote(quote);
            SessionDatabase.save(file, store);

            executeSql(file, "DROP TABLE session_settings", "PRAGMA user_version = 1");
            DataStore previous = SessionDatabase.load(file);
            check(previous.getCurrencySettings().getBaseCurrency() == SessionCurrency.USD
                    && previous.getQuotes().get(0).getTotal() == 135,
                    "older SQLite databases open in USD");

            BigDecimal rate = new BigDecimal("7500.25");
            store.setCurrencySettings(new CurrencySettings(SessionCurrency.USD, SessionCurrency.PYG, rate));
            SessionDatabase.save(file, store);
            DataStore loaded = SessionDatabase.load(file);
            CurrencySettings settings = loaded.getCurrencySettings();
            check(settings.getDisplayCurrency() == SessionCurrency.PYG && settings.getBaseCurrency() == SessionCurrency.USD
                    && settings.getGuaraniesPerDollar().equals(rate)
                    && settings.displayAmount(120).compareTo(new BigDecimal("900030")) == 0,
                    "saving upgrades the database and persists the exact exchange rate");
            Quote restored = loaded.getQuotes().get(0);
            check(loaded.getClients().size() == 2 && restored.getClient() == loaded.getClients().get(1)
                    && restored.getClient().getEmail().equals("studio@example.com")
                    && restored.getClient().getPhone().equals("123"), "session retains exact client and contact details");
            Item restoredItem = restored.getItems().get(0);
            check(loaded.getOfferings().size() == 2 && restoredItem.getOffering() == loaded.getOfferings().get(1)
                    && restoredItem.getOffering().getDescription().equals(description)
                    && restoredItem.getOffering().getUnitPrice() == 120, "session retains exact offering and multiline text");
            check(restored.getDate().equals(quote.getDate()) && restored.getStatus() == Quote.Status.ACCEPTED
                    && restored.getDiscountPercent() == 10 && restoredItem.getDiscountPercent() == 25
                    && restoredItem.getQuantity() == 2 && restoredItem.getUnitPrice() == 100
                    && restored.getTotal() == 135, "session retains quotation state and agreed pricing");

            store.getClients().get(0).update("Unsaved change", "other@example.com", "");
            store.setCurrencySettings(new CurrencySettings(SessionCurrency.USD, SessionCurrency.USD, new BigDecimal("8000")));
            quote.update(new Client("Unregistered", "", ""), quote.getDate(), quote.getStatus(), quote.getDiscountPercent());
            rejectsFileOperation(() -> SessionDatabase.save(file, store), "save with invalid reference");
            DataStore saved = SessionDatabase.load(file);
            check(saved.getCurrencySettings().getDisplayCurrency() == SessionCurrency.PYG
                    && saved.getCurrencySettings().getGuaraniesPerDollar().equals(rate)
                    && saved.getClients().get(0).getName().equals("Alex") && saved.getQuotes().get(0).getTotal() == 135,
                    "failed transaction preserves the previous database");
            store.replaceWith(loaded);
            check(store.getCurrencySettings() == settings && store.getQuotes().get(0) == restored
                    && store.isOfferingReferenced(store.getOfferings().get(1)), "installed session restores currency and links");

            executeSql(file, "DROP TABLE session_settings",
                    "CREATE TABLE session_settings (id INTEGER PRIMARY KEY, currency TEXT NOT NULL) STRICT",
                    "INSERT INTO session_settings VALUES (1, 'PYG')", "PRAGMA user_version = 2");
            DataStore previousGuaranies = SessionDatabase.load(file);
            CurrencySettings previousSettings = previousGuaranies.getCurrencySettings();
            check(previousSettings.getBaseCurrency() == SessionCurrency.PYG
                    && previousSettings.getDisplayCurrency() == SessionCurrency.PYG
                    && previousSettings.getGuaraniesPerDollar() == null, "older PYG sessions keep their original amounts");
            previousGuaranies.setCurrencySettings(new CurrencySettings(SessionCurrency.PYG, SessionCurrency.USD,
                    new BigDecimal("6000")));
            SessionDatabase.save(file, previousGuaranies);
            CurrencySettings reversed = SessionDatabase.load(file).getCurrencySettings();
            check(reversed.displayAmount(120).compareTo(new BigDecimal("0.02")) == 0
                    && reversed.toBasePrice(new BigDecimal("0.02")) == 120,
                    "upgrading an older PYG session converts in both directions");

            executeSql(file, "PRAGMA user_version = 99");
            rejectsFileOperation(() -> SessionDatabase.load(file), "unsupported database version");

            executeSql(unrelated, "CREATE TABLE notes (note TEXT)");
            byte[] unrelatedBytes = Files.readAllBytes(unrelated);
            rejectsFileOperation(() -> SessionDatabase.load(unrelated), "unrelated SQLite file");
            rejectsFileOperation(() -> SessionDatabase.save(unrelated, store), "overwriting an unrelated SQLite file");
            check(Arrays.equals(unrelatedBytes, Files.readAllBytes(unrelated)), "unrelated database stays intact");

            // Exported by the previous version's .qbs writer.
            byte[] legacyBytes = Base64.getDecoder().decode(
                    "UUJTRgAAAAEAAAABAAAABEFsZXgAAAAQdGVzdEBleGFtcGxlLmNvbQAAAAAAAAABAAAABkRlc2lnbgAAABFMaW5l"
                            + "IG9uZQpMaW5lIHR3b0BeAAAAAAAAAAAAAQAAAAAAAAAAAABRLAAAAARTRU5UQCQAAAAAAAAAAAABAAAAAEAAAAAAAAAAQFkAAAAAAABAOQAAAAAAAA==");
            Files.write(legacy, legacyBytes);
            DataStore imported = SessionDatabase.load(legacy);
            Files.delete(file);
            SessionDatabase.save(file, imported);
            DataStore converted = SessionDatabase.load(file);
            check(converted.getCurrencySettings().getBaseCurrency() == SessionCurrency.USD
                    && converted.getQuotes().get(0).getClient() == converted.getClients().get(0)
                    && converted.getQuotes().get(0).getTotal() == 135
                    && converted.getOfferings().get(0).getDescription().equals("Line one\nLine two"),
                    "a legacy session imports into SQLite with relationships and pricing");
            check(Arrays.equals(legacyBytes, Files.readAllBytes(legacy)), "migration preserves the legacy file");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(legacy);
            Files.deleteIfExists(unrelated);
            Files.deleteIfExists(directory);
        }
        System.out.println("PASS SQLite session round trip, rollback, and legacy import");
    }

    private static void testQuotePdf() throws IOException {
        Client client = new Client("Luc\u00eda Ben\u00edtez", "lucia@example.com", "555-0124");
        Offering offering = new Offering("Consultor\u00eda (Asunci\u00f3n)",
                "Detalle de revisi\u00f3n\n".repeat(65) + "Fin de la descripci\u00f3n.", 200);
        Quote quote = new Quote(client, LocalDate.now(), Quote.Status.DRAFT, 10);
        quote.addItem(new Item(offering, 2, 100, 25));
        CurrencySettings currency = new CurrencySettings(SessionCurrency.USD, SessionCurrency.PYG, new BigDecimal("7500"));
        Path file = Files.createTempFile("quotation-export-test-", ".pdf");
        try {
            QuotePdf.write(file, quote, 12, currency);
            String pdf = Files.readString(file, StandardCharsets.ISO_8859_1);
            check(pdf.startsWith("%PDF-1.4") && pdf.contains("QUOTATION #12") && pdf.contains(client.getName()),
                    "export writes a quotation PDF with the customer details");
            check(pdf.contains("Page 2 / ") && pdf.contains("Fin de la descripci\u00f3n."),
                    "export keeps long descriptions across pages");
            check(pdf.contains("PYG 750.000") && pdf.contains("PYG 1.125.000") && pdf.contains("PYG 1.012.500"),
                    "export converts saved prices, discounts, and totals to the selected currency");
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("PASS quotation PDF export, currency, and pagination");
    }

    private static void executeSql(Path file, String... statements) throws SQLException {
        try (Connection database = DriverManager.getConnection("jdbc:sqlite:" + file.toUri().toASCIIString());
                Statement sql = database.createStatement()) {
            for (String statement : statements) {
                sql.execute(statement);
            }
        }
    }

    private interface Operation {
        void run() throws IOException;
    }

    private static void rejectsFileOperation(Operation operation, String description) {
        rejects(IOException.class, operation, description);
    }

    private static void rejects(Runnable action, String description) {
        rejects(IllegalArgumentException.class, action::run, description);
    }

    private static void rejects(Class<? extends Exception> type, Operation operation, String description) {
        try {
            operation.run();
        } catch (Exception error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected error for " + description, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " for " + description);
    }

    static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
