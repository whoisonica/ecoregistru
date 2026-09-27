package ro.ecoregistru.service.export;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * D4.7 — un {@link DepotReport} ca {@code .xlsx}: o foaie, titlul și rândurile de sub el, apoi secțiunile una sub alta
 * (titlu îngroșat, antet, rânduri, total), notele la capăt. Numerele rămân numere, ca în Excel să se poată aduna.
 */
@Component
public class DepotReportXlsx {

    public byte[] render(DepotReport report) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle kg = number(wb, "#,##0.###", null);
            CellStyle lei = number(wb, "#,##0.00", null);
            CellStyle integer = number(wb, "0", null);
            CellStyle kgBold = number(wb, "#,##0.###", font);
            CellStyle leiBold = number(wb, "#,##0.00", font);
            CellStyle integerBold = number(wb, "0", font);

            Sheet sheet = wb.createSheet("Raport");
            int r = 0;
            write(sheet.createRow(r++), 0, report.title(), bold);
            for (String line : report.heading()) {
                write(sheet.createRow(r++), 0, line, null);
            }
            int widest = 1;
            for (DepotReport.Section section : report.sections()) {
                r++;
                if (section.title() != null) {
                    write(sheet.createRow(r++), 0, section.title(), bold);
                }
                List<DepotReport.Column> columns = section.columns();
                widest = Math.max(widest, columns.size());
                Row head = sheet.createRow(r++);
                for (int c = 0; c < columns.size(); c++) {
                    write(head, c, columns.get(c).name(), bold);
                }
                if (section.rows().isEmpty()) {
                    write(sheet.createRow(r++), 0, section.empty(), null);
                }
                for (List<Object> values : section.rows()) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < values.size(); c++) {
                        write(row, c, values.get(c), style(columns.get(c).kind(), kg, lei, integer));
                    }
                }
                if (section.total() != null) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < section.total().size(); c++) {
                        Object value = section.total().get(c);
                        write(row, c, value, value instanceof String ? bold
                                : style(columns.get(c).kind(), kgBold, leiBold, integerBold));
                    }
                }
            }
            if (!report.notes().isEmpty()) {
                r++;
                for (String note : report.notes()) {
                    write(sheet.createRow(r++), 0, note, null);
                }
            }
            for (int c = 0; c < widest; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the depot report (xlsx)", ex);
        }
    }

    private static CellStyle number(Workbook wb, String format, Font font) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat(format));
        if (font != null) {
            style.setFont(font);
        }
        return style;
    }

    private static CellStyle style(DepotReport.Kind kind, CellStyle kg, CellStyle lei, CellStyle integer) {
        return switch (kind) {
            case KG -> kg;
            case LEI -> lei;
            case INT -> integer;
            case TEXT -> null;
        };
    }

    private static void write(Row row, int col, Object value, CellStyle style) {
        if (value == null) {
            return;
        }
        var cell = row.createCell(col);
        if (value instanceof BigDecimal number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Integer number) {
            cell.setCellValue(number);
        } else {
            cell.setCellValue(value.toString());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
