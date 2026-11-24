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
import model.Quote;
import model.QuoteItem;

/** Builds the receipt text used by the preview and the PDF exporter. */
public final class QuotePdf {
    private static final int TEXT_WIDTH = 82;
    private static final int LINES_PER_PAGE = 50;
    private static final String RULE = "-".repeat(TEXT_WIDTH);
    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

    private QuotePdf() {
    }

    public static String preview(Quote quote, int number, CurrencySettings currency) {
        StringBuilder text = new StringBuilder("Quotation #" + number + "\n\n");
        for (List<String> section : sections(quote, currency)) {
            text.append(String.join("\n", section)).append('\n');
        }
        return text.toString();
    }

    public static void write(Path file, Quote quote, int number, CurrencySettings currency) throws IOException {
        byte[] bytes = pdfBytes("QUOTATION #" + number, paginate(sections(quote, currency)));
        Path target = file.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(target.getParent(), ".quotation-", ".pdf.tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static List<List<String>> sections(Quote quote, CurrencySettings currency) {
        List<List<String>> sections = new ArrayList<>();
        Client client = quote.getClient();
        sections.add(wrap("Date: " + quote.getDate() + "    Status: " + quote.getStatus()
                + "\n\nPrepared for\n" + client.getName()
                + "\nEmail: " + client.getEmailOrDefault() + "\nPhone: " + client.getPhoneOrDefault()
                + "\n\n" + RULE, ""));

        int number = 1;
        for (QuoteItem item : quote.getItems()) {
            List<String> lines = wrap(number++ + ". " + item.getOffering().getName(), "");
            String description = item.getOffering().getDescription();
            if (!description.isEmpty()) {
                lines.addAll(wrap(description, "   "));
            }
            lines.addAll(wrap("Qty / hours: " + number(item.getQuantity()) + "    Unit price: "
                    + currency.format(item.getUnitPrice()) + "    Discount: " + number(item.getDiscountPercent()) + "%", "   "));
            lines.add("   Line total: " + currency.format(item.getLineTotal()));
            lines.add("");
            sections.add(lines);
        }

        double subtotal = quote.getSubtotal();
        double total = quote.getTotal();
        sections.add(List.of(RULE,
                totalLine("Subtotal", currency.format(subtotal)),
                totalLine("Quote discount (" + number(quote.getDiscountPercent()) + "%)", currency.format(subtotal - total)),
                totalLine("TOTAL", currency.format(total))));
        return sections;
    }

    private static String totalLine(String name, String amount) {
        return name + " ".repeat(TEXT_WIDTH - name.length() - amount.length()) + amount;
    }

    // Keep each item together when it fits on a page.
    private static List<List<String>> paginate(List<List<String>> sections) {
        List<List<String>> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        pages.add(page);
        for (List<String> section : sections) {
            if (section.size() <= LINES_PER_PAGE && page.size() + section.size() > LINES_PER_PAGE) {
                page = new ArrayList<>();
                pages.add(page);
            }
            for (String line : section) {
                if (page.size() == LINES_PER_PAGE) {
                    page = new ArrayList<>();
                    pages.add(page);
                }
                page.add(line);
            }
        }
        return pages;
    }

    private static List<String> wrap(String text, String indent) {
        List<String> lines = new ArrayList<>();
        int width = TEXT_WIDTH - indent.length();
        for (String paragraph : text.replace('\t', ' ').split("\\R", -1)) {
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
                lines.add(indent + line.substring(start, end).stripTrailing());
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

    private static void draw(StringBuilder page, String text, int y, int size, String font) {
        String latin = new String(text.getBytes(PDF_CHARSET), StandardCharsets.ISO_8859_1);
        String escaped = latin.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        page.append("BT /").append(font).append(' ').append(size).append(" Tf 48 ").append(y)
                .append(" Td (").append(escaped).append(") Tj ET\n");
    }

    private static byte[] pdfBytes(String title, List<List<String>> pages) {
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < pages.size(); i++) {
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]"
                    + " /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >>"
                    + " /Contents " + (6 + 2 * i) + " 0 R >>");
            StringBuilder content = new StringBuilder();
            draw(content, title, 788, 18, "F2");
            int y = 755;
            for (String line : pages.get(i)) {
                draw(content, line, y, 10, line.startsWith("TOTAL") ? "F2" : "F1");
                y -= 14;
            }
            draw(content, "Page " + (i + 1) + " / " + pages.size(), 30, 9, "F1");
            objects.add("<< /Length " + content.length() + " >>\nstream\n" + content + "endstream");
        }

        // The cross-reference table points to the byte offset of each PDF object.
        StringBuilder file = new StringBuilder("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = file.length();
            file.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefStart = file.length();
        file.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            file.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        file.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xrefStart).append("\n%%EOF\n");
        return file.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
