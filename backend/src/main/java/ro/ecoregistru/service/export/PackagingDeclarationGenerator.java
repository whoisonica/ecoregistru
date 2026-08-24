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
import ro.ecoregistru.enums.PackagingMaterial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Renders <b>Anexa 1 Ambalaje</b> (Ordinul MMP 794/2012, anexa nr. 1) — the declaration of
 * packaging put on the national market and of the packaging waste handed over.
 *
 * <p>Laid out after the filled copy received from the specialist
 * ({@code documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}): the seven-line
 * identification header, then <i>Tabel 1. Ambalaje introduse pe piaţa naţională</i> with its eleven
 * material rows and seven numbered columns, then <i>Tabelul 2. Deşeuri de ambalaje gestionate</i>,
 * then the signature block. In <b>kilograms</b>, as the act prints at the head of each table.
 *
 * <p><b>Why this document is called "Anexa 1" here and the waste record is not.</b> The two share
 * the name in the acts, and the client's own vocabulary gives the short name to this one — so on
 * 24.08.2026 the application followed: the HG 856/2002 sheet is now "Evidenţa gestiunii deşeurilor
 * generate", and "Anexa 1" means the packaging declaration.
 *
 * <p>Empty cells stay empty. A material nobody answered for prints blank, not 0.000: this form is
 * filed with an authority and the difference between "none" and "not answered" is the client's to
 * state, not ours to assume.
 */
@Component
public class PackagingDeclarationGenerator {

    private static final String TITLE =
            "ANEXA Nr. 1: Producători şi importatori de ambalaje de desfacere, de produse "
                    + "ambalate, supraambalatori de produse ambalate";

    private final Font title;
    private final Font header;
    private final Font headerValue;
    private final Font tableTitle;
    private final Font columnHead;
    private final Font body;
    private final Font bodyBold;
    private final Font note;

    public PackagingDeclarationGenerator() {
        BaseFont plain = centralEuropean(false);
        BaseFont bold = centralEuropean(true);
        this.title = new Font(bold, 8.5f);
        this.header = new Font(plain, 8f);
        this.headerValue = new Font(bold, 8f);
        this.tableTitle = new Font(bold, 8f);
        this.columnHead = new Font(bold, 6f);
        this.body = new Font(plain, 6.5f);
        this.bodyBold = new Font(bold, 6.5f);
        this.note = new Font(plain, 5f);
    }

    public byte[] render(PackagingDeclaration d) {
        Document doc = new Document(PageSize.A4, 28, 28, 24, 24);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph head = new Paragraph(cp1250(TITLE), title);
            head.setSpacingAfter(8f);
            doc.add(head);

            doc.add(identification(d));
            doc.add(sectionTitle("Tabel 1. Ambalaje introduse pe piaţa naţională", true));
            doc.add(marketTable(d));
            doc.add(marketNotes());
            doc.add(sectionTitle("Tabelul 2. Deşeuri de ambalaje gestionate", true));
            doc.add(handoverTable(d));
            doc.add(handoverNotes(d));
            doc.add(signature(d));

            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the packaging declaration", ex);
        }
    }

    // --- the seven-line header, verbatim from the filled model ---

    private PdfPTable identification(PackagingDeclaration d) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(0);
        t.getDefaultCell().setPadding(0.8f);

        line(t, "Denumirea operatorului economic: ", d.companyName());
        line(t, "Judeţ şi localitate: ", d.county());
        line(t, "Adresa: ", d.address());
        line(t, "Tel./Fax/e-mail: ", d.contact());
        line(t, "Cod CAEN pentru activitatea aferentă raportării: ", d.caenCode());
        line(t, "CUI: ", d.cui());
        line(t, "Anul pentru care se realizează raportarea: ", String.valueOf(d.year()));
        t.setSpacingAfter(6f);
        return t;
    }

    private void line(PdfPTable t, String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(cp1250(label), header));
        p.add(new Phrase(cp1250(value == null ? "" : value), headerValue));
        PdfPCell c = new PdfPCell();
        c.addElement(p);
        c.setBorder(0);
        c.setPadding(0.8f);
        t.addCell(c);
    }

    // --- table 1: what was put on the market ---

    private PdfPTable marketTable(PackagingDeclaration d) {
        PdfPTable t = new PdfPTable(new float[]{16, 12, 12, 12, 12, 12, 12, 12});
        t.setWidthPercentage(100);
        t.setSpacingBefore(2f);

        unitRow(t, 8);

        head(t, "Material", 1, 3);
        head(t, "Ambalaje de desfacere fabricate/importate *1)", 1, 3);
        head(t, "Ambalaje folosite la ambalarea produselor introduse pe piaţa naţională *4)", 5, 1);
        head(t, "Ambalaje cu conţinut periculos *3) din coloana 3", 1, 3);
        head(t, "Total (col. 3+5)", 1, 2);
        head(t, "Ambalaje primare", 2, 1);
        head(t, "Ambalaje secundare şi de transport", 2, 1);
        head(t, "Total", 1, 1);
        head(t, "din care: ambalaj reutilizabil *2)", 1, 1);
        head(t, "Total", 1, 1);
        head(t, "din care: ambalaj reutilizabil *2)", 1, 1);

        // The numbered row the form draws under its headings: 0 1 2 3 4 5 6 7.
        for (int i = 0; i <= 7; i++) {
            cellBold(t, String.valueOf(i), Element.ALIGN_CENTER);
        }

        for (PackagingDeclaration.MarketRow row : d.marketRows()) {
            cell(t, row.material().getOfficialLabel(), Element.ALIGN_LEFT);
            num(t, row.salesPackaging());
            num(t, row.packagedGoodsTotal());
            num(t, row.primaryTotal());
            num(t, row.primaryReusable());
            num(t, row.secondaryTotal());
            num(t, row.secondaryReusable());
            num(t, row.hazardousContent());

            // The two subtotal rows the form draws inside the list, right after their parts.
            if (row.material() == PackagingMaterial.ALTE_PLASTICE) {
                subtotal(t, "Total plastic", d, PackagingMaterial.plasticParts());
            }
            if (row.material() == PackagingMaterial.OTEL) {
                subtotal(t, "Total metal", d, PackagingMaterial.metalParts());
            }
        }
        totalRow(t, d);
        return t;
    }

    private void subtotal(PdfPTable t, String label, PackagingDeclaration d,
                          List<PackagingMaterial> parts) {
        List<PackagingDeclaration.MarketRow> rows = d.marketRows().stream()
                .filter(r -> parts.contains(r.material()))
                .toList();
        cellBold(t, label, Element.ALIGN_LEFT);
        sumInto(t, rows);
    }

    private void totalRow(PdfPTable t, PackagingDeclaration d) {
        cellBold(t, "TOTAL:", Element.ALIGN_LEFT);
        sumInto(t, d.marketRows());
    }

    /**
     * The seven summed cells of a subtotal row. A column where no material carried a figure prints
     * empty rather than 0.000 — the same rule as the rows above it, so a blank table stays blank
     * instead of growing a row of zeroes nobody declared.
     */
    private void sumInto(PdfPTable t, List<PackagingDeclaration.MarketRow> rows) {
        List<Function<PackagingDeclaration.MarketRow, BigDecimal>> columns = List.of(
                PackagingDeclaration.MarketRow::salesPackaging,
                PackagingDeclaration.MarketRow::packagedGoodsTotal,
                PackagingDeclaration.MarketRow::primaryTotal,
                PackagingDeclaration.MarketRow::primaryReusable,
                PackagingDeclaration.MarketRow::secondaryTotal,
                PackagingDeclaration.MarketRow::secondaryReusable,
                PackagingDeclaration.MarketRow::hazardousContent);

        for (Function<PackagingDeclaration.MarketRow, BigDecimal> column : columns) {
            BigDecimal sum = null;
            for (PackagingDeclaration.MarketRow row : rows) {
                BigDecimal value = column.apply(row);
                if (value != null) {
                    sum = sum == null ? value : sum.add(value);
                }
            }
            numBold(t, sum);
        }
    }

    private Paragraph marketNotes() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2f);
        p.setLeading(6f);
        p.add(new Phrase(cp1250(
                "1) Se raportează numai ambalajele de desfacere destinate pieţei naţionale, "
                        + "definite prin HG nr. 621/2005.\n"
                        + "2) Se raportează o singură dată, atunci când sunt introduse în circuitul "
                        + "de umplere şi livrate pentru prima dată.\n"
                        + "3) Se raportează numai ambalajele care au conţinut substanţe periculoase "
                        + "inscripţionate ca atare (HG nr. 937/2010). Cantităţile de ambalaje cu "
                        + "conţinut periculos sunt tot ambalaje primare şi se regăsesc şi în coloana 3.\n"
                        + "4) Se raportează numai ambalajele folosite la ambalarea produselor destinate "
                        + "pieţei naţionale şi se includ şi ambalajele utilizate pentru ambalarea "
                        + "ambalajelor de desfacere."), note));
        return p;
    }

    // --- table 2: what was handed over ---

    private PdfPTable handoverTable(PackagingDeclaration d) {
        PdfPTable t = new PdfPTable(new float[]{16, 14, 32, 14, 24});
        t.setWidthPercentage(100);
        t.setSpacingBefore(2f);

        unitRow(t, 5);

        head(t, "Materialul", 1, 3);
        head(t, "Deşeuri de ambalaje încredinţate unui operator economic autorizat", 3, 1);
        head(t, "Operaţiunea 2) la care a supus deşeul operatorul menţionat în coloana 2", 1, 3);
        head(t, "Cantitatea", 1, 2);
        head(t, "Operatorul economic 1) pentru colectarea, reciclarea şi valorificarea deşeurilor "
                + "de ambalaje", 2, 1);
        head(t, "Denumirea, adresă punct de lucru", 1, 1);
        head(t, "CUI", 1, 1);

        if (d.handoverRows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase(cp1250(
                    "Nicio predare de deşeuri de ambalaje (coduri 15 01) înregistrată în "
                            + d.year() + "."), body));
            empty.setColspan(5);
            empty.setPadding(3f);
            t.addCell(empty);
            return t;
        }

        BigDecimal total = null;
        for (PackagingDeclaration.HandoverRow row : d.handoverRows()) {
            cell(t, row.material().getOfficialLabel(), Element.ALIGN_LEFT);
            num(t, row.quantity());
            cell(t, row.operatorName()
                    + (row.operatorAddress() == null ? "" : ", " + row.operatorAddress()),
                    Element.ALIGN_LEFT);
            cell(t, row.operatorCui(), Element.ALIGN_CENTER);
            cell(t, row.operation() == null || row.operation().isBlank() ? "-" : row.operation(),
                    Element.ALIGN_CENTER);
            if (row.quantity() != null) {
                total = total == null ? row.quantity() : total.add(row.quantity());
            }
        }
        cellBold(t, "TOTAL:", Element.ALIGN_LEFT);
        numBold(t, total);
        cell(t, "", Element.ALIGN_LEFT);
        cell(t, "", Element.ALIGN_CENTER);
        cell(t, "", Element.ALIGN_CENTER);
        return t;
    }

    private Paragraph handoverNotes(PackagingDeclaration d) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(2f);
        p.setLeading(6f);
        p.add(new Phrase(cp1250(
                "1) Se completează câte o rubrică distinctă pentru fiecare dintre operatorii care "
                        + "au preluat deşeurile de ambalaje din materialul respectiv.\n"
                        + "2) Se menţionează operaţiunea la care au fost supuse deşeurile potrivit "
                        + "anexei nr. 3 la OUG nr. 92/2021 privind regimul deşeurilor.\n"
                        + "În cazul în care operaţiunea de reciclare/valorificare se face prin export "
                        + "sau transfer intracomunitar, se va specifica alături de denumirea "
                        + "operatorului economic, adresa punctului de lucru şi ţara de destinaţie.\n"
                        + "NOTĂ: Se completează în tabel distinct în cazul deşeurilor de ambalaje "
                        + "periculoase."), note));

        // The codes the European List cannot place on a material row. Said out loud rather than
        // quietly parked in "Altele": the client is the only one who knows whether the metal
        // packaging was aluminium or steel.
        if (!d.ambiguousCodes().isEmpty()) {
            p.add(new Phrase(cp1250(
                    "\nDe verificat: cantităţile pe codurile " + String.join(", ", d.ambiguousCodes())
                            + " sunt trecute la „Altele”, fiindcă Lista Europeană nu spune din ce "
                            + "material sunt (15 01 04 acoperă şi aluminiul, şi oţelul). Mută-le pe "
                            + "rândul potrivit dacă ştii materialul."), note));
        }
        return p;
    }

    private Paragraph signature(PackagingDeclaration d) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(10f);
        p.setLeading(9f);
        p.add(new Phrase(cp1250("Semnătura autorizată şi ştampila\n"), tableTitle));
        p.add(new Phrase(cp1250("Numele şi prenumele: "), header));
        p.add(new Phrase(cp1250(d.preparedBy() == null ? "" : d.preparedBy()), headerValue));
        p.add(new Phrase(cp1250("\nFuncţia: "), header));
        p.add(new Phrase(cp1250(d.preparedByRole() == null ? "" : d.preparedByRole()), headerValue));
        p.add(new Phrase(cp1250("\nData: "), header));
        return p;
    }

    // --- plumbing ---

    private Paragraph sectionTitle(String text, boolean spaced) {
        Paragraph p = new Paragraph(cp1250(text), tableTitle);
        if (spaced) {
            p.setSpacingBefore(8f);
        }
        return p;
    }

    /** The "[kilograme]" the act prints above the right-hand edge of each table. */
    private void unitRow(PdfPTable t, int span) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250("[kilograme]"), note));
        c.setColspan(span);
        c.setBorder(0);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(1f);
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

    private void cell(PdfPTable t, String text, int align) {
        addCell(t, text, body, align);
    }

    private void cellBold(PdfPTable t, String text, int align) {
        addCell(t, text, bodyBold, align);
    }

    private void addCell(PdfPTable t, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text == null ? "" : text), font));
        c.setPadding(1.4f);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void num(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), body, Element.ALIGN_RIGHT);
    }

    private void numBold(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), bodyBold, Element.ALIGN_RIGHT);
    }

    private static final DecimalFormat KG =
            new DecimalFormat("#0.000", new DecimalFormatSymbols(Locale.ROOT));

    /** Null prints as an empty cell: "not answered" is not zero. */
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
            throw new IllegalStateException("Cannot load Cp1250 Helvetica for the packaging form", ex);
        }
    }
}
