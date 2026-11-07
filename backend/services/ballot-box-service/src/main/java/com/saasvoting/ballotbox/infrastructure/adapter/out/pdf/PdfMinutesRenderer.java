package com.saasvoting.ballotbox.infrastructure.adapter.out.pdf;

import com.saasvoting.ballotbox.application.port.out.MinutesRenderer;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.MinutesContent;
import com.saasvoting.ballotbox.infrastructure.adapter.out.pdf.SimplePdfDocument.Font;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Layout of the minutes (RF-16, D-40) on A4: header, process data, result per option with bars, both hashes, how to
 * verify, signatures and a footer with the page number. Spanish or English; dates in the zone of the organization.
 */
public class PdfMinutesRenderer implements MinutesRenderer {

    private static final float MARGIN = 56;
    private static final float CONTENT = SimplePdfDocument.PAGE_WIDTH - 2 * MARGIN;
    private static final float BOTTOM = 90;
    private static final float INK = 0.11f;
    private static final float MUTED = 0.38f;

    private static final Map<Language, Map<String, String>> LABELS = Map.of(
            Language.ES, Map.ofEntries(
                    Map.entry("title", "ACTA DE RESULTADOS"),
                    Map.entry("subtitle", "Organización: %s · Proceso n.º %d"),
                    Map.entry("data", "Datos del proceso"),
                    Map.entry("question", "Pregunta"),
                    Map.entry("opens", "Apertura"),
                    Map.entry("closes", "Cierre"),
                    Map.entry("tallied", "Escrutinio"),
                    Map.entry("census", "Personas en el censo"),
                    Map.entry("votes", "Votos emitidos"),
                    Map.entry("turnout", "Participación"),
                    Map.entry("result", "Resultado"),
                    Map.entry("option", "Opción"),
                    Map.entry("count", "Votos"),
                    Map.entry("pct", "%"),
                    Map.entry("blank", "Voto en blanco"),
                    Map.entry("total", "Total"),
                    Map.entry("hashes", "Huellas de verificación"),
                    Map.entry("finalHash", "Huella final de la urna (evento de cierre)"),
                    Map.entry("resultHash", "Huella del resultado"),
                    Map.entry("verify", "Cómo verificar este resultado"),
                    Map.entry("verifyText", "La urna publica los votos contabilizados, cada uno solo con un identificador "
                            + "aleatorio y la opción elegida, sin identidad ni recibo. Con esa lista cualquiera puede "
                            + "recalcular el recuento y la huella del resultado, que debe coincidir con la de esta acta; "
                            + "la huella final de la urna debe coincidir con la publicada al cierre."),
                    Map.entry("verifyPublic", "Los votos contabilizados están disponibles en el enlace público de "
                            + "resultados del proceso."),
                    Map.entry("verifyCensus", "Los votos contabilizados están disponibles para la administración y la "
                            + "auditoría de la organización."),
                    Map.entry("signatures", "Firmas"),
                    Map.entry("sign", "Nombre, cargo, firma y fecha"),
                    Map.entry("footer", "Generada el %s por VeriVoto · Página %d de %d"),
                    Map.entry("zone", "hora de %s")),
            Language.EN, Map.ofEntries(
                    Map.entry("title", "MINUTES OF THE RESULT"),
                    Map.entry("subtitle", "Organisation: %s · Process no. %d"),
                    Map.entry("data", "Process details"),
                    Map.entry("question", "Question"),
                    Map.entry("opens", "Opening"),
                    Map.entry("closes", "Closing"),
                    Map.entry("tallied", "Tally"),
                    Map.entry("census", "People in the census"),
                    Map.entry("votes", "Votes cast"),
                    Map.entry("turnout", "Turnout"),
                    Map.entry("result", "Result"),
                    Map.entry("option", "Option"),
                    Map.entry("count", "Votes"),
                    Map.entry("pct", "%"),
                    Map.entry("blank", "Blank vote"),
                    Map.entry("total", "Total"),
                    Map.entry("hashes", "Verification hashes"),
                    Map.entry("finalHash", "Final hash of the ballot box (closing event)"),
                    Map.entry("resultHash", "Result hash"),
                    Map.entry("verify", "How to verify this result"),
                    Map.entry("verifyText", "The ballot box publishes the counted votes, each with only a random "
                            + "identifier and the chosen option, without identity or receipt. With that list anyone can "
                            + "recompute the count and the result hash, which must match the one in these minutes; the "
                            + "final hash of the ballot box must match the one published at closing."),
                    Map.entry("verifyPublic", "The counted votes are available at the public results link of the "
                            + "process."),
                    Map.entry("verifyCensus", "The counted votes are available to the administration and the audit of "
                            + "the organisation."),
                    Map.entry("signatures", "Signatures"),
                    Map.entry("sign", "Name, role, signature and date"),
                    Map.entry("footer", "Generated on %s by VeriVoto · Page %d of %d"),
                    Map.entry("zone", "%s time")));

    private final ZoneId zone;

    public PdfMinutesRenderer(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public byte[] render(MinutesContent content) {
        return new Layout(content).build();
    }

    /** One rendering: keeps the current page and the vertical position, and starts a new page when needed. */
    private final class Layout {

        private final MinutesContent c;
        private final Map<String, String> label;
        private final Locale locale;
        private final SimplePdfDocument pdf;
        private int page;
        private float y;

        Layout(MinutesContent content) {
            this.c = content;
            this.label = LABELS.get(content.language());
            this.locale = content.language() == Language.ES ? Locale.forLanguageTag("es-ES") : Locale.UK;
            this.pdf = new SimplePdfDocument(label.get("title") + " - " + content.title(), content.generatedAt());
            this.page = pdf.newPage();
            this.y = SimplePdfDocument.PAGE_HEIGHT - MARGIN;
        }

        byte[] build() {
            header();
            section(label.get("data"));
            field(label.get("question"), c.question());
            field(label.get("opens"), date(c.opensAt()));
            field(label.get("closes"), date(c.closesAt()));
            field(label.get("tallied"), date(c.talliedAt()));
            field(label.get("census"), integer(c.eligibleVoters()));
            field(label.get("votes"), integer(c.totalVotes()));
            field(label.get("turnout"), percent(c.participation()));
            resultTable();
            section(label.get("hashes"));
            hash(label.get("finalHash"), c.finalHash());
            hash(label.get("resultHash"), c.resultHash());
            section(label.get("verify"));
            paragraph(label.get("verifyText") + " " + label.get(c.publicLink() ? "verifyPublic" : "verifyCensus"));
            signatures();
            footers();
            return pdf.toBytes();
        }

        private void header() {
            pdf.rect(page, 0, SimplePdfDocument.PAGE_HEIGHT - 8, SimplePdfDocument.PAGE_WIDTH, 8, 0.08f, 0.21f, 0.36f);
            pdf.text(page, MARGIN, y - 4, Font.BOLD, 10, "VeriVoto", MUTED);
            y -= 30;
            pdf.text(page, MARGIN, y, Font.BOLD, 18, label.get("title"), INK);
            y -= 24;
            for (String line : SimplePdfDocument.wrap(Font.BOLD, 14, c.title(), CONTENT)) {
                need(18);
                pdf.text(page, MARGIN, y, Font.BOLD, 14, line, INK);
                y -= 18;
            }
            pdf.text(page, MARGIN, y, Font.REGULAR, 10, label.get("subtitle").formatted(c.organizationId(), c.processId()),
                    MUTED);
            y -= 14;
        }

        private void section(String title) {
            need(48);
            y -= 14;
            pdf.text(page, MARGIN, y, Font.BOLD, 12, title, INK);
            y -= 6;
            pdf.line(page, MARGIN, y, MARGIN + CONTENT, y, 0.6f, 0.8f);
            y -= 16;
        }

        private void field(String name, String value) {
            float labelWidth = 150;
            List<String> lines = SimplePdfDocument.wrap(Font.REGULAR, 10, value, CONTENT - labelWidth);
            need(14 * lines.size());
            pdf.text(page, MARGIN, y, Font.REGULAR, 10, name, MUTED);
            for (String line : lines) {
                pdf.text(page, MARGIN + labelWidth, y, Font.REGULAR, 10, line, INK);
                y -= 14;
            }
        }

        private void resultTable() {
            section(label.get("result"));
            float votesRight = MARGIN + CONTENT - 70;
            float pctRight = MARGIN + CONTENT;
            float textWidth = CONTENT - 150;
            tableHeader(votesRight, pctRight);
            for (MinutesContent.Row row : c.rows()) {
                resultRow(row.text(), row.votes(), row.percentage(), textWidth, votesRight, pctRight, false);
            }
            if (c.blankVoteEnabled()) {
                resultRow(label.get("blank"), c.blankVotes(), c.blankPercentage(), textWidth, votesRight, pctRight, false);
            }
            need(20);
            pdf.line(page, MARGIN, y + 8, MARGIN + CONTENT, y + 8, 0.6f, 0.75f);
            pdf.text(page, MARGIN, y - 4, Font.BOLD, 10, label.get("total"), INK);
            rightAt(votesRight, y - 4, Font.BOLD, 10, integer(c.totalVotes()), INK);
            rightAt(pctRight, y - 4, Font.BOLD, 10, c.totalVotes() == 0 ? percent(BigDecimal.ZERO.setScale(2))
                    : percent(new BigDecimal("100.00")), INK);
            y -= 20;
        }

        /** Column headings of the result table; repeated at the top of every page the table continues on. */
        private void tableHeader(float votesRight, float pctRight) {
            pdf.text(page, MARGIN, y, Font.BOLD, 9, label.get("option"), MUTED);
            right(votesRight, Font.BOLD, 9, label.get("count"), MUTED);
            right(pctRight, Font.BOLD, 9, label.get("pct"), MUTED);
            y -= 16;
        }

        private void resultRow(String text, int votes, BigDecimal percentage, float textWidth, float votesRight,
                float pctRight, boolean bold) {
            List<String> lines = SimplePdfDocument.wrap(Font.REGULAR, 10, text, textWidth);
            int before = page;
            need(14 * lines.size() + 12);
            if (page != before) {
                tableHeader(votesRight, pctRight);
            }
            float baseline = y;
            for (String line : lines) {
                pdf.text(page, MARGIN, y, bold ? Font.BOLD : Font.REGULAR, 10, line, INK);
                y -= 14;
            }
            rightAt(votesRight, baseline, Font.REGULAR, 10, integer(votes), INK);
            rightAt(pctRight, baseline, Font.REGULAR, 10, percent(percentage), INK);
            // Bar of the share of the votes, one hue (the numbers carry the meaning).
            pdf.rect(page, MARGIN, y + 6, textWidth, 4, 0.89f, 0.91f, 0.94f);
            float filled = percentage.floatValue() / 100f * textWidth;
            if (filled > 0) {
                pdf.rect(page, MARGIN, y + 6, Math.max(1, filled), 4, 0.08f, 0.21f, 0.36f);
            }
            y -= 10;
        }

        private void hash(String name, String value) {
            need(32);
            pdf.text(page, MARGIN, y, Font.REGULAR, 10, name, MUTED);
            y -= 13;
            pdf.text(page, MARGIN, y, Font.MONO, 8.5f, value == null ? "-" : value, INK);
            y -= 18;
        }

        private void paragraph(String text) {
            for (String line : SimplePdfDocument.wrap(Font.REGULAR, 10, text, CONTENT)) {
                need(14);
                pdf.text(page, MARGIN, y, Font.REGULAR, 10, line, INK);
                y -= 14;
            }
        }

        private void signatures() {
            need(110);
            section(label.get("signatures"));
            y -= 40;
            float half = (CONTENT - 30) / 2;
            pdf.line(page, MARGIN, y, MARGIN + half, y, 0.6f, 0.4f);
            pdf.line(page, MARGIN + half + 30, y, MARGIN + CONTENT, y, 0.6f, 0.4f);
            y -= 12;
            pdf.text(page, MARGIN, y, Font.REGULAR, 8.5f, label.get("sign"), MUTED);
            pdf.text(page, MARGIN + half + 30, y, Font.REGULAR, 8.5f, label.get("sign"), MUTED);
            y -= 14;
        }

        private void footers() {
            int total = pdf.pageCount();
            for (int i = 0; i < total; i++) {
                pdf.line(i, MARGIN, 52, MARGIN + CONTENT, 52, 0.5f, 0.8f);
                pdf.text(i, MARGIN, 38, Font.REGULAR, 8.5f,
                        label.get("footer").formatted(date(c.generatedAt()), i + 1, total), MUTED);
            }
        }

        /** Starts a new page if {@code height} points do not fit above the footer. */
        private void need(float height) {
            if (y - height < BOTTOM) {
                page = pdf.newPage();
                y = SimplePdfDocument.PAGE_HEIGHT - MARGIN;
            }
        }

        private void right(float x, Font font, float size, String text, float gray) {
            rightAt(x, y, font, size, text, gray);
        }

        private void rightAt(float x, float baseline, Font font, float size, String text, float gray) {
            pdf.text(page, x - SimplePdfDocument.width(font, size, text), baseline, font, size, text, gray);
        }

        private String date(Instant instant) {
            String pattern = c.language() == Language.ES ? "dd/MM/yyyy HH:mm" : "d MMM yyyy, HH:mm";
            return DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(instant) + " ("
                    + label.get("zone").formatted(zone.getId()) + ")";
        }

        private String integer(int value) {
            return NumberFormat.getIntegerInstance(locale).format(value);
        }

        private String percent(BigDecimal value) {
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(value) + " %";
        }
    }
}
