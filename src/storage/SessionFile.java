package storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;

public final class SessionFile {
    public static final String EXTENSION = ".qbs";
    private static final int SIGNATURE = 0x51425346; // QBSF
    private static final int VERSION = 1;

    private SessionFile() {
    }

    public static void save(Path file, DataStore data) throws IOException {
        Path target = file.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(target.getParent(), ".quotation-session-", ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(SIGNATURE);
                out.writeInt(VERSION);
                writeRecords(out, data);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static DataStore load(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != SIGNATURE) {
                throw new IOException("Choose a Quotation Builder session file (" + EXTENSION + ").");
            }
            if (in.readInt() != VERSION) {
                throw new IOException("This version of the app cannot open the selected session format.");
            }
            DataStore data = readRecords(in);
            if (in.read() != -1) {
                throw new IOException("The session file contains unexpected data.");
            }
            return data;
        } catch (EOFException error) {
            throw new IOException("The session file is incomplete.", error);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | DateTimeException error) {
            throw new IOException("The session file contains invalid records.", error);
        }
    }

    private static void writeRecords(DataOutputStream out, DataStore data) throws IOException {
        out.writeInt(data.getClientCount());
        for (int i = 0; i < data.getClientCount(); i++) {
            Client client = data.getClient(i);
            writeText(out, client.getName());
            writeText(out, client.getEmail());
            writeText(out, client.getPhone());
        }

        out.writeInt(data.getOfferingCount());
        for (int i = 0; i < data.getOfferingCount(); i++) {
            Offering offering = data.getOffering(i);
            writeText(out, offering.getName());
            writeText(out, offering.getDescription());
            out.writeDouble(offering.getUnitPrice());
        }

        out.writeInt(data.getQuoteCount());
        for (int i = 0; i < data.getQuoteCount(); i++) {
            Quote quote = data.getQuote(i);
            writeReference(out, data.getClientNumber(quote.getClient()));
            out.writeLong(quote.getDate().toEpochDay());
            writeText(out, quote.getStatus().name());
            out.writeDouble(quote.getDiscountPercent());

            List<QuoteItem> items = quote.getItems();
            if (items.isEmpty()) {
                throw new IOException("A quotation needs at least one item before saving the session.");
            }
            out.writeInt(items.size());
            for (QuoteItem item : items) {
                writeReference(out, data.getOfferingNumber(item.getOffering()));
                out.writeDouble(item.getQuantity());
                out.writeDouble(item.getUnitPrice());
                out.writeDouble(item.getDiscountPercent());
            }
        }
    }

    private static DataStore readRecords(DataInputStream in) throws IOException {
        DataStore data = new DataStore();
        int clientCount = readSize(in);
        for (int i = 0; i < clientCount; i++) {
            data.addClient(new Client(readText(in), readText(in), readText(in)));
        }

        int offeringCount = readSize(in);
        for (int i = 0; i < offeringCount; i++) {
            data.addOffering(new Offering(readText(in), readText(in), in.readDouble()));
        }

        int quoteCount = readSize(in);
        for (int i = 0; i < quoteCount; i++) {
            Quote quote = new Quote(data.getClient(in.readInt()));
            quote.setDate(LocalDate.ofEpochDay(in.readLong()));
            quote.setStatus(Quote.Status.valueOf(readText(in)));
            quote.setDiscountPercent(in.readDouble());

            int itemCount = readSize(in);
            for (int j = 0; j < itemCount; j++) {
                Offering offering = data.getOffering(in.readInt());
                quote.addItem(new QuoteItem(offering, in.readDouble(), in.readDouble(), in.readDouble()));
            }
            data.addQuote(quote);
        }
        return data;
    }

    private static void writeReference(DataOutputStream out, int recordNumber) throws IOException {
        if (recordNumber <= 0) {
            throw new IOException("A quotation uses a client or offering that is missing from this session.");
        }
        out.writeInt(recordNumber - 1);
    }

    private static void writeText(DataOutputStream out, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readText(DataInputStream in) throws IOException {
        int length = readSize(in);
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int readSize(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0) {
            throw new IOException("The session file contains an invalid record or text length.");
        }
        return size;
    }
}
