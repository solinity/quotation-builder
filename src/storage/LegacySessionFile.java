package storage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import model.Client;
import model.Offering;
import model.Quote;
import model.QuoteItem;

public final class LegacySessionFile {
    private static final String EXTENSION = ".qbs";
    private static final int SIGNATURE = 0x51425346; // QBSF
    private static final int VERSION = 1;

    private LegacySessionFile() {
    }

    public static boolean matches(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
    }

    static DataStore load(Path file) throws IOException {
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
