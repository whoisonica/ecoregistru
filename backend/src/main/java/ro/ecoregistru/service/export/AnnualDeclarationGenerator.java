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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Renders the annual declaration — the {@code raportare deseuri generate} sheet of the received
 * workbooks — one page per work point: the identification header, one row per waste code, and the
 * signature block.
 *
 * <p>Laid out after the blank template the specialist sent ({@code documente oficiale/RAPORTARE
 * DESEURI GENERATE.xlsx}) and the six filled copies that follow it. Column order and wording are
 * the models': <em>cod deşeu · tip deşeu · stoc la 01.01 · generat · valorificat · eliminat · stoc ·
 * valorificat prin · eliminat prin</em>, all in kilograms.
 *
 * <p><b>One correction against the models.</b> Their "stoc la 01.01.{year}" header is copy-pasted
 * between years and goes stale — the 2024 Cluj sheet says 01.01.2023, the 2024 Bragadiru sheet says
 * 01.01.2020. We print the opening date of the year being declared, because the figure underneath
 * it is that year's opening stock. This is a clerical slip in the source workbooks, not a modelling
 * choice of theirs, so it is corrected rather than reproduced.
 *
 * <p>Diacritics go through Cp1250 for the same reason as the other two forms: Cp1252 has no ă/ş/ţ
 * and would silently drop them.
 */
@Component
public class AnnualDeclarationGenerator {

    private final Font title;
    private final Font header;
    private final Font headerValue;
    private final Font columnHead;
    private final Font body;
    private final Font note;

    public AnnualDeclarationGenerator() {
        BaseFont plain = centralEuropean(false);
        BaseFont bold = centralEuropean(true);
        this.title = new Font(bold, 11f);
        this.header = new Font(plain, 8f);
        this.headerValue = new Font(bold, 8f);
        this.columnHead = new Font(bold, 7f);
        this.body = new Font(plain, 7.5f);
        this.note = new Font(plain, 6.5f);
    }

    public byte[] render(List<AnnualDeclaration> declarations) {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 24, 24);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (int i = 0; i < declarations.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                addDeclaration(doc, declarations.get(i));
            }
            if (declarations.isEmpty()) {
                doc.add(new Paragraph(cp1250(
                        "Nicio evidenţă pentru anul şi punctul de lucru alese."), body));
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the annual declaration", ex);
        }
    }

    private void addDeclaration(Document doc, AnnualDeclaration d) {
        doc.add(identification(d));
        doc.add(documentTitle(d));
        doc.add(table(d));
        if (d.hasUnclassifiedOut()) {
            doc.add(unclassifiedNote());
        }
        doc.add(signature(d));
    }

    /**
     * The eleven-line header of the models, in their order and with their wording. A rubric we do
     * not hold prints its label with nothing after it — the same choice the whole module makes:
     * a missing figure must be visible as missing, never filled with a plausible one.
     */
    private PdfPTable identification(AnnualDeclaration d) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(70);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        widths(t, 30, 70);
        t.getDefaultCell().setBorder(0);
        t.getDefaultCell().setPadding(1f);

        headerLine(t, "Denumirea operatorului economic:", d.companyName());
        headerLine(t, "Adresa:", d.companyAddress());
        headerLine(t, "Tel/fax/e-mail:", d.contactLine());
        headerLine(t, "CUI:", d.cui());
        headerLine(t, "Autorizaţie de mediu (nr./valabilitate):", d.environmentalAuth());
        headerLine(t, "Cod CAEN:", d.caenCode());
        headerLine(t, "Anul pentru care se realizează raportarea:", String.valueOf(d.year()));
        headerLine(t, "Punct de lucru:", d.workPointName());
        headerLine(t, "Unitatea de măsură:", "kg");
        t.setSpacingAfter(6f);
        return t;
    }

    private void headerLine(PdfPTable t, String label, String value) {
        t.addCell(new Phrase(cp1250(label), header));
        t.addCell(new Phrase(cp1250(value == null ? "" : value), headerValue));
    }

    /**
     * The same title the fişa carries, with the year attached — verbatim from six of the received
     * files, and here it sits where those files put it: on the summary sheet.
     */
    private Paragraph documentTitle(AnnualDeclaration d) {
        Paragraph p = new Paragraph(
                cp1250("Evidenţa gestiunii deşeurilor generate " + d.year()), title);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(5f);
        return p;
    }

    private PdfPTable table(AnnualDeclaration d) {
        PdfPTable t = new PdfPTable(9);
        t.setWidthPercentage(100);
        widths(t, 8, 20, 9, 9, 9, 9, 9, 13.5f, 13.5f);
        t.setHeaderRows(1);

        head(t, "Cod deşeu\ncf. HG 856/2002");
        head(t, "Tip deşeu\ncf. HG 856/2002");
        head(t, "Stoc la 01.01." + d.year() + "\n[kg]");
        head(t, "Generat\n[kg]");
        head(t, "Valorificat\n[kg]");
        head(t, "Eliminat\n[kg]");
        head(t, "Stoc\n[kg]");
        head(t, "Valorificat prin:");
        head(t, "Eliminat prin:");

        for (AnnualDeclaration.Row row : d.rows()) {
            cell(t, row.wasteCode(), Element.ALIGN_CENTER);
            cell(t, row.wasteCodeName(), Element.ALIGN_LEFT);
            num(t, row.openingStock());
            num(t, row.generated());
            num(t, row.recovered());
            num(t, row.disposed());
            // The marker rides on the stock, because the stock is the figure that does not add up
            // and because the code column cannot carry it: in the European List a trailing star is
            // what makes a code hazardous, and "02 02 02 *" would read as a different waste.
            cell(t, kg(row.closingStock()) + (row.hasUnclassifiedOut() ? "  (*)" : ""),
                    Element.ALIGN_RIGHT);
            cell(t, dash(row.recoveredThrough()), Element.ALIGN_LEFT);
            cell(t, dash(row.disposedThrough()), Element.ALIGN_LEFT);
        }

        // No TOTAL row: none of the models has one, and it would add kilograms of paper to
        // kilograms of household waste — a number nobody asked for and nobody can use.
        t.setSpacingAfter(5f);
        return t;
    }

    /**
     * Why a starred row does not balance. The models have no such row and no such note — whoever
     * fills one by hand writes the operation code as they write the line. Ours can carry exits
     * recorded before the code became mandatory, and printing them without a word would hand the
     * authority a sheet whose arithmetic is wrong with no explanation on it.
     */
    private Paragraph unclassifiedNote() {
        Paragraph p = new Paragraph(cp1250(
                "(*) Rândul conţine cantităţi ieşite de pe amplasament fără cod de operaţiune R/D, "
                        + "deci scăzute din stoc dar necuprinse în „Valorificat\" sau „Eliminat\". "
                        + "Completaţi codul pe mişcările respective (ecranul Mişcări) şi regeneraţi "
                        + "evidenţa înainte de depunere."), note);
        p.setSpacingAfter(6f);
        return p;
    }

    /** "Intocmit / Functia / Telefon / Email" — the signature block of every filled model. */
    private PdfPTable signature(AnnualDeclaration d) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        widths(t, 26, 74);
        t.getDefaultCell().setBorder(0);
        t.getDefaultCell().setPadding(1f);

        headerLine(t, "Întocmit:", d.preparedBy());
        headerLine(t, "Funcţia:", d.preparedByRole());
        headerLine(t, "Telefon:", d.preparedByPhone());
        headerLine(t, "E-mail:", d.preparedByEmail());
        return t;
    }

    // --- table helpers, same shapes as the other two forms ---

    private void widths(PdfPTable t, float... widths) {
        try {
            t.setWidths(widths);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Bad column widths for the annual declaration", ex);
        }
    }

    private void head(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text), columnHead));
        c.setPadding(2.5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(new java.awt.Color(0xF0, 0xF0, 0xED));
        t.addCell(c);
    }

    private void cell(PdfPTable t, String text, int align) {
        addCell(t, text, body, align);
    }

    private void addCell(PdfPTable t, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text == null ? "" : text), font));
        c.setPadding(2.2f);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(c);
    }

    private void num(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), body, Element.ALIGN_RIGHT);
    }

    private String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static final DecimalFormat KG = kgFormat();

    private static DecimalFormat kgFormat() {
        // Same three decimals with a dot as the fişa, so the two documents of one year read alike.
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
            throw new IllegalStateException(
                    "Cannot load the Cp1250 Helvetica for the annual declaration", ex);
        }
    }
}
