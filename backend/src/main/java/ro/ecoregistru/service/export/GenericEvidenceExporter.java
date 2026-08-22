package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Produces a downloadable, human-readable summary of the monthly evidence — a "generic table",
 * NOT the official Anexa 1 form (that is blocked on the environmental expert, see
 * docs/legislatie.md §4). The header states explicitly that this is an unofficial summary so
 * nobody mistakes it for a regulated report. All quantities are printed in KG.
 */
@Component
public class GenericEvidenceExporter {

    private static final String TITLE = "Evidența gestiunii deșeurilor — rezumat generic (neoficial)";
    // Column order follows Anexa 1 cap. 1 — generated, of which recovered / disposed, in stock —
    // even though the table itself is unofficial. "din care predat" is a memo inside the two
    // treatment columns, and "neclasificat" is what left with no operation code, hence outside
    // both. Goods taken over from third parties are not here at all: they are the art. 48 register.
    private static final String[] COLUMNS = {
            "Punct de lucru", "Luna", "Cod", "Denumire", "Periculos",
            "Generat (kg)", "Valorificat (kg)", "Eliminat (kg)", "din care predat (kg)",
            "Neclasificat (kg)", "Stoc (kg)"
    };
    private static final String[] MONTHS_RO = {
            "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"
    };

    public byte[] export(ExportFormat format, String companyName, int year, Integer month,
                         List<MonthlyEvidenceResponse> rows) {
        return switch (format) {
            case XLSX -> toXlsx(companyName, year, month, rows);
            case PDF -> toPdf(companyName, year, month, rows);
        };
    }

    /** Subtitle line, e.g. "Anul 2026 · Luna Iulie" or just "Anul 2026". */
    private String subtitle(int year, Integer month) {
        String s = "Anul " + year;
        if (month != null && month >= 1 && month <= 12) {
            s += " · Luna " + MONTHS_RO[month - 1];
        }
        return s;
    }

    private static String monthName(int month) {
        return (month >= 1 && month <= 12) ? MONTHS_RO[month - 1] : String.valueOf(month);
    }

    private static double kg(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    // --- XLSX (Apache POI) ---

    private byte[] toXlsx(String companyName, int year, Integer month, List<MonthlyEvidenceResponse> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Evidență");

            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle numberStyle = wb.createCellStyle();
            numberStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.###"));

            int r = 0;
            cell(sheet.createRow(r++), 0, companyName, titleStyle);
            cell(sheet.createRow(r++), 0, TITLE, titleStyle);
            cell(sheet.createRow(r++), 0, subtitle(year, month), null);
            r++; // spacer row

            Row headerRow = sheet.createRow(r++);
            for (int c = 0; c < COLUMNS.length; c++) {
                cell(headerRow, c, COLUMNS[c], headerStyle);
            }

            for (MonthlyEvidenceResponse e : rows) {
                Row row = sheet.createRow(r++);
                cell(row, 0, e.workPointName(), null);
                cell(row, 1, monthName(e.month()), null);
                cell(row, 2, e.wasteCode(), null);
                cell(row, 3, e.wasteCodeName(), null);
                cell(row, 4, e.hazardous() ? "Da" : "Nu", null);
                numberCell(row, 5, kg(e.totalGenerated()), numberStyle);
                numberCell(row, 6, kg(e.totalRecovered()), numberStyle);
                numberCell(row, 7, kg(e.totalDisposed()), numberStyle);
                numberCell(row, 8, kg(e.totalHandedOver()), numberStyle);
                numberCell(row, 9, kg(e.totalUnclassifiedOut()), numberStyle);
                numberCell(row, 10, kg(e.closingStock()), numberStyle);
            }

            for (int c = 0; c < COLUMNS.length; c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build XLSX evidence export", ex);
        }
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void numberCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // --- PDF (OpenPDF) ---

    private byte[] toPdf(String companyName, int year, Integer month, List<MonthlyEvidenceResponse> rows) {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font companyFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font titleFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font subFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            doc.add(new Paragraph(companyName == null ? "" : companyName, companyFont));
            doc.add(new Paragraph(TITLE, titleFont));
            Paragraph sub = new Paragraph(subtitle(year, month), subFont);
            sub.setSpacingAfter(10f);
            doc.add(sub);

            PdfPTable table = new PdfPTable(COLUMNS.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{16, 9, 8, 22, 8, 10, 10, 10, 11, 10, 10});

            Font headFont = new Font(Font.HELVETICA, 8, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            for (String col : COLUMNS) {
                PdfPCell hc = new PdfPCell(new Phrase(col, headFont));
                hc.setBackgroundColor(new java.awt.Color(0xEC, 0xFD, 0xF5)); // emerald-50
                hc.setPadding(4f);
                table.addCell(hc);
            }

            for (MonthlyEvidenceResponse e : rows) {
                textCell(table, e.workPointName(), bodyFont, Element.ALIGN_LEFT);
                textCell(table, monthName(e.month()), bodyFont, Element.ALIGN_LEFT);
                textCell(table, e.wasteCode(), bodyFont, Element.ALIGN_LEFT);
                textCell(table, e.wasteCodeName(), bodyFont, Element.ALIGN_LEFT);
                textCell(table, e.hazardous() ? "Da" : "Nu", bodyFont, Element.ALIGN_CENTER);
                numCell(table, kg(e.totalGenerated()), bodyFont);
                numCell(table, kg(e.totalRecovered()), bodyFont);
                numCell(table, kg(e.totalDisposed()), bodyFont);
                numCell(table, kg(e.totalHandedOver()), bodyFont);
                numCell(table, kg(e.totalUnclassifiedOut()), bodyFont);
                numCell(table, kg(e.closingStock()), bodyFont);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build PDF evidence export", ex);
        }
    }

    private static void textCell(PdfPTable table, String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setPadding(3f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static final java.text.DecimalFormat KG_FMT = buildKgFormat();

    private static java.text.DecimalFormat buildKgFormat() {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.of("ro", "RO"));
        return new java.text.DecimalFormat("#,##0.###", symbols);
    }

    private static void numCell(PdfPTable table, double value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(KG_FMT.format(value), font));
        cell.setPadding(3f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
