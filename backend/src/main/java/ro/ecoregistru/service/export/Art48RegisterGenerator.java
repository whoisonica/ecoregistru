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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prints {@link Art48Register} as {@code .xlsx} (to work from and copy into the portal) and as PDF
 * (to hand to an inspector). Both carry the same four tables in the same order.
 *
 * <p>The header says what the document is and what it is not: a table with the content of art. 48
 * alin. (1), not an official form, because the act has none.
 */
@Component
public class Art48RegisterGenerator {

    static final String TITLE = "Evidența cronologică lunară a deșeurilor preluate de la terți";
    static final String BASIS = "OUG nr. 92/2021, art. 48 alin. (1): evidență cronologică lunară, în format tabelar, "
            + "cu conținutul de la lit. a)–c). Actul nu prevede un formular.";
    static final String SIM_NOTE = "Totalurile anuale urmează chestionarul SIM „Colectare/Tratare”, în tone. "
            + "Portalul cere în plus „sursa colectării”, aleasă din lista lui, pe care ghidul nu o tipărește; "
            + "coloana „Originea” din evidența cronologică arată de la cine s-a preluat.";
    static final String STOCK_NOTE = "Stocul la începutul anului se calculează din mișcările înregistrate în aplicație "
            + "în anii anteriori.";

    static final String SECTION_CHRONO = "1. Evidența cronologică";
    static final String SECTION_CAP1 = "2. Cap. 1 — Colectarea deșeurilor (totaluri pe an)";
    static final String SECTION_CAP2A = "3. Cap. 2 A — Valorificarea deșeurilor colectate";
    static final String SECTION_CAP2B = "4. Cap. 2 B — Eliminarea deșeurilor colectate";

    static final String[] CHRONO_COLUMNS = {
            "Luna", "Data", "Punct de lucru", "Operațiunea", "Cod deșeu", "Denumire",
            "Cantitate (kg)", "Cantitate (t)", "Partener", "CUI partener", "Originea", "Cod R/D",
            "Mod de transport", "Metoda de tratare", "Document"
    };
    static final String[] CAP1_COLUMNS = {
            "Cod deșeu", "Denumire", "Stoc la începutul anului (t)", "Cantitate colectată (t)",
            "Valorificată din colectat (t)", "Eliminată din colectat (t)", "Ieșită fără cod R/D (t)",
            "Stoc la sfârșitul anului (t)", "Coduri R", "Coduri D"
    };
    static final String[] CAP2A_COLUMNS = {
            "Unitatea care preia", "CUI", "Adresa", "Cod deșeu", "Denumire", "Cantitate preluată (t)", "Cod R"
    };
    static final String[] CAP2B_COLUMNS = {
            "Unitatea care preia", "CUI", "Adresa", "Cod deșeu", "Denumire", "Cantitate preluată (t)", "Cod D"
    };

    /** Rows above the column headers on every sheet: company, title, period, spacer. */
    static final int XLSX_HEADER_ROW = 4;

    private static final String[] MONTHS = {
            "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"
    };
    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // ------------------------------------------------------------------ xlsx

    public byte[] xlsx(Art48Register r) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles s = new Styles(wb);

            Sheet chrono = sheet(wb, s, "Cronologic", r, CHRONO_COLUMNS);
            int row = XLSX_HEADER_ROW + 1;
            for (Art48Register.Entry e : r.entries()) {
                Row x = chrono.createRow(row++);
                text(x, 0, MONTHS[e.date().getMonthValue() - 1]);
                text(x, 1, e.date().format(DATE));
                text(x, 2, e.workPoint());
                text(x, 3, e.operation());
                text(x, 4, e.wasteCode());
                text(x, 5, e.wasteName());
                number(x, 6, e.kg(), s.kg);
                number(x, 7, tons(e.kg()), s.tons);
                text(x, 8, e.partner());
                text(x, 9, e.partnerCui());
                text(x, 10, e.origin());
                text(x, 11, e.operationCode());
                text(x, 12, e.transport());
                text(x, 13, e.treatment());
                text(x, 14, e.document());
            }
            row++;
            for (String note : notes(r)) {
                text(chrono.createRow(row++), 0, note);
            }

            Sheet cap1 = sheet(wb, s, "Cap. 1 Colectare", r, CAP1_COLUMNS);
            row = XLSX_HEADER_ROW + 1;
            for (Art48Register.CodeTotal c : r.collection()) {
                Row x = cap1.createRow(row++);
                text(x, 0, c.wasteCode());
                text(x, 1, c.wasteName());
                number(x, 2, tons(c.openingKg()), s.tons);
                number(x, 3, tons(c.collectedKg()), s.tons);
                number(x, 4, tons(c.recoveredKg()), s.tons);
                number(x, 5, tons(c.disposedKg()), s.tons);
                number(x, 6, tons(c.unclassifiedKg()), s.tons);
                number(x, 7, tons(c.closingKg()), s.tons);
                text(x, 8, String.join(", ", c.recoveryCodes()));
                text(x, 9, String.join(", ", c.disposalCodes()));
            }

            handoverSheet(wb, s, "Cap. 2A Valorificare", r, CAP2A_COLUMNS, r.recovery());
            handoverSheet(wb, s, "Cap. 2B Eliminare", r, CAP2B_COLUMNS, r.disposal());

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sh = wb.getSheetAt(i);
                for (int c = 0; c < sh.getRow(XLSX_HEADER_ROW).getLastCellNum(); c++) {
                    sh.autoSizeColumn(c);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the art. 48 register (xlsx)", ex);
        }
    }

    private void handoverSheet(Workbook wb, Styles s, String name, Art48Register r, String[] columns,
                               List<Art48Register.Handover> rows) {
        Sheet sh = sheet(wb, s, name, r, columns);
        int row = XLSX_HEADER_ROW + 1;
        for (Art48Register.Handover h : rows) {
            Row x = sh.createRow(row++);
            text(x, 0, h.recipient());
            text(x, 1, h.cui());
            text(x, 2, h.address());
            text(x, 3, h.wasteCode());
            text(x, 4, h.wasteName());
            number(x, 5, tons(h.kg()), s.tons);
            text(x, 6, h.operationCode());
        }
    }

    private Sheet sheet(Workbook wb, Styles s, String name, Art48Register r, String[] columns) {
        Sheet sh = wb.createSheet(name);
        styled(sh.createRow(0), 0, company(r), s.bold);
        styled(sh.createRow(1), 0, TITLE, s.bold);
        text(sh.createRow(2), 0, period(r));
        Row head = sh.createRow(XLSX_HEADER_ROW);
        for (int c = 0; c < columns.length; c++) {
            styled(head, c, columns[c], s.bold);
        }
        return sh;
    }

    private static void text(Row row, int col, String value) {
        if (value != null) {
            row.createCell(col).setCellValue(value);
        }
    }

    private static void styled(Row row, int col, String value, CellStyle style) {
        row.createCell(col).setCellValue(value);
        row.getCell(col).setCellStyle(style);
    }

    private static void number(Row row, int col, BigDecimal value, CellStyle style) {
        if (value != null) {
            row.createCell(col).setCellValue(value.doubleValue());
            row.getCell(col).setCellStyle(style);
        }
    }

    private static final class Styles {
        final CellStyle bold;
        final CellStyle kg;
        final CellStyle tons;

        Styles(Workbook wb) {
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true);
            bold = wb.createCellStyle();
            bold.setFont(font);
            kg = wb.createCellStyle();
            kg.setDataFormat(wb.createDataFormat().getFormat("#,##0.###"));
            tons = wb.createCellStyle();
            tons.setDataFormat(wb.createDataFormat().getFormat("#,##0.000###"));
        }
    }

    // ------------------------------------------------------------------ pdf

    public byte[] pdf(Art48Register r) {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont plain = centralEuropean(false);
            BaseFont boldBase = centralEuropean(true);
            Font title = new Font(boldBase, 11f);
            Font heading = new Font(boldBase, 9f);
            Font head = new Font(boldBase, 6.5f);
            Font body = new Font(plain, 6.5f);
            Font small = new Font(plain, 7f);

            doc.add(new Paragraph(cp1250(company(r)), heading));
            doc.add(new Paragraph(cp1250(TITLE), title));
            doc.add(new Paragraph(cp1250(period(r)), small));
            Paragraph basis = new Paragraph(cp1250(BASIS), small);
            basis.setSpacingAfter(8f);
            doc.add(basis);

            // 1. Chronological, one block per month. The month is the heading, so it is not a column.
            section(doc, SECTION_CHRONO, heading);
            String[] chronoColumns = java.util.Arrays.copyOfRange(CHRONO_COLUMNS, 1, CHRONO_COLUMNS.length);
            if (r.entries().isEmpty()) {
                doc.add(new Paragraph(cp1250("Nicio mișcare în registru în anul acesta."), small));
            } else {
                PdfPTable t = table(chronoColumns, head, new float[]{6, 9, 9, 6, 15, 6, 6, 11, 7, 7, 4, 7, 7, 7});
                int month = 0;
                for (Art48Register.Entry e : r.entries()) {
                    if (e.date().getMonthValue() != month) {
                        month = e.date().getMonthValue();
                        PdfPCell m = new PdfPCell(new Phrase(MONTHS[month - 1] + " " + r.year(), head));
                        m.setColspan(chronoColumns.length);
                        m.setPadding(3f);
                        m.setBackgroundColor(new java.awt.Color(0xF1, 0xF5, 0xF9));
                        t.addCell(m);
                    }
                    cells(t, body, e.date().format(DATE), e.workPoint(), e.operation(), e.wasteCode(), e.wasteName());
                    numbers(t, body, kgText(e.kg()), tonsText(e.kg()));
                    cells(t, body, e.partner(), e.partnerCui(), e.origin(), e.operationCode(), e.transport(), e.treatment(), e.document());
                }
                doc.add(t);
            }

            section(doc, SECTION_CAP1, heading);
            if (r.collection().isEmpty()) {
                doc.add(new Paragraph(cp1250("Nicio cantitate de raportat."), small));
            } else {
                PdfPTable t = table(CAP1_COLUMNS, head, new float[]{7, 22, 9, 9, 9, 9, 9, 9, 8, 8});
                for (Art48Register.CodeTotal c : r.collection()) {
                    cells(t, body, c.wasteCode(), c.wasteName());
                    numbers(t, body, tonsText(c.openingKg()), tonsText(c.collectedKg()), tonsText(c.recoveredKg()),
                            tonsText(c.disposedKg()), tonsText(c.unclassifiedKg()), tonsText(c.closingKg()));
                    cells(t, body, String.join(", ", c.recoveryCodes()), String.join(", ", c.disposalCodes()));
                }
                doc.add(t);
            }

            handoverSection(doc, SECTION_CAP2A, CAP2A_COLUMNS, r.recovery(), heading, head, body, small);
            handoverSection(doc, SECTION_CAP2B, CAP2B_COLUMNS, r.disposal(), heading, head, body, small);

            Paragraph gap = new Paragraph(" ", small);
            doc.add(gap);
            for (String note : notes(r)) {
                doc.add(new Paragraph(cp1250(note), small));
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the art. 48 register (pdf)", ex);
        }
    }

    private void handoverSection(Document doc, String name, String[] columns, List<Art48Register.Handover> rows,
                                 Font heading, Font head, Font body, Font small) {
        section(doc, name, heading);
        if (rows.isEmpty()) {
            doc.add(new Paragraph(cp1250("Nicio predare."), small));
            return;
        }
        PdfPTable t = table(columns, head, new float[]{18, 9, 22, 7, 24, 10, 6});
        for (Art48Register.Handover h : rows) {
            cells(t, body, h.recipient(), h.cui(), h.address(), h.wasteCode(), h.wasteName());
            numbers(t, body, tonsText(h.kg()));
            cells(t, body, h.operationCode());
        }
        doc.add(t);
    }

    private static void section(Document doc, String name, Font font) {
        Paragraph p = new Paragraph(cp1250(name), font);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(4f);
        doc.add(p);
    }

    private static PdfPTable table(String[] columns, Font font, float[] widths) {
        PdfPTable t = new PdfPTable(columns.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setHeaderRows(1);
        for (String c : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), font));
            cell.setPadding(3f);
            cell.setBackgroundColor(new java.awt.Color(0xEC, 0xFD, 0xF5));
            t.addCell(cell);
        }
        return t;
    }

    private static void cells(PdfPTable t, Font font, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(v), font));
            cell.setPadding(2.5f);
            t.addCell(cell);
        }
    }

    private static void numbers(PdfPTable t, Font font, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v, font));
            cell.setPadding(2.5f);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(cell);
        }
    }

    // ------------------------------------------------------------------ shared

    static List<String> notes(Art48Register r) {
        List<String> notes = new ArrayList<>(List.of(SIM_NOTE, STOCK_NOTE));
        if (r.unweighed() > 0) {
            notes.add(r.unweighed() == 1
                    ? "O mișcare fără cantitate (cântarul nu a venit) apare în evidența cronologică, dar nu în totaluri."
                    : r.unweighed() + " mișcări fără cantitate (cântarul nu a venit) apar în evidența cronologică, "
                            + "dar nu în totaluri.");
        }
        return notes;
    }

    private static String company(Art48Register r) {
        return r.companyCui() == null ? r.companyName() : r.companyName() + " · CUI " + r.companyCui();
    }

    private static String period(Art48Register r) {
        return "Anul " + r.year() + " · " + (r.workPointName() == null ? "toate punctele de lucru" : "Punct de lucru: " + r.workPointName());
    }

    static BigDecimal tons(BigDecimal kg) {
        return kg == null ? null : kg.divide(KG_PER_TON, MathContext.DECIMAL64);
    }

    private static String kgText(BigDecimal kg) {
        return kg == null ? "" : format("#,##0.###").format(kg);
    }

    private static String tonsText(BigDecimal kg) {
        return kg == null ? "" : format("#,##0.000###").format(tons(kg));
    }

    private static DecimalFormat format(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.of("ro", "RO")));
    }

    /** Cp1250 has the cedilla forms of ş/ţ only; the comma-below ones would drop out of the PDF. */
    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
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
