package ro.ecoregistru.service.export;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.WasteOperationCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Anexa 3 la Ordinul 794/2012 as the spreadsheet the authority receives.
 *
 * <p><b>One sheet, not two.</b> Art. 4 alin. (1) says the operator reports "tabelul 1 sau, dupa
 * caz, tabelul 2", so printing both would hand the client a file half of which they must delete.
 * Which one comes from the company profile; when it is unanswered nothing is rendered at all and
 * the caller says so, because a document is an assertion (decision 37).
 *
 * <p>Layout from {@code documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods}, the
 * blank tabelul 1 the specialist sent: the nine-line identification header, the band naming the
 * table, the two-level column heading with the numbered band under it, then material blocks in
 * which <b>provenance is a sub-row</b> and each material closes with its own total.
 *
 * <p><b>Two places where the model is not followed, both on the act's authority</b>
 * (regula de lucru 2 - the corpus says how a rubric is filled in, never what the law is):
 * <ul>
 *   <li>the model's headings say <b>(tone)</b>; art. 8 alin. (1) lit. a) says
 *       "se raporteaza in kilograme", so the sheet is in kg like every other packaging form here;</li>
 *   <li>the model has a single merged <b>metal /aluminiu</b> row; the annex has Aluminiu and Otel
 *       as separate material rows, the same rows anexa 1 uses, and
 *       {@link PackagingMaterial} already holds them in the printed order.</li>
 * </ul>
 *
 * <p><b>BIFF8 and protected</b>, for the same two reasons as anexa 1: art. 6 names the format
 * ".xls" and requires it "protejat impotriva modificarii datelor", with the paper copy alongside.
 * The password is deliberately empty so the client can lift the protection to correct something
 * before filing. See {@link PackagingDeclarationXlsGenerator} for the full reasoning.
 *
 * <p>Empty stays empty: a material nobody has movements for is not printed as 0. On a filed form
 * "none" and "not answered" are different statements, and only the client may make either.
 */
@Component
public class PackagingAnexa3XlsGenerator {

    private static final String TITLE = "ANEXA Nr. 3";

    /**
     * The only note of the annex we hold verbatim, and the one the model's column actually
     * references - it prints the provenance header as "Provenienta*2".
     *
     * <p>The other notes are deliberately absent rather than invented. We have the blank tabelul 1
     * and the text of the articles; we do not have the annex's full note block, and writing
     * plausible notes under an official form would be exactly the kind of invention regula de
     * lucru 1 forbids. When the specialist supplies a filled copy, they go in with a citation.
     */
    private static final String NOTE_2 =
            "*2) Se mentioneaza, dupa caz, «populatie», «generator persoana juridica», "
                    + "«colector», «comerciant», in functie de persoanele juridice sau fizice de la "
                    + "care provin deseurile de ambalaje preluate.";

    public byte[] render(PackagingAnexa3 d) {
        if (!d.printable()) {
            throw new IllegalStateException("packaging.operator.role.required");
        }
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles s = new Styles(wb);
            sheet(wb, s, d);
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                wb.getSheetAt(i).protectSheet("");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void sheet(Workbook wb, Styles s, PackagingAnexa3 d) {
        boolean table2 = d.usesTable2();
        Sheet sh = wb.createSheet(table2 ? "Tabelul nr. 2" : "Tabelul nr. 1");

        sh.setColumnWidth(0, 900);
        sh.setColumnWidth(1, 6400);   // material
        sh.setColumnWidth(2, 3800);   // total
        sh.setColumnWidth(3, 3800);   // din care periculoase
        sh.setColumnWidth(4, 5200);   // provenienta
        sh.setColumnWidth(5, 4200);   // cantitate iesita / reciclata
        sh.setColumnWidth(6, 6000);   // operator / valorificata
        sh.setColumnWidth(7, 5200);   // (tabelul 2) metoda

        int last = table2 ? 7 : 6;
        int r = 1;

        put(sh, s.title, r, 1, TITLE + " — " + d.role().tableHeading());
        merge(sh, r, r, 1, last);
        sh.getRow(r).setHeightInPoints(30);
        r += 2;

        r = headerLine(sh, s, r, "Denumirea operatorului economic:", d.companyName(), last);
        r = headerLine(sh, s, r, "Judet si localitate:", d.county(), last);
        r = headerLine(sh, s, r, "Adresa:", d.address(), last);
        r = headerLine(sh, s, r, "Tel/fax/e-mail:", d.contact(), last);
        r = headerLine(sh, s, r, "CUI:", d.cui(), last);
        r = headerLine(sh, s, r,
                "Autorizatie de mediu/nr inregistrare/data/valabilitate:", d.authorization(), last);
        r = headerLine(sh, s, r, "COD CAEN:", d.caenCode(), last);
        r = headerLine(sh, s, r,
                "Anul pentru care se realizeaza raportarea:", String.valueOf(d.year()), last);
        r = headerLine(sh, s, r, "Punct de lucru:", workPoint(d), last);
        r++;

        // Band naming what the left half counts. The model writes it over the quantity columns.
        put(sh, s.head, r, 2, table2
                ? "DESEURI AMBALAJE PRELUATE"
                : "DESEURI AMBALAJE COLECTATE");
        merge(sh, r, r, 2, last);
        r++;

        r = columnHeadings(sh, s, r, table2, last);
        r = body(sh, s, r, d, table2, last);
        r++;

        put(sh, s.note, r, 1, NOTE_2);
        merge(sh, r, r, 1, last);
        sh.getRow(r).setHeightInPoints(26);
        r += 2;

        r = headerLine(sh, s, r, "Intocmit:", d.preparedBy(), last);
        headerLine(sh, s, r, "Functia:", d.preparedByRole(), last);
    }

    // ---------------------------------------------------------------- headings

    /**
     * The two-level column band of the model, then the numbered row under it. The numbering is not
     * decoration: the annex's own notes refer to columns by these numbers, and so does art. 8
     * alin. (1) lit. d) when it says "coloana 0".
     */
    private int columnHeadings(Sheet sh, Styles s, int r, boolean table2, int last) {
        String quantityGroup = table2
                ? "Cantitatea de deseuri de ambalaje preluata (kg)"
                : "Cantitatea de deseuri de ambalaje colectata (kg)";

        put(sh, s.head, r, 1, "Material");
        merge(sh, r, r + 1, 1, 1);
        put(sh, s.head, r, 2, quantityGroup);
        merge(sh, r, r, 2, 3);
        put(sh, s.head, r, 4, "Provenienta*2");
        merge(sh, r, r + 1, 4, 4);

        if (table2) {
            put(sh, s.head, r, 5, "Deseuri de ambalaje valorificate (kg)");
            merge(sh, r, r, 5, 6);
            put(sh, s.head, r, 7, "Metoda");
            merge(sh, r, r + 1, 7, 7);
        } else {
            put(sh, s.head, r, 5,
                    "Deseuri de ambalaje comercializate/trimise la reciclare/valorificare/exportate");
            merge(sh, r, r, 5, 6);
        }
        sh.getRow(r).setHeightInPoints(34);
        r++;

        put(sh, s.head, r, 2, "Total");
        put(sh, s.head, r, 3, "din care periculoase");
        if (table2) {
            put(sh, s.head, r, 5, "cantitatea reciclata");
            put(sh, s.head, r, 6, "cantitatea valorificata prin alte metode");
        } else {
            put(sh, s.head, r, 5, "cantitatea (kg)");
            put(sh, s.head, r, 6, "Operatorul economic");
        }
        sh.getRow(r).setHeightInPoints(30);
        r++;

        for (int c = 1; c <= last; c++) {
            put(sh, s.cellCenter, r, c, String.valueOf(c - 1));
        }
        return r + 1;
    }

    // ---------------------------------------------------------------- body

    /**
     * One block per material that has anything in it, then the two group sums and the grand total.
     *
     * <p>The left half has one line per provenance and the right half one line per operator (or one
     * line for the material, on tabelul 2). The two halves rarely have the same number of lines, so
     * the block is as tall as the taller of them and the shorter one simply stops - which is what
     * the paper form does, the material's rubric being a box rather than a row.
     *
     * <p>A material with no movements at all is skipped rather than printed as a row of zeros.
     */
    private int body(Sheet sh, Styles s, int r, PackagingAnexa3 d, boolean table2, int last) {

        for (PackagingMaterial material : PackagingMaterial.values()) {
            List<PackagingAnexa3.IntakeRow> intake = d.intake().stream()
                    .filter(x -> x.material() == material).toList();
            List<PackagingAnexa3.HandoverRow> handovers = d.handovers().stream()
                    .filter(x -> x.material() == material).toList();
            PackagingAnexa3.TreatmentRow treatment = d.treatments().stream()
                    .filter(x -> x.material() == material).findFirst().orElse(null);
            int rightLines = table2 ? (treatment == null ? 0 : 1) : handovers.size();

            if (intake.isEmpty() && rightLines == 0) {
                // Skipped, but the group sum below still has to run: "total plastic" is owed
                // whenever PET has figures, even if Alte plastice has none.
                r = groupSum(sh, s, r, d, material, table2, last);
                continue;
            }

            int blockStart = r;

            // Stacked, not paired: the two halves have different cardinalities, and putting an
            // operator's kilograms on the same row as a provenance would read as if that is where
            // the load came from. On paper the material's rubric is a box; a bordered row here
            // looks like a statement, so each line carries one half only.
            for (PackagingAnexa3.IntakeRow row : intake) {
                number(sh, s.number, r, 2, row.total());
                number(sh, s.number, r, 3, zeroToNull(row.hazardous()));
                put(sh, s.cell, r, 4, row.origin().getOfficialLabel());
                blank(sh, s.cell, r, 5, last);
                r++;
            }

            // The two tables are written out separately rather than through one shared row writer.
            // Sharing it meant passing every cell as a String, which turned tabelul 2's second
            // figure into a text cell Excel will not sum - caught by reading the rendered sheet.
            if (table2) {
                if (treatment != null) {
                    blank(sh, s.cell, r, 2, 4);
                    number(sh, s.number, r, 5, treatment.recycled());
                    number(sh, s.number, r, 6, treatment.otherRecovery());
                    put(sh, s.cellCenter, r, 7, methods(treatment));
                    r++;
                }
            } else {
                for (PackagingAnexa3.HandoverRow h : handovers) {
                    blank(sh, s.cell, r, 2, 4);
                    number(sh, s.number, r, 5, h.quantity());
                    put(sh, s.cell, r, 6, operator(h));
                    r++;
                }
            }

            put(sh, s.label, blockStart, 1, material.getOfficialLabel());
            merge(sh, blockStart, r - 1, 1, 1);

            // No line of its own for a material that "total plastic" or "total metal" already
            // closes: the same kilograms under two labels read as two quantities.
            if (!material.isSummedIntoAGroup()) {
                totalLine(sh, s, r, "total " + material.getOfficialLabel().toLowerCase(),
                        d.totalsFor(material), table2, last);
                r++;
            }

            r = groupSum(sh, s, r, d, material, table2, last);
        }

        totalLine(sh, s, r, "TOTAL ambalaje", d.grandTotals(), table2, last);
        return r + 1;
    }

    /**
     * A summed line: the label, the intake total, and whichever right-hand totals the chosen table
     * actually has. On tabelul 1 col. 6 holds an operator's name, so there is nothing to total
     * there and the cell stays empty; on tabelul 2 it is the second figure and it does get one.
     */
    private void totalLine(Sheet sh, Styles s, int r, String label,
                           PackagingAnexa3.Totals totals, boolean table2, int last) {
        put(sh, s.labelBold, r, 1, label);
        number(sh, s.numberBold, r, 2, totals.intake());
        // "din care periculoase" is a sub-column of the total beside it, so it gets summed too.
        // Leaving it blank while its own detail lines show figures made the document contradict
        // itself - caught by reading the rendered sheet, not by a test.
        number(sh, s.numberBold, r, 3, zeroToNull(totals.hazardous()));
        blank(sh, s.cell, r, 4, 4);
        number(sh, s.numberBold, r, 5, zeroToNull(totals.first()));
        if (table2) {
            number(sh, s.numberBold, r, 6, zeroToNull(totals.second()));
            blank(sh, s.cell, r, 7, last);
        } else {
            blank(sh, s.cell, r, 6, last);
        }
    }

    /**
     * "total plastic" and "total metal", printed after the last of their parts - the two summed
     * rows the annex draws between the material rows. Sticla, hartie carton, lemn and altele stand
     * on their own and get no such line.
     */
    private int groupSum(Sheet sh, Styles s, int r, PackagingAnexa3 d,
                         PackagingMaterial justPrinted, boolean table2, int last) {
        List<PackagingMaterial> group = null;
        String label = null;
        if (justPrinted == PackagingMaterial.ALTE_PLASTICE) {
            group = PackagingMaterial.plasticParts();
            label = "total plastic";
        } else if (justPrinted == PackagingMaterial.OTEL) {
            group = PackagingMaterial.metalParts();
            label = "total metal";
        }
        if (group == null) {
            return r;
        }

        // A group with nothing in either part prints no total line at all, the same way an empty
        // material prints no block.
        PackagingAnexa3.Totals totals = d.totalsOver(group);
        if (totals.isEmpty()) {
            return r;
        }

        totalLine(sh, s, r, label, totals, table2, last);
        return r + 1;
    }

    /** "Numele (CUI)" for the operator column of tabelul 1; just the name when there is no CUI. */
    private String operator(PackagingAnexa3.HandoverRow h) {
        String name = nvl(h.operatorName());
        return h.operatorCui() == null || h.operatorCui().isBlank()
                ? name
                : name + " (" + h.operatorCui() + ")";
    }

    /** The R codes behind the two figures of tabelul 2, in the order they appeared. */
    private String methods(PackagingAnexa3.TreatmentRow treatment) {
        return treatment.methods().stream().map(WasteOperationCode::name)
                .reduce((a, b) -> a + ", " + b).orElse("");
    }


    // ---------------------------------------------------------------- cells

    private int headerLine(Sheet sh, Styles s, int r, String label, String value, int last) {
        put(sh, s.plain, r, 1, label);
        put(sh, s.plainBold, r, 2, nvl(value));
        merge(sh, r, r, 2, last);
        return r + 1;
    }

    private String workPoint(PackagingAnexa3 d) {
        if (d.workPointName() == null) {
            return "";
        }
        return d.workPointAddress() == null || d.workPointAddress().isBlank()
                ? d.workPointName()
                : d.workPointName() + " — " + d.workPointAddress();
    }

    /** Draws the border on cells that stay empty, so the grid does not break mid-block. */
    private void blank(Sheet sh, CellStyle style, int r, int from, int to) {
        for (int c = from; c <= to; c++) {
            put(sh, style, r, c, "");
        }
    }

    private void number(Sheet sh, CellStyle style, int r, int c, BigDecimal value) {
        Cell cell = cellAt(sh, r, c);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    /** Zero and absent are different on this form; a computed zero prints as an empty cell. */
    private BigDecimal zeroToNull(BigDecimal value) {
        return value == null || value.signum() == 0 ? null : value;
    }

    private void put(Sheet sh, CellStyle style, int r, int c, String value) {
        Cell cell = cellAt(sh, r, c);
        cell.setCellStyle(style);
        cell.setCellValue(nvl(value));
    }

    private void merge(Sheet sh, int r1, int r2, int c1, int c2) {
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                cellAt(sh, r, c);
            }
        }
        if (r1 != r2 || c1 != c2) {
            sh.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    private Cell cellAt(Sheet sh, int r, int c) {
        Row row = sh.getRow(r);
        if (row == null) {
            row = sh.createRow(r);
        }
        Cell cell = row.getCell(c);
        return cell == null ? row.createCell(c) : cell;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    /** One place for the handful of looks the sheet needs, built once per workbook. */
    private static final class Styles {
        final CellStyle title;
        final CellStyle plain;
        final CellStyle plainBold;
        final CellStyle head;
        final CellStyle label;
        final CellStyle labelBold;
        final CellStyle cell;
        final CellStyle cellCenter;
        final CellStyle number;
        final CellStyle numberBold;
        final CellStyle note;

        Styles(Workbook wb) {
            Font bold = wb.createFont();
            bold.setBold(true);
            Font small = wb.createFont();
            small.setFontHeightInPoints((short) 8);

            title = wb.createCellStyle();
            title.setFont(bold);
            title.setWrapText(true);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            plain = wb.createCellStyle();
            plain.setVerticalAlignment(VerticalAlignment.CENTER);

            plainBold = wb.createCellStyle();
            plainBold.setFont(bold);
            plainBold.setVerticalAlignment(VerticalAlignment.CENTER);

            head = bordered(wb);
            head.setFont(bold);
            head.setWrapText(true);
            head.setAlignment(HorizontalAlignment.CENTER);
            head.setVerticalAlignment(VerticalAlignment.CENTER);

            label = bordered(wb);
            label.setVerticalAlignment(VerticalAlignment.CENTER);

            labelBold = bordered(wb);
            labelBold.setFont(bold);
            labelBold.setVerticalAlignment(VerticalAlignment.CENTER);

            cell = bordered(wb);
            cell.setWrapText(true);
            cell.setVerticalAlignment(VerticalAlignment.CENTER);

            cellCenter = bordered(wb);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);
            cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            short kg = wb.createDataFormat().getFormat("#,##0.###");
            number = bordered(wb);
            number.setDataFormat(kg);
            number.setAlignment(HorizontalAlignment.RIGHT);

            numberBold = bordered(wb);
            numberBold.setDataFormat(kg);
            numberBold.setAlignment(HorizontalAlignment.RIGHT);
            numberBold.setFont(bold);

            note = wb.createCellStyle();
            note.setFont(small);
            note.setWrapText(true);
            note.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private static CellStyle bordered(Workbook wb) {
            CellStyle style = wb.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
