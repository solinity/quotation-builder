import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import storage.DataStore;

public class ModelTest {
    public static void main(String[] args) throws IOException {
        testClients();
        testOfferings();
        testCalculations();
        testSavedQuotes();
        testPersistence();
        System.out.println("Model and data store tests passed (5 groups).");
    }

    private static void testClients() {
        DataStore store = new DataStore();
        Client first = new Client(" Alex ", "first@example.com", " 123 ");
        Client second = new Client("Alex", null, null);
        store.addClient(first);
        store.addClient(second);
        check(store.getClientCount() == 2 && store.getClient(1) == second, "client add and display");
        check(store.searchClients(" aLeX ").size() == 2, "case-insensitive client search with duplicates");
        check(store.searchClients("Al").size() == 2 && store.searchClients("").size() == 2, "partial client search and show all");
        check(first.getPhone().equals("123") && second.getEmail().isEmpty(), "optional client fields and trimming");
        rejects(() -> new Client(" ", "", ""), "blank client name");
        rejects(() -> first.setEmail("missing-at-sign"), "invalid email");
        check(first.getEmail().equals("first@example.com"), "email retained after validation failure");
        check(store.removeClient(second) && store.getClient(0) == first, "delete exact duplicate client");
        check(!store.removeClient(second), "missing client removal");
        System.out.println("PASS client records, validation and duplicate selection");
    }

    private static void testOfferings() {
        DataStore store = new DataStore();
        Offering first = new Offering("Editing", "Copy edit", 45.5);
        Offering second = new Offering("Editing", null, 0);
        store.addOffering(first);
        store.addOffering(second);
        check(store.getOfferingCount() == 2 && store.getOffering(1) == second, "offering add and display");
        check(store.searchOfferings(" dit ").size() == 2 && store.searchOfferings("").size() == 2,
                "offering search and show all");
        check(second.getDescription().isEmpty() && second.getUnitPrice() == 0, "optional description and zero price");
        rejects(() -> new Offering("", "", 0), "blank offering name");
        for (double invalid : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            rejects(() -> first.setUnitPrice(invalid), "invalid offering price");
        }
        check(first.getUnitPrice() == 45.5, "offering retains valid price");
        check(store.removeOffering(second) && store.getOffering(0) == first, "delete exact duplicate offering");
        System.out.println("PASS offering records, validation and duplicate selection");
    }

    private static void testCalculations() {
        Client client = new Client("Alex", "", "");
        Offering first = new Offering("Design", "", 80);
        Offering second = new Offering("Review", "", 20);
        Quote quote = new Quote(client);
        check(quote.getStatus() == Quote.Status.DRAFT && quote.getSubtotal() == 0 && quote.getTotal() == 0,
                "new quote defaults and empty totals");

        QuoteItem item = new QuoteItem(first, 1.5, first.getUnitPrice());
        check(item.getOffering() == first && item.getLineTotal() == 120, "fractional quantity and offering identity");
        quote.addItem(item);
        check(quote.getSubtotal() == 120 && quote.getTotal() == 120, "single item and zero discount");
        quote.addItem(new QuoteItem(second, 2, 20));
        quote.setDiscountPercent(10);
        check(quote.getSubtotal() == 160 && quote.getDiscountAmount() == 16 && quote.getTotal() == 144,
                "10 percent discount amount and total");
        quote.setDiscountPercent(25);
        check(quote.getSubtotal() == 160 && quote.getTotal() == 120, "multiple items and partial discount");
        quote.setDiscountPercent(100);
        check(quote.getTotal() == 0, "100 percent discount");
        quote.removeItem(item);
        check(quote.getSubtotal() == 40, "remove one of multiple items");
        quote.getItems().clear();
        check(quote.getItems().size() == 1, "external list changes do not affect quote");

        rejects(() -> new Quote(null), "missing client");
        rejects(() -> quote.setDate(null), "missing date");
        rejects(() -> quote.setStatus(null), "missing status");
        rejects(() -> quote.addItem(null), "missing item");
        rejects(() -> new QuoteItem(null, 1, 1), "missing offering");
        rejects(() -> new QuoteItem(first, 2, Double.MAX_VALUE), "line total overflow");
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> new QuoteItem(first, invalid, 10), "invalid quantity");
        }
        for (double invalid : new double[] {-1, Double.NaN, Double.NEGATIVE_INFINITY}) {
            rejects(() -> new QuoteItem(first, 1, invalid), "invalid item price");
        }
        for (double invalid : new double[] {-1, 100.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(() -> quote.setDiscountPercent(invalid), "invalid discount");
        }
        check(quote.getDiscountPercent() == 100 && new QuoteItem(first, 1, 0).getLineTotal() == 0,
                "invalid discount has no effect and zero-priced item is valid");

        Quote large = new Quote(client);
        QuoteItem largeItem = new QuoteItem(first, 1, Double.MAX_VALUE);
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
        quote.addItem(new QuoteItem(offering, 3, offering.getUnitPrice()));
        store.addQuote(quote);
        Quote duplicateName = new Quote(otherClient);
        duplicateName.addItem(new QuoteItem(otherOffering, 1, otherOffering.getUnitPrice()));
        store.addQuote(duplicateName);
        check(store.getQuoteReference(quote).equals("Q-0001") && store.getQuoteNumber(duplicateName) == 2
                && store.searchQuotes(" q-0002 ").get(0) == duplicateName, "stable references and reference search");
        check(store.getQuoteCount() == 2 && store.searchQuotes("alex").size() == 2, "save and find duplicate names");
        check(store.getQuote(0) == quote && quote.getClient() == client, "saved quote and client identity");
        rejects(() -> store.removeClient(client), "protect referenced client");
        rejects(() -> store.removeOffering(offering), "protect referenced offering");
        rejects(() -> store.addQuote(quote), "cannot save same object twice");

        client.setName("Alex Rivera");
        offering.setUnitPrice(180);
        check(store.searchQuotes("aLeX rIvErA").get(0) == quote, "client update visible on original quote");
        check(quote.getItems().get(0).getOffering() == offering && quote.getSubtotal() == 300,
                "existing items keep agreed price");
        Quote changes = new Quote(client);
        changes.updateFrom(quote);
        changes.setClient(new Client("Unregistered", "", ""));
        changes.setStatus(Quote.Status.SENT);
        rejects(() -> store.updateQuote(quote, changes), "cannot update to unregistered client");
        check(quote.getStatus() == Quote.Status.DRAFT && quote.getClient() == client, "invalid update is atomic");
        changes.setClient(otherClient);
        QuoteItem unavailable = new QuoteItem(new Offering("Unregistered", "", 1), 1, 1);
        changes.addItem(unavailable);
        rejects(() -> store.updateQuote(quote, changes), "cannot save an unregistered offering");
        changes.removeItem(unavailable);
        changes.setDate(LocalDate.of(2026, 11, 20));
        changes.setDiscountPercent(10);
        changes.setPreparedBy(" Studio Norte ");
        store.updateQuote(quote, changes);
        check(store.getQuote(0) == quote && quote.getClient() == otherClient, "quote update preserves identity");
        check(quote.getStatus() == Quote.Status.SENT && quote.getTotal() == 270
                && quote.getDate().equals(LocalDate.of(2026, 11, 20)) && quote.getPreparedBy().equals("Studio Norte")
                && store.getQuoteNumber(quote) == 1, "updated fields, recalculated total and same number");
        check(store.removeClient(client), "old client released after quote update");
        check(store.removeQuote(quote) && !store.removeQuote(quote) && store.removeOffering(offering),
                "delete exact quote and release offering");
        check(store.getQuote(0) == duplicateName, "other saved quotation survives deletion");
        Quote next = new Quote(otherClient);
        next.addItem(new QuoteItem(otherOffering, 1, 1));
        store.addQuote(next);
        check(store.getQuoteNumber(duplicateName) == 2 && store.getQuoteNumber(next) == 3,
                "deletion does not shift or reuse previous numbers");
        System.out.println("PASS shared relationships, saved quote updates and protected deletions");
    }

    private static void testPersistence() throws IOException {
        Path folder = Files.createTempDirectory("quotation-model-test-");
        Path data = folder.resolve("data");
        Path file = data.resolve("quotations.txt");
        Path malformed = folder.resolve("malformed.txt");
        String description = "Diseño\nBrief\twith tabs and a \\ path.";
        try {
            DataStore store = DataStore.open(file);
            check(store.getQuoteCount() == 0 && !Files.exists(file), "missing file starts empty");
            Client first = new Client("Alex", "one@example.com", "");
            Client second = new Client("Alex", "two@example.com", "");
            store.addClient(first);
            store.addClient(second);
            Offering basic = new Offering("Editing", "", 30);
            Offering detailed = new Offering("Editing", description, 120);
            store.addOffering(basic);
            store.addOffering(detailed);

            Quote removed = quoteWithItem(first, basic);
            store.addQuote(removed);
            Quote quote = quoteWithItem(second, detailed);
            store.addQuote(quote);
            store.removeQuote(removed);
            store.updateClient(second, new Client("Alex", "two@example.com", "555-0102"));
            store.updateOffering(detailed, new Offering("Editing", description, 160));

            Quote changes = new Quote(second);
            changes.updateFrom(quote);
            changes.setDate(LocalDate.of(2026, 11, 23));
            changes.setStatus(Quote.Status.ACCEPTED);
            changes.setPreparedBy("Estudio Ñandutí");
            changes.setDiscountPercent(10);
            store.updateQuote(quote, changes);

            DataStore reopened = DataStore.open(file);
            Quote reloaded = reopened.getQuote(0);
            QuoteItem item = reloaded.getItems().get(0);
            check(reopened.getClientCount() == 2 && reopened.getOfferingCount() == 2 && reopened.getQuoteCount() == 1
                    && reloaded.getClient() == reopened.getClient(1) && item.getOffering() == reopened.getOffering(1),
                    "restart restores duplicate records and points to the exact corresponding objects");
            check(item.getQuantity() == 2 && item.getUnitPrice() == 120
                    && item.getOffering().getDescription().equals(description) && item.getOffering().getUnitPrice() == 160
                    && reloaded.getClient().getPhone().equals("555-0102"),
                    "restart preserves quoted prices alongside current catalog and contact values");
            check(reopened.getQuoteNumber(reloaded) == 2 && reloaded.getStatus() == Quote.Status.ACCEPTED
                    && reloaded.getDate().equals(LocalDate.of(2026, 11, 23)) && reloaded.getDiscountPercent() == 10
                    && reloaded.getPreparedBy().equals("Estudio Ñandutí") && reloaded.getTotal() == 216,
                    "restart preserves quotation fields, totals, and reference");
            rejects(() -> reopened.removeClient(reloaded.getClient()), "restored client reference is protected");
            rejects(() -> reopened.removeOffering(item.getOffering()), "restored offering reference is protected");

            reopened.removeClient(reopened.getClient(0));
            reopened.removeOffering(reopened.getOffering(0));
            DataStore restarted = DataStore.open(file);
            Quote stillSaved = restarted.getQuote(0);
            check(stillSaved.getClient() == restarted.getClient(0)
                    && stillSaved.getItems().get(0).getOffering() == restarted.getOffering(0),
                    "deleting earlier unreferenced records keeps relationships correct");
            Quote next = quoteWithItem(restarted.getClient(0), restarted.getOffering(0));
            restarted.addQuote(next);
            check(restarted.getQuoteNumber(next) == 3, "number is not reused after restart");
            restarted.removeQuote(next);
            String snapshot = Files.readString(file);

            rejectsFile(malformed, snapshot.replace("ITEM\t1", "ITEM\t999"), "unknown offering reference");
            rejectsFile(malformed, snapshot.replace("NEXT\t4", "NEXT\t2"), "invalid next number");
            rejectsFile(malformed, snapshot.substring(0, snapshot.lastIndexOf("END")), "truncated snapshot");

            DataStore retry = DataStore.open(file);
            testWriteFailure(data, file, retry);
            check(Files.readString(file).equals(snapshot), "failed writes preserve the last snapshot");
            Quote afterFailure = quoteWithItem(retry.getClient(0), retry.getOffering(0));
            retry.addQuote(afterFailure);
            check(retry.getQuoteNumber(afterFailure) == 4 && DataStore.open(file).getQuoteCount() == 2,
                    "retry saves cleanly and deleted quotation numbers stay retired");
        } finally {
            Files.deleteIfExists(malformed);
            Files.deleteIfExists(file);
            Files.deleteIfExists(data);
            Files.deleteIfExists(folder);
        }
        System.out.println("PASS text persistence, restart, malformed input and write rollback");
    }

    private static Quote quoteWithItem(Client client, Offering offering) {
        Quote quote = new Quote(client);
        quote.addItem(new QuoteItem(offering, 2, offering.getUnitPrice()));
        return quote;
    }

    private static void testWriteFailure(Path data, Path file, DataStore store) throws IOException {
        Path backup = data.resolveSibling("backup");
        Files.move(data, backup);
        try {
            // A regular file in place of the data directory makes any write fail.
            Files.createFile(data);
            Quote saved = store.getQuote(0);
            Client client = store.getClient(0);
            Offering offering = store.getOffering(0);
            Quote draft = quoteWithItem(client, offering);
            rejectsWrite(() -> store.addQuote(draft), "add quote");
            rejectsWrite(() -> store.removeQuote(saved), "delete quote");
            rejectsWrite(() -> store.updateClient(client, new Client("Changed", "", "")), "edit client");
            rejectsWrite(() -> store.updateOffering(offering, new Offering("Changed", "", 99)), "edit offering");
            rejectsWrite(() -> store.updateQuote(saved, draft), "edit quote");
            check(store.getQuoteCount() == 1 && store.getQuote(0) == saved && store.getQuoteNumber(draft) == 0
                    && client.getName().equals("Alex") && offering.getUnitPrice() == 160
                    && saved.getDiscountPercent() == 10, "write errors roll back the attempted changes in memory");
        } finally {
            Files.deleteIfExists(data);
            Files.move(backup, data);
        }
        check(Files.isRegularFile(file), "original data file restored as test writing becomes available again");
    }

    private static void rejectsWrite(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected write failure for " + description);
    }

    private static void rejectsFile(Path file, String text, String description) throws IOException {
        Files.writeString(file, text);
        try {
            DataStore.open(file);
        } catch (IOException expected) {
            check(Files.readString(file).equals(text), "malformed data stays available for recovery");
            return;
        }
        throw new AssertionError("Expected data file to be rejected for " + description);
    }

    private static void rejects(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected validation error for " + description);
    }

    static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
