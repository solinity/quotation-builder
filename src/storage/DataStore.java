package storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import model.SessionCurrency;

public class DataStore {
    private final ArrayList<Client> clients = new ArrayList<>();
    private final ArrayList<Offering> offerings = new ArrayList<>();
    private final ArrayList<Quote> quotes = new ArrayList<>();
    private CurrencySettings currencySettings = new CurrencySettings(SessionCurrency.USD, SessionCurrency.USD, null);

    public void replaceWith(DataStore source) {
        Objects.requireNonNull(source);
        if (source != this) {
            clients.clear();
            clients.addAll(source.clients);
            offerings.clear();
            offerings.addAll(source.offerings);
            quotes.clear();
            quotes.addAll(source.quotes);
            currencySettings = source.currencySettings;
        }
    }

    public CurrencySettings getCurrencySettings() {
        return currencySettings;
    }

    public void setCurrencySettings(CurrencySettings currencySettings) {
        this.currencySettings = Objects.requireNonNull(currencySettings);
    }

    public void addClient(Client client) {
        if (client == null || hasClient(client)) {
            throw new IllegalArgumentException("Provide a new client record.");
        }
        clients.add(client);
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
        String keyword = name.trim();
        ArrayList<Client> matches = new ArrayList<>();
        for (Client client : clients) {
            if (keyword.isEmpty() || client.getName().equalsIgnoreCase(keyword)) {
                matches.add(client);
            }
        }
        return matches;
    }

    public List<Client> findClientsByName(String name) {
        return findByName(clients, Client::getName, name);
    }

    public void addOffering(Offering offering) {
        if (offering == null || hasOffering(offering)) {
            throw new IllegalArgumentException("Provide a new offering record.");
        }
        offerings.add(offering);
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
        String keyword = name.trim();
        ArrayList<Offering> matches = new ArrayList<>();
        for (Offering offering : offerings) {
            if (keyword.isEmpty() || offering.getName().equalsIgnoreCase(keyword)) {
                matches.add(offering);
            }
        }
        return matches;
    }

    public List<Offering> findOfferingsByName(String name) {
        return findByName(offerings, Offering::getName, name);
    }

    public void addQuote(Quote quote) {
        if (indexOfRecord(quotes, quote) >= 0) {
            throw new IllegalArgumentException("This quotation has already been saved.");
        }
        validateQuote(quote);
        quotes.add(quote);
    }

    public void updateQuote(Quote savedQuote, Quote changes) {
        if (indexOfRecord(quotes, savedQuote) < 0) {
            throw new IllegalArgumentException("The selected quotation could not be found.");
        }
        validateQuote(changes);
        savedQuote.updateFrom(changes);
    }

    public boolean removeQuote(Quote quote) {
        return removeRecord(quotes, quote);
    }

    public int getQuoteCount() {
        return quotes.size();
    }

    public Quote getQuote(int index) {
        return quotes.get(index);
    }

    public int getQuoteNumber(Quote quote) {
        return indexOfRecord(quotes, quote) + 1;
    }

    public ArrayList<Quote> searchQuotes(String clientName) {
        String keyword = clientName.trim();
        ArrayList<Quote> matches = new ArrayList<>();
        for (Quote quote : quotes) {
            if (keyword.isEmpty() || quote.getClient().getName().equalsIgnoreCase(keyword)) {
                matches.add(quote);
            }
        }
        return matches;
    }

    public boolean isClientReferenced(Client client) {
        for (Quote quote : quotes) {
            if (quote.getClient() == client) {
                return true;
            }
        }
        return false;
    }

    public boolean isOfferingReferenced(Offering offering) {
        for (Quote quote : quotes) {
            for (QuoteItem item : quote.getItems()) {
                if (item.getOffering() == offering) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateQuote(Quote quote) {
        if (quote == null || !hasClient(quote.getClient())) {
            throw new IllegalArgumentException("Select a registered client.");
        }
        List<QuoteItem> items = quote.getItems();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one item before saving.");
        }
        for (QuoteItem item : items) {
            if (!hasOffering(item.getOffering())) {
                throw new IllegalArgumentException(
                    "An item uses an offering that was deleted. Remove that item before saving.");
            }
        }
    }

    private <T> List<T> findByName(List<T> records, Function<T, String> nameOf, String name) {
        String keyword = name.strip().toLowerCase(Locale.ROOT);
        List<T> matches = new ArrayList<>();
        for (T record : records) {
            if (nameOf.apply(record).toLowerCase(Locale.ROOT).contains(keyword)) {
                matches.add(record);
            }
        }
        return matches;
    }

    private int indexOfRecord(ArrayList<?> records, Object selected) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i) == selected) {
                return i;
            }
        }
        return -1;
    }

    private boolean removeRecord(ArrayList<?> records, Object selected) {
        int index = indexOfRecord(records, selected);
        if (index < 0) {
            return false;
        }
        records.remove(index);
        return true;
    }
}
