package storage;

import static model.Validation.require;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import model.CatalogEntry.Client;
import model.CurrencySettings;
import model.CurrencySettings.SessionCurrency;
import model.CatalogEntry.Offering;
import model.Quote;
import model.Quote.Item;

public class DataStore {
    private final List<Client> clients = new ArrayList<>();
    private final List<Offering> offerings = new ArrayList<>();
    private final List<Quote> quotes = new ArrayList<>();
    private CurrencySettings currencySettings = new CurrencySettings(SessionCurrency.USD, SessionCurrency.USD, null);

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public List<Offering> getOfferings() {
        return Collections.unmodifiableList(offerings);
    }

    public List<Quote> getQuotes() {
        return Collections.unmodifiableList(quotes);
    }

    public static <T> int numberOf(List<T> records, T record) {
        return records.indexOf(record) + 1;
    }

    public boolean isEmpty() {
        return clients.isEmpty() && offerings.isEmpty() && quotes.isEmpty();
    }

    public void replaceWith(DataStore source) {
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

    public void setCurrencySettings(CurrencySettings settings) {
        currencySettings = Objects.requireNonNull(settings);
    }

    public void addClient(Client client) {
        require(client != null && !clients.contains(client), "Provide a new client record.");
        clients.add(client);
    }

    public void addOffering(Offering offering) {
        require(offering != null && !offerings.contains(offering), "Provide a new offering record.");
        offerings.add(offering);
    }

    public boolean removeClient(Client client) {
        require(client != null, "Select a client to delete.");
        require(!isClientReferenced(client), "This client is used by a saved quotation.");
        return clients.remove(client);
    }

    public boolean removeOffering(Offering offering) {
        require(offering != null, "Select an offering to delete.");
        require(!isOfferingReferenced(offering), "This offering is used by a saved quotation.");
        return offerings.remove(offering);
    }

    public void addQuote(Quote quote) {
        require(!quotes.contains(quote), "This quotation has already been saved.");
        validateQuote(quote);
        quotes.add(quote);
    }

    public void updateQuote(Quote saved, Quote changes) {
        require(quotes.contains(saved), "The selected quotation could not be found.");
        validateQuote(changes);
        saved.updateFrom(changes);
    }

    public boolean removeQuote(Quote quote) {
        return quotes.remove(quote);
    }

    public boolean isClientReferenced(Client client) {
        return quotes.stream().anyMatch(quote -> quote.getClient() == client);
    }

    public boolean isOfferingReferenced(Offering offering) {
        return quotes.stream().flatMap(quote -> quote.getItems().stream())
                .anyMatch(item -> item.getOffering() == offering);
    }

    private void validateQuote(Quote quote) {
        require(quote != null && clients.contains(quote.getClient()), "Select a registered client.");
        List<Item> items = quote.getItems();
        require(!items.isEmpty(), "Add at least one item before saving.");
        for (Item item : items) {
            require(offerings.contains(item.getOffering()),
                    "An item uses an offering that was deleted. Remove that item before saving.");
        }
    }

    public List<Client> searchClients(String query, boolean partial) {
        return search(clients, Client::getName, query, partial);
    }

    public List<Offering> searchOfferings(String query, boolean partial) {
        return search(offerings, Offering::getName, query, partial);
    }

    public List<Quote> searchQuotes(String query) {
        return search(quotes, quote -> quote.getClient().getName(), query, false);
    }

    private static <T> List<T> search(List<T> records, Function<T, String> nameOf, String query, boolean partial) {
        String keyword = partial ? query.strip().toLowerCase(Locale.ROOT) : query.trim();
        List<T> matches = new ArrayList<>();
        for (T record : records) {
            String name = nameOf.apply(record);
            if (partial ? name.toLowerCase(Locale.ROOT).contains(keyword)
                    : keyword.isEmpty() || name.equalsIgnoreCase(keyword)) {
                matches.add(record);
            }
        }
        return matches;
    }
}
