package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Renders the Anexa 1 form (HG 856/2002) — <em>fişa de evidenţă a gestiunii deşeurilor</em> — one
 * page per waste code per work point, laid out after the filled sheets received from the
 * specialist: the title "Evidenţa gestiunii deşeurilor generate {year}", an identification header,
 * then the four chapters, each with twelve rows and a TOTAL AN line.
 *
 * <p>Reference: {@code documente oficiale/deseuri generate_Cluj_2025_Iuhos Lorena.pdf} plus the ten
 * filled workbooks. This is the document the whole generator module has been building towards; the
 * quantities are in kilograms, as every one of those sheets declares in its header.
 *
 * <p><b>One deliberate departure from the models.</b> They head chapters 3 and 4 with "conform
 * Anexei 3 / Anexei 2 din Legea 211/2011". That act was repealed by OUG 92/2021, whose annexes
 * carry the same numbers and the same operation lists, so the reference is updated rather than
 * reproduced — printing a repealed act on a form filed with the authority is the kind of detail an
 * inspection notices. Flagged for the specialist in docs/status.md.
 *
 * <p>Diacritics go through Cp1250 for the same reason as {@link Anexa3FormGenerator}: Cp1252 has no
 * ă/ş/ţ and would drop them.
 */
@Component
public class Anexa1FormGenerator {

    private static final String[] MONTHS = {
            "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"
    };

    private static final Color BAND = new Color(0x22, 0x22, 0x22);

    private final Font title;
    private final Font header;
    private final Font headerValue;
    private final Font band;
    private final Font columnHead;
    private final Font body;
    private final Font bodyBold;
    private final Font note;

    public Anexa1FormGenerator() {
        BaseFont plain = centralEuropean(false);
        BaseFont bold = centralEuropean(true);
        // Tuned so a whole sheet — header plus four chapters plus the notes — lands on one page,
        // the way every filled model does. A sheet that spills is a sheet nobody can file.
        this.title = new Font(bold, 10f);
        this.header = new Font(plain, 7.5f);
        this.headerValue = new Font(bold, 7.5f);
        this.band = new Font(bold, 7f, Font.NORMAL, Color.WHITE);
        this.columnHead = new Font(bold, 6f);
        this.body = new Font(plain, 6f);
        this.bodyBold = new Font(bold, 6f);
        this.note = new Font(plain, 5f);
    }

    public byte[] render(List<Anexa1Sheet> sheets) {
        Document doc = new Document(PageSize.A4, 24, 24, 20, 20);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (int i = 0; i < sheets.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                addSheet(doc, sheets.get(i));
            }
            if (sheets.isEmpty()) {
                doc.add(new Paragraph(cp1250(
                        "Nicio evidenţă pentru anul şi punctul de lucru alese."), body));
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the Anexa 1 sheet", ex);
        }
    }

    private void addSheet(Document doc, Anexa1Sheet sheet) {
        doc.add(documentTitle(sheet));
        doc.add(identification(sheet));
        doc.add(chapterOne(sheet));
        doc.add(chapterTwo(sheet));
        doc.add(chapterThree(sheet));
        doc.add(chapterFour(sheet));
        doc.add(notes());
    }

    /**
     * The document's own title, above everything else.
     *
     * <p>Verbatim from the filled workbooks, which write it with the year attached — "Evidenta
     * gestiunii deseurilor generate 2024". Six of the received files carry it in exactly that form
     * (Cluj and Timişoara, 2022 through 2024), and the seventh, the blank template, has it with the
     * year left as "20..". So it is a yearly document, not a monthly one: the sheet under it has
     * twelve rows and a TOTAL AN, and the header's own rubric is "Anul", never "Luna".
     *
     * <p><b>A second deliberate departure from the models.</b> In those workbooks the title sits on
     * the summary sheet ({@code raportare deseuri generate}); the per-code sheets start straight at
     * "Agentul economic:". We print it on every sheet instead, because the meeting note ties the
     * title to the control dossier and a twenty-page PDF with no title on its pages is harder to
     * hand to an inspector. Flagged for the specialist as question P in
     * docs/intrebari-specialist.md; it is one line to move if she wants it only on the summary.
     */
    private Paragraph documentTitle(Anexa1Sheet s) {
        Paragraph p = new Paragraph(
                cp1250("Evidenţa gestiunii deşeurilor generate " + s.year()), title);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4f);
        return p;
    }

    // --- the identification block above chapter 1 ---

    private PdfPTable identification(Anexa1Sheet s) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(62);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        widths(t, 34, 66);
        t.getDefaultCell().setBorder(0);
        t.getDefaultCell().setPadding(1f);

        headerLine(t, "Agentul economic:", s.companyName());
        headerLine(t, "Anul:", String.valueOf(s.year()));
        headerLine(t, "Punct de lucru:", s.workPointName());
        headerLine(t, "Tipul de deşeu:", s.wasteCodeName());
        headerLine(t, "Cod deşeu:", s.wasteCode());
        headerLine(t, "Starea fizică:", s.physicalState());
        headerLine(t, "Unitatea de măsură:", "kg");
        headerLine(t, "Stoc/kg:", kg(s.openingStock()));
        t.setSpacingAfter(2f);
        return t;
    }

    private void headerLine(PdfPTable t, String label, String value) {
        t.addCell(new Phrase(cp1250(label), header));
        t.addCell(new Phrase(cp1250(value == null ? "" : value), headerValue));
    }

    // --- chapter 1: generarea ---

    private PdfPTable chapterOne(Anexa1Sheet s) {
        PdfPTable t = table(new float[]{8, 16, 19, 19, 19, 19});
        bandRow(t, "1. GENERAREA DEŞEURILOR", 6);

        // Three header rows, as the form draws them: Nr. crt. and Luna run down all three,
        // "Generate" down the last two, and only the three "din care" columns sit on the bottom.
        head(t, "Nr. crt.", 1, 3);
        head(t, "Luna", 1, 3);
        headSpan(t, "Cantitatea de deşeuri", 4);
        head(t, "Generate", 1, 2);
        headSpan(t, "din care:", 3);
        head(t, "valorificată", 1, 1);
        head(t, "eliminată final", 1, 1);
        head(t, "rămasă în stoc", 1, 1);

        BigDecimal g = BigDecimal.ZERO, r = BigDecimal.ZERO, d = BigDecimal.ZERO;
        for (Anexa1Sheet.Anexa1MonthRow row : s.rows()) {
            cell(t, String.valueOf(row.month()), Element.ALIGN_CENTER);
            cell(t, MONTHS[row.month() - 1], Element.ALIGN_LEFT);
            num(t, row.generated());
            num(t, row.recovered());
            num(t, row.disposed());
            num(t, row.closingStock());
            g = g.add(row.generated());
            r = r.add(row.recovered());
            d = d.add(row.disposed());
        }
        totalCell(t, 1);
        cellBold(t, "TOTAL AN", Element.ALIGN_LEFT);
        numBold(t, g);
        numBold(t, r);
        numBold(t, d);
        // The stock is not summed — it is a running balance, and adding the twelve values would
        // produce a number that means nothing. What goes here is December's closing stock, which
        // is also the year's. That is what the filled sheets do: of the 33 sheets in the corpus,
        // 28 carry December's figure and none leaves the cell empty. The five that differ hold
        // "generat − valorificat", which coincides with December whenever the year opened at zero;
        // the one sheet where the two readings disagree — a year opening on 50.582 kg of 19 12 12
        // — writes December. We printed an empty cell until 24.08.2026, which is the only thing
        // no model does.
        numBold(t, s.rows().get(s.rows().size() - 1).closingStock());
        return t;
    }

    // --- chapter 2: stocarea provizorie, tratarea şi transportul ---

    private PdfPTable chapterTwo(Anexa1Sheet s) {
        PdfPTable t = table(new float[]{6, 12, 12, 11, 9, 11, 9, 8, 10, 12});
        bandRow(t, "2. STOCAREA PROVIZORIE, TRATAREA ŞI TRANSPORTUL DEŞEURILOR", 10);

        head(t, "Nr. crt.", 1, 2);
        head(t, "Luna", 1, 2);
        head(t, "Secţia", 1, 2);
        headSpan(t, "Stocare", 2);
        headSpan(t, "Tratare", 3);
        headSpan(t, "Transport", 2);
        head(t, "Cant.", 1, 1);
        head(t, "Tipul 1)", 1, 1);
        head(t, "Cant.", 1, 1);
        head(t, "Modul 2)", 1, 1);
        head(t, "Scopul 3)", 1, 1);
        head(t, "Mijlocul 4)", 1, 1);
        head(t, "Destinaţia 5)", 1, 1);

        BigDecimal stored = BigDecimal.ZERO;
        for (Anexa1Sheet.Anexa1MonthRow row : s.rows()) {
            cell(t, String.valueOf(row.month()), Element.ALIGN_CENTER);
            cell(t, MONTHS[row.month() - 1], Element.ALIGN_LEFT);
            cell(t, row.section(), Element.ALIGN_LEFT);
            num(t, row.storedQuantity());
            cell(t, row.storageType(), Element.ALIGN_CENTER);
            num(t, row.treatedQuantity());
            cell(t, row.treatmentMethod(), Element.ALIGN_CENTER);
            cell(t, row.purpose(), Element.ALIGN_CENTER);
            cell(t, row.transportMeans(), Element.ALIGN_CENTER);
            cell(t, row.destination(), Element.ALIGN_CENTER);
            stored = stored.add(row.storedQuantity());
        }
        totalCell(t, 1);
        cellBold(t, "TOTAL AN", Element.ALIGN_LEFT);
        cell(t, "", Element.ALIGN_LEFT);
        numBold(t, stored);
        // Six empty cells, not five: Tipul, Cant., Modul, Scopul, Mijlocul, Destinaţia. A row that
        // is one cell short is silently dropped by PdfPTable, which is how this went missing once.
        for (int i = 0; i < 6; i++) {
            cell(t, "", Element.ALIGN_CENTER);
        }
        return t;
    }

    // --- chapters 3 and 4: the same shape, one for recovery and one for disposal ---

    private PdfPTable chapterThree(Anexa1Sheet s) {
        return treatmentChapter(s, true);
    }

    private PdfPTable chapterFour(Anexa1Sheet s) {
        return treatmentChapter(s, false);
    }

    private PdfPTable treatmentChapter(Anexa1Sheet s, boolean recovery) {
        PdfPTable t = table(new float[]{7, 14, 20, 27, 32});
        bandRow(t, recovery ? "3. VALORIFICAREA DEŞEURILOR" : "4. ELIMINAREA DEŞEURILOR", 5);

        head(t, "Nr. crt.", 1, 1);
        head(t, "Luna", 1, 1);
        head(t, recovery ? "Cantitatea de deşeu valorificată" : "Cantitatea de deşeu eliminată", 1, 1);
        // Legea 211/2011 was repealed by OUG 92/2021; its annexes carry the same numbers.
        head(t, recovery
                ? "Operaţia de valorificare, conform anexei nr. 3 din OUG 92/2021"
                : "Operaţia de eliminare, conform anexei nr. 2 din OUG 92/2021", 1, 1);
        head(t, recovery
                ? "Agentul economic care efectuează operaţia de valorificare"
                : "Agentul economic care efectuează operaţia de eliminare", 1, 1);

        BigDecimal total = BigDecimal.ZERO;
        for (Anexa1Sheet.Anexa1MonthRow row : s.rows()) {
            BigDecimal quantity = recovery ? row.recovered() : row.disposed();
            String operations = recovery ? row.recoveryOperations() : row.disposalOperations();
            String operators = recovery ? row.recoveryOperators() : row.disposalOperators();

            cell(t, String.valueOf(row.month()), Element.ALIGN_CENTER);
            cell(t, MONTHS[row.month() - 1], Element.ALIGN_LEFT);
            num(t, quantity);
            cell(t, dash(operations), Element.ALIGN_CENTER);
            // No operator named means we did it ourselves, on our own site.
            cell(t, operators == null || operators.isBlank()
                    ? (quantity.signum() > 0 ? "în activitatea proprie" : "-")
                    : operators, Element.ALIGN_LEFT);
            total = total.add(quantity);
        }
        totalCell(t, 1);
        cellBold(t, "TOTAL AN", Element.ALIGN_LEFT);
        numBold(t, total);
        cell(t, "", Element.ALIGN_CENTER);
        cell(t, "", Element.ALIGN_LEFT);
        return t;
    }

    /** The five closed nomenclators, verbatim, exactly as the form prints them under the tables. */
    private Paragraph notes() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3f);
        p.add(new Phrase(cp1250("NOTĂ:\n"), columnHead));
        p.add(new Phrase(cp1250(
                "1) Tipul de stocare: RM - recipient metalic; RP - recipient de plastic; "
                        + "BZ - bazin decantor; CT - container transportabil; CF - container fix; "
                        + "S - saci; PD - platformă de deshidratare; VN - în vrac, neacoperit; "
                        + "VA - în vrac, incintă acoperită; RL - recipient din lemn; A - altele.\n"
                        + "2) Modul de tratare: TM - tratare mecanică; TC - tratare chimică; "
                        + "TMC - tratare mecano-chimică; TB - tratare biochimică; TT - tratare termică; "
                        + "D - deshidratare; A - altele.\n"
                        + "3) Scopul tratării: V - pentru valorificare; E - în vederea eliminării.\n"
                        + "4) Mijlocul de transport: AS - autospeciale; AN - auto nespecial; "
                        + "H - transport hidraulic; CF - cale ferată; A - altele.\n"
                        + "5) Destinaţia: DO - depozitul de gunoi al oraşului/comunei; HP - haldă proprie; "
                        + "HC - haldă industrială comună; I - incinerarea în scopul eliminării; "
                        + "Vr - valorificare prin agenţi economici autorizaţi; "
                        + "P - utilizare materială sau energetică în propria întreprindere; "
                        + "Ve - valorificare energetică prin agenţi economici autorizaţi; A - altele."), note));
        return p;
    }

    // --- table plumbing ---

    private PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        widths(t, widths);
        t.setSpacingBefore(3f);
        return t;
    }

    private void widths(PdfPTable t, float... widths) {
        try {
            t.setWidths(widths);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Bad column widths for the Anexa 1 sheet", ex);
        }
    }

    /** The black band the form puts above every chapter, with its title reversed out of it. */
    private void bandRow(PdfPTable t, String title, int span) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(title), band));
        c.setColspan(span);
        c.setBackgroundColor(BAND);
        c.setPadding(1.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void head(PdfPTable t, String text, int colspan, int rowspan) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text), columnHead));
        c.setColspan(colspan);
        c.setRowspan(rowspan);
        c.setPadding(1.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(c);
    }

    private void headSpan(PdfPTable t, String text, int colspan) {
        head(t, text, colspan, 1);
    }

    private void cell(PdfPTable t, String text, int align) {
        addCell(t, text, body, align);
    }

    private void cellBold(PdfPTable t, String text, int align) {
        addCell(t, text, bodyBold, align);
    }

    private void addCell(PdfPTable t, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text == null ? "" : text), font));
        c.setPadding(1.2f);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void num(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), body, Element.ALIGN_RIGHT);
    }

    private void numBold(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), bodyBold, Element.ALIGN_RIGHT);
    }

    private void totalCell(PdfPTable t, int span) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setColspan(span);
        c.setPadding(2f);
        t.addCell(c);
    }

    private String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static final DecimalFormat KG = kgFormat();

    private static DecimalFormat kgFormat() {
        // Three decimals with a dot, exactly as every filled sheet prints them: "53.000",
        // "636.000". Not the Romanian comma — the models are what an inspector will compare against.
        return new DecimalFormat("#0.000", new DecimalFormatSymbols(Locale.ROOT));
    }

    private String kg(BigDecimal value) {
        return value == null ? "" : KG.format(value);
    }

    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş')
                .replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont centralEuropean(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for Anexa 1", ex);
        }
    }
}
