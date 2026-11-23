package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;

public class DataStore {
    private static final String FILE_HEADER = "Freelance Quotations 1";

    private final Path file;
    private ArrayList<Client> clients = new ArrayList<>();
    private ArrayList<Offering> offerings = new ArrayList<>();
    private ArrayList<Quote> quotes = new ArrayList<>();
    private int nextQuoteNumber = 1;

    public DataStore() throws IOException {
        file = Path.of(System.getProperty("user.home"), "freelance-quotations.txt");
        if (!Files.notExists(file)) {
            load();
        }
    }

    public ArrayList<Client> getClients() {
        return new ArrayList<>(clients);
    }

    public ArrayList<Offering> getOfferings() {
        return new ArrayList<>(offerings);
    }

    public ArrayList<Quote> getQuotes() {
        return new ArrayList<>(quotes);
    }

    public int getNextQuoteNumber() {
        return nextQuoteNumber;
    }

    public void saveClients(ArrayList<Client> updated) throws IOException {
        writeFile(updated, offerings, quotes, nextQuoteNumber);
        clients = new ArrayList<>(updated);
    }

    public void saveOfferings(ArrayList<Offering> updated) throws IOException {
        writeFile(clients, updated, quotes, nextQuoteNumber);
        offerings = new ArrayList<>(updated);
    }

    public void saveQuotes(ArrayList<Quote> updated) throws IOException {
        int next = nextQuoteNumber;
        for (Quote quote : updated) {
            if (quote.getNumber() >= next) {
                next = quote.getNumber() + 1;
            }
        }

        writeFile(clients, offerings, updated, next);
        quotes = new ArrayList<>(updated);
        nextQuoteNumber = next;
    }

    private void load() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            if (!FILE_HEADER.equals(readLine(reader))) {
                throw new IOException("Unrecognized file format.");
            }
            nextQuoteNumber = Integer.parseInt(readLine(reader));
            if (nextQuoteNumber < 1) {
                throw new IOException("The data file has an invalid next quote number.");
            }

            int clientCount = readCount(reader);
            for (int i = 0; i < clientCount; i++) {
                clients.add(readClient(reader));
            }

            int offeringCount = readCount(reader);
            for (int i = 0; i < offeringCount; i++) {
                offerings.add(readOffering(reader));
            }

            int quoteCount = readCount(reader);
            for (int i = 0; i < quoteCount; i++) {
                quotes.add(readQuote(reader));
            }
        } catch (IOException | IllegalArgumentException | DateTimeParseException error) {
            throw new IOException("Could not read " + file + "\n" + error.getMessage(), error);
        }
    }

    // Each record starts with its count or has a fixed number of lines.
    private Quote readQuote(BufferedReader reader) throws IOException {
        int number = Integer.parseInt(readLine(reader));
        LocalDate date = LocalDate.parse(readLine(reader));
        Client client = readClient(reader);
        String project = readLine(reader);
        Quote.Status status = Quote.Status.valueOf(readLine(reader));
        int discount = Integer.parseInt(readLine(reader));
        String notes = readLine(reader);
        ArrayList<QuoteItem> items = new ArrayList<>();

        int itemCount = readCount(reader);
        for (int i = 0; i < itemCount; i++) {
            Offering offering = readOffering(reader);
            int quantity = Integer.parseInt(readLine(reader));
            items.add(new QuoteItem(offering, quantity));
        }

        return new Quote(number, date, client, project, status, discount, notes, items);
    }

    private Client readClient(BufferedReader reader) throws IOException {
        return new Client(readLine(reader), readLine(reader), readLine(reader));
    }

    private Offering readOffering(BufferedReader reader) throws IOException {
        return new Offering(readLine(reader), readLine(reader), Long.parseLong(readLine(reader)));
    }

    private String readLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("The data file ended unexpectedly.");
        }
        return line;
    }

    private int readCount(BufferedReader reader) throws IOException {
        int count = Integer.parseInt(readLine(reader));
        if (count < 0) {
            throw new IOException("The data file has an invalid record count.");
        }
        return count;
    }

    private void writeFile(ArrayList<Client> updatedClients, ArrayList<Offering> updatedOfferings,
            ArrayList<Quote> updatedQuotes, int nextNumber) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), "quotations-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) {
                writeLine(writer, FILE_HEADER);
                writeLine(writer, "" + nextNumber);
                writeLine(writer, "" + updatedClients.size());
                for (Client client : updatedClients) {
                    writeClient(writer, client);
                }

                writeLine(writer, "" + updatedOfferings.size());
                for (Offering offering : updatedOfferings) {
                    writeOffering(writer, offering);
                }

                writeLine(writer, "" + updatedQuotes.size());
                for (Quote quote : updatedQuotes) {
                    writeQuote(writer, quote);
                }
            }

            // Finish the new file before replacing the last successful save.
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void writeQuote(BufferedWriter writer, Quote quote) throws IOException {
        writeLine(writer, "" + quote.getNumber());
        writeLine(writer, quote.getDate().toString());
        writeClient(writer, quote.getClient());
        writeLine(writer, quote.getProject());
        writeLine(writer, quote.getStatus().name());
        writeLine(writer, "" + quote.getDiscount());
        writeLine(writer, quote.getNotes());

        ArrayList<QuoteItem> items = quote.getItems();
        writeLine(writer, "" + items.size());
        for (QuoteItem item : items) {
            writeOffering(writer, item.getOffering());
            writeLine(writer, "" + item.getQuantity());
        }
    }

    private void writeClient(BufferedWriter writer, Client client) throws IOException {
        writeLine(writer, client.getName());
        writeLine(writer, client.getEmail());
        writeLine(writer, client.getPhone());
    }

    private void writeOffering(BufferedWriter writer, Offering offering) throws IOException {
        writeLine(writer, offering.getName());
        writeLine(writer, offering.getUnit());
        writeLine(writer, "" + offering.getRate());
    }

    private void writeLine(BufferedWriter writer, String value) throws IOException {
        writer.write(value);
        writer.newLine();
    }
}
