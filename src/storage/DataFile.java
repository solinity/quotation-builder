package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;

final class DataFile {
    private static final String HEADER = "FREELANCE-QUOTATIONS\t1";

    private DataFile() {
    }

    static void load(Path file, DataStore store) throws IOException {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException missing) {
            return;
        }

        try (BufferedReader input = reader) {
            if (!HEADER.equals(input.readLine())) {
                throw new IllegalArgumentException("Unrecognized file header.");
            }
            String[] next = readRow(input);
            requireRow(next, "NEXT", 2);
            readRecords(input, store);
            store.restoreNextQuoteNumber(Integer.parseInt(next[1]));
        } catch (IllegalArgumentException | DateTimeParseException | IndexOutOfBoundsException error) {
            throw new IOException("Invalid quotation data. " + error.getMessage(), error);
        }
    }

    private static void readRecords(BufferedReader input, DataStore store) throws IOException {
        while (true) {
            String[] fields = readRow(input);
            switch (fields[0]) {
                case "CLIENT":
                    requireRow(fields, "CLIENT", 4);
                    store.addClient(new Client(fields[1], fields[2], fields[3]));
                    break;
                case "OFFERING":
                    requireRow(fields, "OFFERING", 4);
                    store.addOffering(new Offering(fields[1], fields[2], Double.parseDouble(fields[3])));
                    break;
                case "QUOTE":
                    readQuote(input, store, fields);
                    break;
                case "END":
                    requireRow(fields, "END", 1);
                    if (input.readLine() != null) {
                        throw new IllegalArgumentException("Unexpected content after the last record.");
                    }
                    return;
                default:
                    throw new IllegalArgumentException("Unknown record type.");
            }
        }
    }

    private static void readQuote(BufferedReader input, DataStore store, String[] fields) throws IOException {
        requireRow(fields, "QUOTE", 8);
        int number = Integer.parseInt(fields[1]);
        Client client = store.getClient(Integer.parseInt(fields[2]) - 1);
        Quote quote = new Quote(client);
        quote.setDate(LocalDate.parse(fields[3]));
        quote.setStatus(Quote.Status.valueOf(fields[4]));
        quote.setDiscountPercent(Double.parseDouble(fields[5]));
        quote.setPreparedBy(fields[6]);
        int itemCount = Integer.parseInt(fields[7]);
        for (int i = 0; i < itemCount; i++) {
            String[] item = readRow(input);
            requireRow(item, "ITEM", 4);
            Offering offering = store.getOffering(Integer.parseInt(item[1]) - 1);
            quote.addItem(new QuoteItem(offering, Double.parseDouble(item[2]), Double.parseDouble(item[3])));
        }
        store.restoreQuote(number, quote);
    }

    private static String[] readRow(BufferedReader input) throws IOException {
        String line = input.readLine();
        if (line == null) {
            throw new IllegalArgumentException("File ended before all records were read.");
        }
        String[] fields = line.split("\t", -1);
        for (int i = 1; i < fields.length; i++) {
            fields[i] = unescape(fields[i]);
        }
        return fields;
    }

    private static void requireRow(String[] fields, String type, int length) {
        if (!type.equals(fields[0]) || fields.length != length) {
            throw new IllegalArgumentException("Expected a complete " + type + " record.");
        }
    }

    static void save(Path file, DataStore store) throws IOException {
        Path folder = file.getParent();
        Files.createDirectories(folder);
        Path pending = Files.createTempFile(folder, "quotations-", ".tmp");
        try {
            try (BufferedWriter output = Files.newBufferedWriter(pending, StandardCharsets.UTF_8)) {
                output.write(HEADER);
                output.newLine();
                writeRow(output, "NEXT", Integer.toString(store.getNextQuoteNumber()));
                writeRecords(output, store);
                writeRow(output, "END");
            }
            // Replace the old snapshot only after all new records have been written.
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private static void writeRecords(BufferedWriter output, DataStore store) throws IOException {
        for (int i = 0; i < store.getClientCount(); i++) {
            Client client = store.getClient(i);
            writeRow(output, "CLIENT", client.getName(), client.getEmail(), client.getPhone());
        }
        for (int i = 0; i < store.getOfferingCount(); i++) {
            Offering offering = store.getOffering(i);
            writeRow(output, "OFFERING", offering.getName(), offering.getDescription(), Double.toString(offering.getUnitPrice()));
        }
        for (int i = 0; i < store.getQuoteCount(); i++) {
            Quote quote = store.getQuote(i);
            List<QuoteItem> items = quote.getItems();
            writeRow(output, "QUOTE", Integer.toString(store.getQuoteNumber(quote)),
                    Integer.toString(store.getClientNumber(quote.getClient())), quote.getDate().toString(),
                    quote.getStatus().name(), Double.toString(quote.getDiscountPercent()), quote.getPreparedBy(),
                    Integer.toString(items.size()));
            for (QuoteItem item : items) {
                writeRow(output, "ITEM", Integer.toString(store.getOfferingNumber(item.getOffering())),
                        Double.toString(item.getQuantity()), Double.toString(item.getUnitPrice()));
            }
        }
    }

    private static void writeRow(BufferedWriter output, String type, String... fields) throws IOException {
        output.write(type);
        for (String field : fields) {
            output.write('\t');
            output.write(field.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n"));
        }
        output.newLine();
    }

    private static String unescape(String field) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char current = field.charAt(i);
            if (current != '\\') {
                text.append(current);
                continue;
            }
            i++;
            if (i == field.length()) {
                throw new IllegalArgumentException("Incomplete escaped character.");
            }
            switch (field.charAt(i)) {
                case '\\': text.append('\\'); break;
                case 't': text.append('\t'); break;
                case 'r': text.append('\r'); break;
                case 'n': text.append('\n'); break;
                default: throw new IllegalArgumentException("Unknown escaped character.");
            }
        }
        return text.toString();
    }
}
