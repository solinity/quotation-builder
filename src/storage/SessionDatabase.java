package storage;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import model.Quote;
import model.QuoteItem;
import model.SessionCurrency;

public final class SessionDatabase {
    public static final String EXTENSION = ".sqlite";
    private static final int APPLICATION_ID = 0x51425344; // QBSD
    private static final int SCHEMA_VERSION = 3;

    private SessionDatabase() {
    }

    public static void save(Path file, DataStore data) throws IOException {
        Path target = file.toAbsolutePath().normalize();
        if (Files.exists(target)) {
            write(target, data, false);
            return;
        }

        Path temporary = Files.createTempFile(target.getParent(), ".quotation-session-", EXTENSION);
        try {
            write(temporary, data, true);
            Files.move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static DataStore load(Path file) throws IOException {
        if (LegacySessionFile.matches(file)) {
            return LegacySessionFile.load(file);
        }

        try (Connection database = open(file, "ro"); Statement sql = database.createStatement()) {
            database.setAutoCommit(false);
            int version = checkSchema(sql);

            DataStore data = new DataStore();
            if (version >= 2) {
                data.setCurrencySettings(readCurrencySettings(sql, version));
            }
            Map<Long, Client> clients = readClients(sql, data);
            Map<Long, Offering> offerings = readOfferings(sql, data);
            readQuotes(sql, data, clients, offerings);
            database.commit();
            return data;
        } catch (SQLException error) {
            throw databaseError("read", error);
        } catch (IllegalArgumentException | DateTimeException error) {
            throw new IOException("The session database contains invalid records.", error);
        }
    }

    private static Connection open(Path file, String mode) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.toUri().toASCIIString() + "?mode=" + mode);
    }

    private static void write(Path file, DataStore data, boolean newFile) throws IOException {
        try (Connection database = open(file, "rw"); Statement sql = database.createStatement()) {
            sql.execute("PRAGMA foreign_keys = ON");
            database.setAutoCommit(false);
            try {
                if (newFile) {
                    createSchema(sql);
                } else {
                    upgradeSettingsTable(sql, checkSchema(sql));
                    sql.executeUpdate("DELETE FROM quotations");
                    sql.executeUpdate("DELETE FROM offerings");
                    sql.executeUpdate("DELETE FROM clients");
                }

                writeCurrencySettings(database, data.getCurrencySettings());
                writeClients(database, data);
                writeOfferings(database, data);
                writeQuotes(database, data);
                database.commit();
            } catch (SQLException | IOException | RuntimeException error) {
                try {
                    database.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        } catch (SQLException error) {
            throw databaseError("write", error);
        }
    }

    private static void createSchema(Statement sql) throws SQLException {
        sql.execute("PRAGMA application_id = " + APPLICATION_ID);
        sql.execute("CREATE TABLE clients ("
                + "id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL, phone TEXT NOT NULL) STRICT");
        sql.execute("CREATE TABLE offerings ("
                + "id INTEGER PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL,"
                + " unit_price REAL NOT NULL CHECK (unit_price >= 0)) STRICT");
        sql.execute("CREATE TABLE quotations ("
                + "id INTEGER PRIMARY KEY, client_id INTEGER NOT NULL REFERENCES clients(id),"
                + " quotation_date TEXT NOT NULL,"
                + " status TEXT NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED')),"
                + " discount_percent REAL NOT NULL CHECK (discount_percent BETWEEN 0 AND 100)) STRICT");
        sql.execute("CREATE TABLE quotation_items ("
                + "quotation_id INTEGER NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,"
                + " line_number INTEGER NOT NULL CHECK (line_number > 0),"
                + " offering_id INTEGER NOT NULL REFERENCES offerings(id),"
                + " quantity REAL NOT NULL CHECK (quantity > 0),"
                + " unit_price REAL NOT NULL CHECK (unit_price >= 0),"
                + " discount_percent REAL NOT NULL CHECK (discount_percent BETWEEN 0 AND 100),"
                + " PRIMARY KEY (quotation_id, line_number)) STRICT");
        sql.execute("CREATE INDEX quotations_client_id ON quotations(client_id)");
        sql.execute("CREATE INDEX quotation_items_offering_id ON quotation_items(offering_id)");
        createSettingsTable(sql);
    }

    private static void createSettingsTable(Statement sql) throws SQLException {
        sql.execute("CREATE TABLE session_settings (id INTEGER PRIMARY KEY CHECK (id = 1),"
                + " currency TEXT NOT NULL CHECK (currency IN ('USD', 'PYG')),"
                + " base_currency TEXT NOT NULL CHECK (base_currency IN ('USD', 'PYG')),"
                + " pyg_per_usd TEXT) STRICT");
        sql.execute("PRAGMA user_version = " + SCHEMA_VERSION);
    }

    private static void upgradeSettingsTable(Statement sql, int version) throws SQLException {
        if (version == 1) {
            createSettingsTable(sql);
        } else if (version == 2) {
            sql.execute("ALTER TABLE session_settings ADD COLUMN base_currency TEXT NOT NULL DEFAULT 'USD'"
                    + " CHECK (base_currency IN ('USD', 'PYG'))");
            sql.execute("ALTER TABLE session_settings ADD COLUMN pyg_per_usd TEXT");
            sql.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        }
    }

    private static int checkSchema(Statement sql) throws SQLException, IOException {
        try (ResultSet row = sql.executeQuery("PRAGMA application_id")) {
            if (!row.next() || row.getInt(1) != APPLICATION_ID) {
                throw new IOException("Choose a Quotation Builder session database.");
            }
        }
        try (ResultSet row = sql.executeQuery("PRAGMA user_version")) {
            int version = row.next() ? row.getInt(1) : 0;
            if (version < 1 || version > SCHEMA_VERSION) {
                throw new IOException("This version of the app cannot open the selected database version.");
            }
            return version;
        }
    }

    private static CurrencySettings readCurrencySettings(Statement sql, int version) throws SQLException, IOException {
        String columns = version == 2 ? "currency" : "currency, base_currency, pyg_per_usd";
        try (ResultSet row = sql.executeQuery("SELECT " + columns + " FROM session_settings WHERE id = 1")) {
            if (!row.next()) {
                throw new IOException("The session database is missing its currency setting.");
            }
            SessionCurrency display = SessionCurrency.valueOf(row.getString("currency"));
            if (version == 2) {
                return new CurrencySettings(display, display, null);
            }
            SessionCurrency base = SessionCurrency.valueOf(row.getString("base_currency"));
            String rate = row.getString("pyg_per_usd");
            return new CurrencySettings(base, display, rate == null ? null : new BigDecimal(rate));
        }
    }

    private static void writeCurrencySettings(Connection database, CurrencySettings settings) throws SQLException {
        try (PreparedStatement save = database.prepareStatement(
                "INSERT INTO session_settings (id, currency, base_currency, pyg_per_usd) VALUES (1, ?, ?, ?)"
                        + " ON CONFLICT(id) DO UPDATE SET currency = excluded.currency,"
                        + " base_currency = excluded.base_currency, pyg_per_usd = excluded.pyg_per_usd")) {
            save.setString(1, settings.getDisplayCurrency().name());
            save.setString(2, settings.getBaseCurrency().name());
            BigDecimal rate = settings.getGuaraniesPerDollar();
            save.setString(3, rate == null ? null : rate.toPlainString());
            save.executeUpdate();
        }
    }

    private static void writeClients(Connection database, DataStore data) throws SQLException {
        try (PreparedStatement insert = database.prepareStatement(
                "INSERT INTO clients (id, name, email, phone) VALUES (?, ?, ?, ?)")) {
            for (int i = 0; i < data.getClientCount(); i++) {
                Client client = data.getClient(i);
                insert.setInt(1, i + 1);
                insert.setString(2, client.getName());
                insert.setString(3, client.getEmail());
                insert.setString(4, client.getPhone());
                insert.executeUpdate();
            }
        }
    }

    private static void writeOfferings(Connection database, DataStore data) throws SQLException {
        try (PreparedStatement insert = database.prepareStatement(
                "INSERT INTO offerings (id, name, description, unit_price) VALUES (?, ?, ?, ?)")) {
            for (int i = 0; i < data.getOfferingCount(); i++) {
                Offering offering = data.getOffering(i);
                insert.setInt(1, i + 1);
                insert.setString(2, offering.getName());
                insert.setString(3, offering.getDescription());
                insert.setDouble(4, offering.getUnitPrice());
                insert.executeUpdate();
            }
        }
    }

    private static void writeQuotes(Connection database, DataStore data) throws SQLException, IOException {
        try (PreparedStatement quotation = database.prepareStatement(
                "INSERT INTO quotations (id, client_id, quotation_date, status, discount_percent) VALUES (?, ?, ?, ?, ?)");
                PreparedStatement line = database.prepareStatement(
                        "INSERT INTO quotation_items (quotation_id, line_number, offering_id, quantity, unit_price,"
                                + " discount_percent) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (int i = 0; i < data.getQuoteCount(); i++) {
                Quote quote = data.getQuote(i);
                List<QuoteItem> items = quote.getItems();
                if (items.isEmpty()) {
                    throw new IOException("A quotation needs at least one item before saving the session.");
                }

                quotation.setInt(1, i + 1);
                quotation.setInt(2, data.getClientNumber(quote.getClient()));
                quotation.setString(3, quote.getDate().toString());
                quotation.setString(4, quote.getStatus().name());
                quotation.setDouble(5, quote.getDiscountPercent());
                quotation.executeUpdate();

                for (int j = 0; j < items.size(); j++) {
                    QuoteItem item = items.get(j);
                    line.setInt(1, i + 1);
                    line.setInt(2, j + 1);
                    line.setInt(3, data.getOfferingNumber(item.getOffering()));
                    line.setDouble(4, item.getQuantity());
                    line.setDouble(5, item.getUnitPrice());
                    line.setDouble(6, item.getDiscountPercent());
                    line.executeUpdate();
                }
            }
        }
    }

    private static Map<Long, Client> readClients(Statement sql, DataStore data) throws SQLException {
        Map<Long, Client> clients = new HashMap<>();
        try (ResultSet rows = sql.executeQuery("SELECT id, name, email, phone FROM clients ORDER BY id")) {
            while (rows.next()) {
                Client client = new Client(rows.getString("name"), rows.getString("email"), rows.getString("phone"));
                data.addClient(client);
                clients.put(rows.getLong("id"), client);
            }
        }
        return clients;
    }

    private static Map<Long, Offering> readOfferings(Statement sql, DataStore data) throws SQLException {
        Map<Long, Offering> offerings = new HashMap<>();
        try (ResultSet rows = sql.executeQuery("SELECT id, name, description, unit_price FROM offerings ORDER BY id")) {
            while (rows.next()) {
                Offering offering = new Offering(rows.getString("name"), rows.getString("description"),
                        rows.getDouble("unit_price"));
                data.addOffering(offering);
                offerings.put(rows.getLong("id"), offering);
            }
        }
        return offerings;
    }

    private static void readQuotes(Statement sql, DataStore data, Map<Long, Client> clients,
            Map<Long, Offering> offerings) throws SQLException, IOException {
        Map<Long, Quote> quotes = new LinkedHashMap<>();
        try (ResultSet rows = sql.executeQuery(
                "SELECT id, client_id, quotation_date, status, discount_percent FROM quotations ORDER BY id")) {
            while (rows.next()) {
                Quote quote = new Quote(clients.get(rows.getLong("client_id")));
                quote.setDate(LocalDate.parse(rows.getString("quotation_date")));
                quote.setStatus(Quote.Status.valueOf(rows.getString("status")));
                quote.setDiscountPercent(rows.getDouble("discount_percent"));
                quotes.put(rows.getLong("id"), quote);
            }
        }
        try (ResultSet rows = sql.executeQuery(
                "SELECT quotation_id, offering_id, quantity, unit_price, discount_percent"
                        + " FROM quotation_items ORDER BY quotation_id, line_number")) {
            while (rows.next()) {
                Quote quote = quotes.get(rows.getLong("quotation_id"));
                if (quote == null) {
                    throw new IOException("A line item refers to a missing quotation.");
                }
                Offering offering = offerings.get(rows.getLong("offering_id"));
                quote.addItem(new QuoteItem(offering, rows.getDouble("quantity"), rows.getDouble("unit_price"),
                        rows.getDouble("discount_percent")));
            }
        }
        for (Quote quote : quotes.values()) {
            data.addQuote(quote);
        }
    }

    private static IOException databaseError(String operation, SQLException error) {
        return new IOException("Could not " + operation + " the SQLite session. " + error.getMessage(), error);
    }
}
