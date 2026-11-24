package export;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Client;
import model.CurrencySettings;
import model.Offering;
import model.Quote;
import model.QuoteItem;

/**
 * Writes a quotation to a PDF file without any external library.
 *
 * Uses the standard Courier fonts and a cross-reference table for the page objects.
 */
public final class QuotePdf {
    private static final int LEFT = 48;
    private static final int RIGHT = 547;
    private static final int TOP = 788;
    private static final int BOTTOM = 50;
    private static final int LINE_HEIGHT = 14;
    private static final int FONT_SIZE = 10;
    private static final int TITLE_HEIGHT = 23;
    private static final int METADATA_HEIGHT = 28;
    private static final int TABLE_HEADER_HEIGHT = LINE_HEIGHT + 7;
    private static final int ITEM_GAP = 4;
    private static final int ITEM_LINES_PER_PAGE =
            (TOP - BOTTOM - TITLE_HEIGHT - METADATA_HEIGHT - TABLE_HEADER_HEIGHT - ITEM_GAP) / LINE_HEIGHT;

    private static final int OFFERING_LEFT = 74;
    private static final int QTY_LEFT = 226;
    private static final int PRICE_LEFT = 284;
    private static final int DISCOUNT_LEFT = 388;
    private static final int TOTAL_LEFT = 448;
    private static final int COLUMN_GAP = 6;
    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

    private final Quote quote;
    private final int number;
    private final CurrencySettings currency;
    private final List<StringBuilder> pages = new ArrayList<>();
    private int y;

    private QuotePdf(Quote quote, int number, CurrencySettings currency) {
        this.quote = quote;
        this.number = number;
        this.currency = currency;
    }

    public static void write(Path file, Quote quote, int number, CurrencySettings currency) throws IOException {
        QuotePdf pdf = new QuotePdf(quote, number, currency);
        pdf.addContent();
        Path target = file.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(target.getParent(), ".quotation-", ".pdf.tmp");
        try {
            Files.write(temporary, pdf.toPdfBytes());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void addContent() {
        Client client = quote.getClient();
        startPage(false);
        addLine("Prepared for", LEFT, FONT_SIZE, true);
        addContact(client.getName());
        addContact("Email: " + client.getEmailOrDefault());
        addContact("Phone: " + client.getPhoneOrDefault());
        y -= 12;
        ensureSpace(4 * LINE_HEIGHT, false);
        addTableHeader();

        int line = 1;
        for (QuoteItem item : quote.getItems()) {
            addItem(item, line++);
        }
        addTotals();
        for (int i = 0; i < pages.size(); i++) {
            drawText(pages.get(i), LEFT, 30, "Page " + (i + 1) + " / " + pages.size(), 9, false);
        }
    }

    private void startPage(boolean withTableHeader) {
        pages.add(new StringBuilder());
        y = TOP;
        drawText(page(), LEFT, y, "QUOTATION #" + number, 18, true);
        y -= TITLE_HEIGHT;
        drawText(page(), LEFT, y, "Date: " + quote.getDate() + "    Status: " + quote.getStatus(), FONT_SIZE, false);
        y -= METADATA_HEIGHT;
        if (withTableHeader) {
            addTableHeader();
        }
    }

    private void addContact(String text) {
        for (String line : wrap(text, (RIGHT - LEFT) / 6)) {
            ensureSpace(LINE_HEIGHT, false);
            addLine(line, LEFT, FONT_SIZE, false);
        }
    }

    private void addTableHeader() {
        fillBand(LEFT);
        drawText(page(), LEFT + 3, y, "No", FONT_SIZE, true);
        drawText(page(), OFFERING_LEFT, y, "Offering", FONT_SIZE, true);
        drawRight("Qty", QTY_LEFT, PRICE_LEFT - COLUMN_GAP, true);
        drawRight("Unit price", PRICE_LEFT, DISCOUNT_LEFT - COLUMN_GAP, true);
        drawRight("Disc.", DISCOUNT_LEFT, TOTAL_LEFT - COLUMN_GAP, true);
        drawRight("Line total", TOTAL_LEFT, RIGHT - COLUMN_GAP, true);
        y -= TABLE_HEADER_HEIGHT;
    }

    private void addItem(QuoteItem item, int number) {
        Offering offering = item.getOffering();
        int textWidth = QTY_LEFT - OFFERING_LEFT - COLUMN_GAP;
        List<String> name = wrap(offering.getName(), textWidth / 6);
        String description = offering.getDescription();
        List<String> details = description.isEmpty() ? List.of() : wrap(description, (int) (textWidth / 5.4));

        int lineCount = name.size() + details.size();
        int linesToReserve = lineCount <= ITEM_LINES_PER_PAGE ? lineCount : 2;
        ensureSpace(linesToReserve * LINE_HEIGHT + ITEM_GAP, true);
        drawRight(Integer.toString(number), LEFT, OFFERING_LEFT - COLUMN_GAP, false);
        drawText(page(), OFFERING_LEFT, y, name.get(0), FONT_SIZE, true);
        drawRight(number(item.getQuantity()), QTY_LEFT, PRICE_LEFT - COLUMN_GAP, false);
        drawRight(currency.format(item.getUnitPrice()), PRICE_LEFT, DISCOUNT_LEFT - COLUMN_GAP, false);
        drawRight(number(item.getDiscountPercent()) + "%", DISCOUNT_LEFT, TOTAL_LEFT - COLUMN_GAP, false);
        drawRight(currency.format(item.getLineTotal()), TOTAL_LEFT, RIGHT - COLUMN_GAP, false);
        y -= LINE_HEIGHT;
        for (int i = 1; i < name.size(); i++) {
            addItemLine(name.get(i), number, FONT_SIZE, true);
        }
        for (String line : details) {
            addItemLine(line, number, 9, false);
        }
        y -= ITEM_GAP;
    }

    private void addTotals() {
        ensureSpace(4 * LINE_HEIGHT, false);
        y -= 8;
        double subtotal = quote.getSubtotal();
        double total = quote.getTotal();
        addTotal("Subtotal", subtotal, false);
        addTotal("Quote discount (" + number(quote.getDiscountPercent()) + "%)", subtotal - total, false);
        fillBand(PRICE_LEFT - 8);
        addTotal("TOTAL", total, true);
    }

    private void addTotal(String label, double amount, boolean bold) {
        drawText(page(), PRICE_LEFT - 2, y, label, fitSize(label, TOTAL_LEFT - PRICE_LEFT - COLUMN_GAP), bold);
        drawRight(currency.format(amount), TOTAL_LEFT, RIGHT - COLUMN_GAP, bold);
        y -= LINE_HEIGHT + 5;
    }

    private void addItemLine(String text, int number, int size, boolean bold) {
        if (y - LINE_HEIGHT < BOTTOM) {
            startPage(true);
            addLine("Item " + number + " (continued)", OFFERING_LEFT, 9, true);
        }
        addLine(text, OFFERING_LEFT, size, bold);
    }

    private void addLine(String text, int x, int size, boolean bold) {
        drawText(page(), x, y, text, size, bold);
        y -= LINE_HEIGHT;
    }

    private void ensureSpace(int height, boolean inTable) {
        if (y - height < BOTTOM) {
            startPage(inTable);
        }
    }

    private void fillBand(int left) {
        page().append("q 0.88 0.92 0.96 rg ").append(left).append(' ').append(y - 5).append(' ')
                .append(RIGHT - left).append(" 18 re f Q\n");
    }

    private StringBuilder page() {
        return pages.get(pages.size() - 1);
    }

    private void drawRight(String text, int left, int right, boolean bold) {
        int size = fitSize(text, right - left);
        drawText(page(), right - size * text.length() * 0.6, y, text, size, bold);
    }

    private static int fitSize(String text, int width) {
        return Math.min(FONT_SIZE, Math.max(1, (int) (width / (text.length() * 0.6))));
    }

    private static void drawText(StringBuilder page, double x, int y, String text, int size, boolean bold) {
        page.append("BT /").append(bold ? "F2" : "F1").append(' ').append(size).append(" Tf ")
                .append(String.format(Locale.ROOT, "%.2f", x)).append(' ').append(y).append(" Td (")
                .append(escape(text)).append(") Tj ET\n");
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ').split("\n", -1)) {
            String line = paragraph.strip();
            int start = 0;
            do {
                int end = start + Math.min(width, line.length() - start);
                if (end < line.length()) {
                    int space = end;
                    while (space > start && !Character.isWhitespace(line.charAt(space))) {
                        space--;
                    }
                    if (space > start) {
                        end = space;
                    }
                    if (Character.isHighSurrogate(line.charAt(end - 1)) && Character.isLowSurrogate(line.charAt(end))) {
                        end--;
                    }
                }
                lines.add(line.substring(start, end).stripTrailing());
                start = end;
                while (start < line.length() && Character.isWhitespace(line.charAt(start))) {
                    start++;
                }
            } while (start < line.length());
        }
        return lines;
    }

    private static String number(double value) {
        String text = Double.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static String escape(String text) {
        String latin = new String(text.getBytes(PDF_CHARSET), StandardCharsets.ISO_8859_1);
        return latin.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private byte[] toPdfBytes() {
        int pageCount = pages.size();
        List<String> objects = new ArrayList<>();

        // Object 1: the catalog (entry point). Object 2: the list of pages.
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");   // page objects are numbered 5, 7, 9, ...
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");

        // Objects 3 and 4: the two fonts (regular and bold) built into every PDF reader.
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>");

        // For each page: one object describing the page, one object holding its drawing commands.
        for (int i = 0; i < pageCount; i++) {
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]"
                    + " /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >>"
                    + " /Contents " + (6 + 2 * i) + " 0 R >>");
            String content = pages.get(i).toString();
            objects.add("<< /Length " + content.length() + " >>\nstream\n" + content + "endstream");
        }

        // Write header, every object (remembering where each starts), then the xref table.
        StringBuilder file = new StringBuilder("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            starts.add(file.length());
            file.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefStart = file.length();
        file.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int start : starts) {
            file.append(String.format(Locale.ROOT, "%010d 00000 n \n", start));
        }
        file.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
            .append("startxref\n").append(xrefStart).append("\n%%EOF\n");

        return file.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
