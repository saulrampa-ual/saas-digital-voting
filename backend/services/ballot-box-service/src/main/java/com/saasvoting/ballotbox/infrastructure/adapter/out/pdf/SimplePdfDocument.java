package com.saasvoting.ballotbox.infrastructure.adapter.out.pdf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal PDF 1.4 writer for the minutes (RF-16, D-40): A4 pages with text in the standard Helvetica and Courier fonts
 * (WinAnsiEncoding, enough for Spanish and English), lines and filled rectangles. No external library and the same
 * input always gives the same bytes, so the SHA-256 of the file can be published.
 */
final class SimplePdfDocument {

    enum Font {
        REGULAR("F1", "Helvetica"), BOLD("F2", "Helvetica-Bold"), MONO("F3", "Courier");

        private final String resource;
        private final String baseFont;

        Font(String resource, String baseFont) {
            this.resource = resource;
            this.baseFont = baseFont;
        }
    }

    static final float PAGE_WIDTH = 595.28f;
    static final float PAGE_HEIGHT = 841.89f;

    /** Glyph widths (1/1000 em) of the standard fonts for the characters 32–126 (Adobe AFM). */
    private static final int[] HELVETICA = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556,
        556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667,
        556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556,
        556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722,
        500, 500, 500, 334, 260, 334, 584};
    private static final int[] HELVETICA_BOLD = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556,
        556, 556, 556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722,
        611, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556,
        611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778,
        556, 556, 500, 389, 280, 389, 584};
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private final List<StringBuilder> pages = new ArrayList<>();
    private final String title;
    private final Instant created;

    SimplePdfDocument(String title, Instant created) {
        this.title = title;
        this.created = created;
    }

    int newPage() {
        pages.add(new StringBuilder());
        return pages.size() - 1;
    }

    int pageCount() {
        return pages.size();
    }

    /** Text with its baseline at (x, y) from the bottom-left corner of the page. */
    void text(int page, float x, float y, Font font, float size, String text, float gray) {
        pages.get(page).append("BT ").append(num(gray)).append(" g /").append(font.resource).append(' ').append(num(size))
                .append(" Tf ").append(num(x)).append(' ').append(num(y)).append(" Td ").append(literal(text))
                .append(" Tj ET\n");
    }

    void line(int page, float x1, float y1, float x2, float y2, float width, float gray) {
        pages.get(page).append(num(gray)).append(" G ").append(num(width)).append(" w ").append(num(x1)).append(' ')
                .append(num(y1)).append(" m ").append(num(x2)).append(' ').append(num(y2)).append(" l S\n");
    }

    void rect(int page, float x, float y, float width, float height, float r, float g, float b) {
        pages.get(page).append(num(r)).append(' ').append(num(g)).append(' ').append(num(b)).append(" rg ")
                .append(num(x)).append(' ').append(num(y)).append(' ').append(num(width)).append(' ').append(num(height))
                .append(" re f\n");
    }

    /** Width of the text in points, with the metrics of the standard font. */
    static float width(Font font, float size, String text) {
        if (font == Font.MONO) {
            return text.length() * 0.6f * size;
        }
        int[] widths = font == Font.BOLD ? HELVETICA_BOLD : HELVETICA;
        float total = 0;
        for (char c : text.toCharArray()) {
            char base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).charAt(0);
            total += base >= 32 && base <= 126 ? widths[base - 32] : 556;
        }
        return total * size / 1000f;
    }

    /** Splits the text into lines that fit in {@code maxWidth}; a word longer than the line is cut. */
    static List<String> wrap(Font font, float size, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (width(font, size, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
            }
            String rest = word;
            while (width(font, size, rest) > maxWidth && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && width(font, size, rest.substring(0, cut)) > maxWidth) {
                    cut--;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            current.append(rest);
        }
        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    byte[] toBytes() {
        List<byte[]> objects = new ArrayList<>();
        int fontBase = 3;
        int firstPage = fontBase + Font.values().length;
        int infoObject = firstPage + pages.size() * 2;
        // 1: catalog, 2: page tree
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(firstPage + i * 2).append(" 0 R ");
        }
        objects.add(ascii("<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pages.size() + " >>"));
        for (Font font : Font.values()) {
            objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /" + font.baseFont
                    + " /Encoding /WinAnsiEncoding >>"));
        }
        StringBuilder fonts = new StringBuilder();
        for (Font font : Font.values()) {
            fonts.append('/').append(font.resource).append(' ').append(fontBase + font.ordinal()).append(" 0 R ");
        }
        for (int i = 0; i < pages.size(); i++) {
            int contents = firstPage + i * 2 + 1;
            objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + num(PAGE_WIDTH) + " " + num(PAGE_HEIGHT)
                    + "] /Resources << /Font << " + fonts.toString().trim() + " >> >> /Contents " + contents + " 0 R >>"));
            byte[] stream = pages.get(i).toString().getBytes(WIN_ANSI);
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            content.writeBytes(ascii("<< /Length " + stream.length + " >>\nstream\n"));
            content.writeBytes(stream);
            content.writeBytes(ascii("\nendstream"));
            objects.add(content.toByteArray());
        }
        String date = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(ZoneOffset.UTC).format(created);
        ByteArrayOutputStream info = new ByteArrayOutputStream();
        info.writeBytes(ascii("<< /Title "));
        info.writeBytes(literal(title).getBytes(WIN_ANSI));
        info.writeBytes(ascii(" /Producer (VeriVoto) /CreationDate (D:" + date + "Z) >>"));
        objects.add(info.toByteArray());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(ascii("%PDF-1.4\n"));
        out.writeBytes(new byte[] {'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.writeBytes(ascii((i + 1) + " 0 obj\n"));
            out.writeBytes(objects.get(i));
            out.writeBytes(ascii("\nendobj\n"));
        }
        int xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R /Info ").append(infoObject)
                .append(" 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.writeBytes(ascii(table.toString()));
        return out.toByteArray();
    }

    /** PDF string literal; characters outside WinAnsi become «?», special ones are escaped. */
    private static String literal(String text) {
        CharsetEncoder encoder = WIN_ANSI.newEncoder();
        StringBuilder out = new StringBuilder("(");
        for (char c : text.toCharArray()) {
            char safe = encoder.canEncode(c) && c >= 32 ? c : (c == '\t' ? ' ' : '?');
            if (safe == '(' || safe == ')' || safe == '\\') {
                out.append('\\');
            }
            out.append(safe);
        }
        return out.append(')').toString();
    }

    private static String num(float value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.endsWith(".00") ? text.substring(0, text.length() - 3) : text;
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
