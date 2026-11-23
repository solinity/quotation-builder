package storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;

public class DataStore {
    private final ArrayList<Client> clients = new ArrayList<>();
    private final ArrayList<Offering> offerings = new ArrayList<>();
    private final ArrayList<SavedQuote> quotes = new ArrayList<>();
    private int nextQuoteNumber = 1;
    private Path file;

    private static class SavedQuote {
        private final int number;
        private final Quote quote;

        private SavedQuote(int number, Quote quote) {
            this.number = number;
            this.quote = quote;
        }
    }

    public static DataStore open(Path file) throws IOException {
        DataStore store = new DataStore();
        Path location = file.toAbsolutePath();
        DataFile.load(location, store);
        store.file = location;
        return store;
    }

    public void addClient(Client client) {
        if (client == null || hasClient(client)) {
            throw new IllegalArgumentException("Provide a new client record.");
        }
        addRecord(clients, client);
    }

    public void updateClient(Client selected, Client changes) {
        if (!hasClient(selected) || changes == null) {
            throw new IllegalArgumentException("Select a registered client.");
        }
        Client original = new Client(selected.getName(), selected.getEmail(), selected.getPhone());
        copyClient(selected, changes);
        try {
            saveChanges();
        } catch (IllegalStateException error) {
            copyClient(selected, original);
            throw error;
        }
    }

    private static void copyClient(Client target, Client source) {
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
    }

    public boolean removeClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Select a client to delete.");
        }
        if (isClientReferenced(client)) {
            throw new IllegalArgumentException("This client is used by a saved quotation.");
        }
        return removeRecord(clients, client);
    }

    public int getClientCount() {
        return clients.size();
    }

    public Client getClient(int index) {
        return clients.get(index);
    }

    public boolean hasClient(Client client) {
        return indexOfRecord(clients, client) >= 0;
    }

    public int getClientNumber(Client client) {
        return indexOfRecord(clients, client) + 1;
    }

    public ArrayList<Client> searchClients(String name) {
        String keyword = searchTerm(name);
        ArrayList<Client> matches = new ArrayList<>();
        for (Client client : clients) {
            if (contains(client.getName(), keyword)) {
                matches.add(client);
            }
        }
        return matches;
    }

    public void addOffering(Offering offering) {
        if (offering == null || hasOffering(offering)) {
            throw new IllegalArgumentException("Provide a new offering record.");
        }
        addRecord(offerings, offering);
    }

    public void updateOffering(Offering selected, Offering changes) {
        if (!hasOffering(selected) || changes == null) {
            throw new IllegalArgumentException("Select a registered offering.");
        }
        Offering original = new Offering(selected.getName(), selected.getDescription(), selected.getUnitPrice());
        copyOffering(selected, changes);
        try {
            saveChanges();
        } catch (IllegalStateException error) {
            copyOffering(selected, original);
            throw error;
        }
    }

    private static void copyOffering(Offering target, Offering source) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setUnitPrice(source.getUnitPrice());
    }

    public boolean removeOffering(Offering offering) {
        if (offering == null) {
            throw new IllegalArgumentException("Select an offering to delete.");
        }
        if (isOfferingReferenced(offering)) {
            throw new IllegalArgumentException("This offering is used by a saved quotation.");
        }
        return removeRecord(offerings, offering);
    }

    public int getOfferingCount() {
        return offerings.size();
    }

    public Offering getOffering(int index) {
        return offerings.get(index);
    }

    public boolean hasOffering(Offering offering) {
        return indexOfRecord(offerings, offering) >= 0;
    }

    public int getOfferingNumber(Offering offering) {
        return indexOfRecord(offerings, offering) + 1;
    }

    public ArrayList<Offering> searchOfferings(String name) {
        String keyword = searchTerm(name);
        ArrayList<Offering> matches = new ArrayList<>();
        for (Offering offering : offerings) {
            if (contains(offering.getName(), keyword)) {
                matches.add(offering);
            }
        }
        return matches;
    }

    public void addQuote(Quote quote) {
        if (findQuoteIndex(quote) >= 0) {
            throw new IllegalArgumentException("This quotation has already been saved.");
        }
        validateQuote(quote);
        if (nextQuoteNumber == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("All quotation numbers have been used.");
        }
        SavedQuote saved = new SavedQuote(nextQuoteNumber, quote);
        nextQuoteNumber++;
        try {
            addRecord(quotes, saved);
        } catch (IllegalStateException error) {
            nextQuoteNumber--;
            throw error;
        }
    }

    public void updateQuote(Quote savedQuote, Quote changes) {
        if (findQuoteIndex(savedQuote) < 0) {
            throw new IllegalArgumentException("The selected quotation could not be found.");
        }
        validateQuote(changes);
        Quote original = new Quote(savedQuote.getClient());
        original.updateFrom(savedQuote);
        savedQuote.updateFrom(changes);
        try {
            saveChanges();
        } catch (IllegalStateException error) {
            savedQuote.updateFrom(original);
            throw error;
        }
    }

    public boolean removeQuote(Quote quote) {
        int index = findQuoteIndex(quote);
        if (index < 0) {
            return false;
        }
        return removeRecord(quotes, quotes.get(index));
    }

    public int getQuoteCount() {
        return quotes.size();
    }

    public Quote getQuote(int index) {
        return quotes.get(index).quote;
    }

    public int getQuoteNumber(Quote quote) {
        int index = findQuoteIndex(quote);
        return index < 0 ? 0 : quotes.get(index).number;
    }

    public String getQuoteReference(Quote quote) {
        int number = getQuoteNumber(quote);
        return number == 0 ? "" : formatQuoteNumber(number);
    }

    private static String formatQuoteNumber(int number) {
        return String.format(Locale.ROOT, "Q-%04d", number);
    }

    int getNextQuoteNumber() {
        return nextQuoteNumber;
    }

    void restoreQuote(int number, Quote quote) {
        if (number <= 0 || (!quotes.isEmpty() && number <= quotes.get(quotes.size() - 1).number)) {
            throw new IllegalArgumentException("Quotation numbers must be positive and in increasing order.");
        }
        validateQuote(quote);
        quotes.add(new SavedQuote(number, quote));
    }

    void restoreNextQuoteNumber(int number) {
        int largest = quotes.isEmpty() ? 0 : quotes.get(quotes.size() - 1).number;
        if (number <= largest) {
            throw new IllegalArgumentException("The next quotation number must follow all saved numbers.");
        }
        nextQuoteNumber = number;
    }

    private int findQuoteIndex(Quote quote) {
        for (int i = 0; i < quotes.size(); i++) {
            if (quotes.get(i).quote == quote) {
                return i;
            }
        }
        return -1;
    }

    public ArrayList<Quote> searchQuotes(String query) {
        String keyword = searchTerm(query);
        ArrayList<Quote> matches = new ArrayList<>();
        for (SavedQuote saved : quotes) {
            if (contains(saved.quote.getClient().getName(), keyword)
                    || contains(formatQuoteNumber(saved.number), keyword)) {
                matches.add(saved.quote);
            }
        }
        return matches;
    }

    private static String searchTerm(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String keyword) {
        return value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public boolean isClientReferenced(Client client) {
        for (SavedQuote saved : quotes) {
            if (saved.quote.getClient() == client) {
                return true;
            }
        }
        return false;
    }

    public boolean isOfferingReferenced(Offering offering) {
        for (SavedQuote saved : quotes) {
            for (QuoteItem item : saved.quote.getItems()) {
                if (item.getOffering() == offering) {
                    return true;
                }
            }
        }
        return false;
    }

    public void validateQuote(Quote quote) {
        if (quote == null || !hasClient(quote.getClient())) {
            throw new IllegalArgumentException("Select a registered client.");
        }
        List<QuoteItem> items = quote.getItems();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one item to the quotation.");
        }
        for (QuoteItem item : items) {
            if (!hasOffering(item.getOffering())) {
                throw new IllegalArgumentException(
                    "An item uses an offering that was deleted. Remove that item before saving.");
            }
        }
    }

    private int indexOfRecord(ArrayList<?> records, Object selected) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i) == selected) {
                return i;
            }
        }
        return -1;
    }

    private <T> void addRecord(ArrayList<T> records, T record) {
        records.add(record);
        try {
            saveChanges();
        } catch (IllegalStateException error) {
            records.remove(records.size() - 1);
            throw error;
        }
    }

    private <T> boolean removeRecord(ArrayList<T> records, T selected) {
        int index = indexOfRecord(records, selected);
        if (index < 0) {
            return false;
        }
        records.remove(index);
        try {
            saveChanges();
        } catch (IllegalStateException error) {
            records.add(index, selected);
            throw error;
        }
        return true;
    }

    private void saveChanges() {
        if (file == null) {
            return;
        }
        try {
            DataFile.save(file, this);
        } catch (IOException | SecurityException error) {
            throw new IllegalStateException("Could not write the data file. The change was not saved.\n" + file, error);
        }
    }
}
