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
import ro.ecoregistru.enums.WasteOperationCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Renders <b>Anexa 3 la Ordinul 794/2012</b> — the annual report of collectors, traders, recyclers
 * and recoverers of packaging waste — as the paper copy art. 6 requires beside the {@code .xls}.
 *
 * <p>Same content and same single table as {@link PackagingAnexa3XlsGenerator}; this one exists
 * because the article asks for both: "Datele de raportare se transmit în format electronic «.xls»
 * protejat împotriva modificării datelor <b>şi pe suport hârtie</b>". So the PDF is not a
 * convenience, it is the second half of a legal requirement.
 *
 * <p>Landscape A4, because the widest table has eight columns and the provenance labels are
 * sentences rather than words ("generator persoană juridică"). Portrait fitted anexa 1, whose
 * columns are all numbers.
 *
 * <p>Cp1250 like every other printed form here: Cp1252 has no ă/ş/ţ and would silently drop them
 * off an official document. See decision 8.
 */
@Component
public class PackagingAnexa3Generator {

    private final Font title;
    private final Font header;
    private final Font headerValue;
    private final Font columnHead;
    private final Font body;
    private final Font bodyBold;
    private final Font note;

    public PackagingAnexa3Generator() {
        BaseFont plain = centralEuropean(false);
        BaseFont bold = centralEuropean(true);
        this.title = new Font(bold, 9f);
        this.header = new Font(plain, 8f);
        this.headerValue = new Font(bold, 8f);
        this.columnHead = new Font(bold, 6.5f);
        this.body = new Font(plain, 7f);
        this.bodyBold = new Font(bold, 7f);
        this.note = new Font(plain, 5.5f);
    }

    public byte[] render(PackagingAnexa3 d) {
        if (!d.printable()) {
            throw new IllegalStateException("packaging.operator.role.required");
        }
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 24, 24);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph head = new Paragraph(
                    cp1250("ANEXA Nr. 3 — " + d.role().tableHeading()), title);
            head.setSpacingAfter(8f);
            doc.add(head);

            doc.add(identification(d));
            doc.add(table(d));
            doc.add(notes());
            doc.add(signature(d));

            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the packaging anexa 3", ex);
        }
    }

    // --- the nine-line header of the received model ---

    private PdfPTable identification(PackagingAnexa3 d) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(6f);

        line(t, "Denumirea operatorului economic: ", d.companyName());
        line(t, "Judeţ şi localitate: ", d.county());
        line(t, "Adresa: ", d.address());
        line(t, "Tel/fax/e-mail: ", d.contact());
        line(t, "CUI: ", d.cui());
        line(t, "Autorizaţie de mediu/nr. înregistrare/data/valabilitate: ", d.authorization());
        line(t, "COD CAEN: ", d.caenCode());
        line(t, "Anul pentru care se realizează raportarea: ", String.valueOf(d.year()));
        line(t, "Punct de lucru: ", workPoint(d));
        return t;
    }

    private void line(PdfPTable t, String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(cp1250(label), header));
        p.add(new Phrase(cp1250(value == null ? "" : value), headerValue));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(0);
        c.setPadding(0.6f);
        t.addCell(c);
    }

    private String workPoint(PackagingAnexa3 d) {
        if (d.workPointName() == null) {
            return "";
        }
        return d.workPointAddress() == null || d.workPointAddress().isBlank()
                ? d.workPointName()
                : d.workPointName() + " — " + d.workPointAddress();
    }

    // --- the table itself ---

    private PdfPTable table(PackagingAnexa3 d) {
        boolean table2 = d.usesTable2();
        PdfPTable t = table2
                ? new PdfPTable(new float[]{16, 11, 11, 18, 12, 14, 12})
                : new PdfPTable(new float[]{18, 12, 12, 20, 13, 25});
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        headRow(t, table2);
        numberBand(t, table2 ? 7 : 6);
        rows(t, d, table2);
        return t;
    }

    private void headRow(PdfPTable t, boolean table2) {
        head(t, "Material");
        head(t, table2
                ? "Cantitatea preluată (kg)\nTotal"
                : "Cantitatea colectată (kg)\nTotal");
        head(t, "din care periculoase");
        head(t, "Provenienţa*2");
        if (table2) {
            head(t, "Cantitatea reciclată (kg)");
            head(t, "Cantitatea valorificată prin alte metode (kg)");
            head(t, "Metoda");
        } else {
            head(t, "Cantitatea comercializată/trimisă la reciclare/valorificare/export (kg)");
            head(t, "Operatorul economic");
        }
    }

    /**
     * The numbered band under the headings. Not decoration: the annex's notes refer to columns by
     * these numbers, and art. 8 alin. (1) lit. d) names "coloana 0" when it restricts "altele".
     */
    private void numberBand(PdfPTable t, int columns) {
        for (int i = 0; i < columns; i++) {
            addCell(t, String.valueOf(i), columnHead, Element.ALIGN_CENTER);
        }
    }

    /**
     * One block per material with anything in it: a line per provenance on the left, a line per
     * operator (tabelul 1) or a single line of figures (tabelul 2) on the right, then the
     * material's own total. "total plastic" and "total metal" follow their last part, and the grand
     * total closes the table.
     *
     * <p>A material nobody moved is skipped rather than printed as zeros — on a filed form, absent
     * and zero are different statements.
     */
    private void rows(PdfPTable t, PackagingAnexa3 d, boolean table2) {
        for (PackagingMaterial material : PackagingMaterial.values()) {
            List<PackagingAnexa3.IntakeRow> intake = d.intake().stream()
                    .filter(x -> x.material() == material).toList();
            List<PackagingAnexa3.HandoverRow> handovers = d.handovers().stream()
                    .filter(x -> x.material() == material).toList();
            PackagingAnexa3.TreatmentRow treatment = d.treatments().stream()
                    .filter(x -> x.material() == material).findFirst().orElse(null);

            int rightLines = table2 ? (treatment == null ? 0 : 1) : handovers.size();
            if (intake.isEmpty() && rightLines == 0) {
                // Skipped, but the group sum still runs: "total plastic" is owed whenever PET
                // has figures, even if Alte plastice has none.
                groupTotals(t, d, material, table2);
                continue;
            }

            boolean first = true;

            // The two halves are stacked, not aligned side by side. They have different
            // cardinalities - three provenances against two operators, say - and pairing them row
            // by row would put "22.000 kg -> Reciclator SA" on the same line as "populatie" and
            // read as if that is where the load came from. On paper the material's rubric is a box,
            // not a row; here the border makes a row look like a statement, so a line carries one
            // half or the other and never a coincidence.
            for (PackagingAnexa3.IntakeRow row : intake) {
                cell(t, first ? material.getOfficialLabel() : "", Element.ALIGN_LEFT);
                first = false;
                num(t, row.total());
                num(t, zeroToNull(row.hazardous()));
                cell(t, row.origin().getOfficialLabel(), Element.ALIGN_LEFT);
                emptyRight(t, table2);
            }

            if (table2) {
                if (treatment != null) {
                    cell(t, first ? material.getOfficialLabel() : "", Element.ALIGN_LEFT);
                    first = false;
                    emptyLeft(t);
                    num(t, treatment.recycled());
                    num(t, treatment.otherRecovery());
                    cell(t, methods(treatment), Element.ALIGN_CENTER);
                }
            } else {
                for (PackagingAnexa3.HandoverRow h : handovers) {
                    cell(t, first ? material.getOfficialLabel() : "", Element.ALIGN_LEFT);
                    first = false;
                    emptyLeft(t);
                    num(t, h.quantity());
                    cell(t, operator(h), Element.ALIGN_LEFT);
                }
            }

            // A material summed into "total plastic" or "total metal" gets no line of its own: the
            // model closes hartie-carton and lemn that way, never PET, and printing both would
            // state the same kilograms twice under two labels.
            if (!material.isSummedIntoAGroup()) {
                totalLine(t, "total " + material.getOfficialLabel().toLowerCase(),
                        d.totalsFor(material), table2);
            }
            groupTotals(t, d, material, table2);
        }

        totalLine(t, "TOTAL ambalaje", d.grandTotals(), table2);
    }

    /** The right-hand columns left blank, so a provenance line claims nothing about an operator. */
    private void emptyRight(PdfPTable t, boolean table2) {
        cell(t, "", Element.ALIGN_RIGHT);
        cell(t, "", table2 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        if (table2) {
            cell(t, "", Element.ALIGN_CENTER);
        }
    }

    /** The intake columns left blank on an outbound line, for the same reason. */
    private void emptyLeft(PdfPTable t) {
        cell(t, "", Element.ALIGN_RIGHT);
        cell(t, "", Element.ALIGN_RIGHT);
        cell(t, "", Element.ALIGN_LEFT);
    }

    /**
     * "total plastic" after Alte plastice, "total metal" after Oţel — the two summed rows the annex
     * draws between the material rows. Sticlă, hârtie carton, lemn and altele stand on their own.
     *
     * <p>Called even for a material that printed no block, because the sum is owed whenever any of
     * its parts has figures: PET alone still earns a "total plastic" line.
     */
    private void groupTotals(PdfPTable t, PackagingAnexa3 d, PackagingMaterial justPrinted,
                             boolean table2) {
        List<PackagingMaterial> group;
        String label;
        if (justPrinted == PackagingMaterial.ALTE_PLASTICE) {
            group = PackagingMaterial.plasticParts();
            label = "total plastic";
        } else if (justPrinted == PackagingMaterial.OTEL) {
            group = PackagingMaterial.metalParts();
            label = "total metal";
        } else {
            return;
        }

        PackagingAnexa3.Totals totals = d.totalsOver(group);
        if (!totals.isEmpty()) {
            totalLine(t, label, totals, table2);
        }
    }

    /**
     * A summed line, printed from the figures the document computed once
     * ({@link PackagingAnexa3.Totals}) rather than from totals this class adds up itself.
     *
     * <p>That is not tidiness. When each generator summed on its own, this one folded both of
     * tabelul 2's outbound figures into the "cantitatea reciclata" column and left its neighbour
     * blank — 24.100 kg reported as recycled where 22.000 were recycled and 2.100 recovered by
     * other means — while the spreadsheet printed them correctly.
     */
    private void totalLine(PdfPTable t, String label, PackagingAnexa3.Totals totals,
                           boolean table2) {
        cellBold(t, label, Element.ALIGN_LEFT);
        numBold(t, totals.intake());
        // "din care periculoase" is a sub-column of the figure beside it, so it is summed too: a
        // total row that totals one column and leaves its neighbour blank contradicts its own
        // detail lines.
        numBold(t, zeroToNull(totals.hazardous()));
        cell(t, "", Element.ALIGN_LEFT);
        numBold(t, zeroToNull(totals.first()));
        if (table2) {
            numBold(t, zeroToNull(totals.second()));
            cell(t, "", Element.ALIGN_CENTER);
        } else {
            cell(t, "", Element.ALIGN_LEFT);
        }
    }

    private String operator(PackagingAnexa3.HandoverRow h) {
        String name = h.operatorName() == null ? "" : h.operatorName();
        if (h.operatorCui() == null || h.operatorCui().isBlank()) {
            return name;
        }
        return name + " (" + h.operatorCui() + ")";
    }

    private String methods(PackagingAnexa3.TreatmentRow treatment) {
        return treatment.methods().stream().map(WasteOperationCode::name)
                .reduce((a, b) -> a + ", " + b).orElse("");
    }

    // --- notes and signature ---

    /**
     * Only nota 2, which is the note the provenance column actually references and the only one we
     * hold verbatim. The rest are not invented: see {@link PackagingAnexa3XlsGenerator}.
     */
    private Paragraph notes() {
        Paragraph p = new Paragraph(cp1250(
                "*2) Se menţionează, după caz, «populaţie», «generator persoană juridică», "
                        + "«colector», «comerciant», în funcţie de persoanele juridice sau fizice "
                        + "de la care provin deşeurile de ambalaje preluate."), note);
        p.setSpacingAfter(8f);
        return p;
    }

    private PdfPTable signature(PackagingAnexa3 d) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        line(t, "Întocmit: ", d.preparedBy());
        line(t, "Funcţia: ", d.preparedByRole());
        return t;
    }

    // --- cells ---

    private void head(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text), columnHead));
        c.setPadding(2f);
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
        c.setPadding(1.6f);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void num(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), body, Element.ALIGN_RIGHT);
    }

    private void numBold(PdfPTable t, BigDecimal value) {
        addCell(t, kg(value), bodyBold, Element.ALIGN_RIGHT);
    }


    private static BigDecimal zeroToNull(BigDecimal value) {
        return value == null || value.signum() == 0 ? null : value;
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
            throw new IllegalStateException("Cannot load the Cp1250 base font", ex);
        }
    }
}
