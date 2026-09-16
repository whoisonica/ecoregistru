package ro.ecoregistru.service.export;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * D1.14 — „Registrul intrărilor și ieșirilor” al depozitului, ca {@code .xlsx}: o linie pe sortiment,
 * cu capul operațiunii repetat pe fiecare, cum îl scot programele de cântar pe care depozitele le
 * folosesc azi. Nu e un formular din lege; e documentul de lucru al depozitului, deci coloanele
 * urmează exportul unui depozit funcțional (15.09.2026), cu trei abateri:
 * <ul>
 *   <li><b>fără oră</b> — decizia proprietarului („nu pune ora la intrare”); ordinea din zi o dă numărul;</li>
 *   <li><b>„Sortiment”</b> în locul unui ID intern, care nu spune nimic cuiva din afara programului;</li>
 *   <li><b>prețul și valoarea</b> doar pentru cine vede prețurile (D1.8), iar codul R/D la ieșiri.</li>
 * </ul>
 * Toate stările intră, cu starea scrisă: un registru din care lipsesc anulările nu arată ce s-a anulat.
 * Persoana fizică apare doar cu numele, fără CNP.
 */
@Component
public class DepotRegisterGenerator {

    static final String TITLE = "Registrul intrărilor și ieșirilor";
    /** Rânduri deasupra antetului: titlul, firma și perioada, un rând liber. */
    static final int HEADER_ROW = 3;

    static final List<String> COLUMNS = List.of(
            "Data", "Tip", "Nr. operațiune", "Nr. comandă", "Depozit", "Rol partener",
            "Client / generator / destinatar", "Sortiment", "Cod deșeu", "Denumire deșeu",
            "Cantitate (kg)", "Mașină", "Șofer / delegat",
            "Brut (kg)", "Tara (kg)", "Neto (kg)", "Final (kg)",
            "Brut operațiune (kg)", "Tara operațiune (kg)", "Cod R/D",
            "Stare", "Data finalizării", "Data anulării", "Anulat de", "Motiv anulare");
    static final List<String> PRICE_COLUMNS = List.of("Preț (lei/kg)", "Valoare (lei)");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    public byte[] xlsx(String company, String period, List<WeighingOperation> operations,
                       Map<UUID, List<WasteMovement>> lines, Map<UUID, String> userNames,
                       boolean pricesVisible) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle kg = wb.createCellStyle();
            kg.setDataFormat(wb.createDataFormat().getFormat("#,##0.###"));
            CellStyle lei = wb.createCellStyle();
            lei.setDataFormat(wb.createDataFormat().getFormat("#,##0.00##"));

            Sheet sheet = wb.createSheet("Registru");
            styled(sheet.createRow(0), 0, TITLE, bold);
            text(sheet.createRow(1), 0, company + " · " + period);
            List<String> columns = new ArrayList<>(COLUMNS);
            if (pricesVisible) {
                columns.addAll(PRICE_COLUMNS);
            }
            Row head = sheet.createRow(HEADER_ROW);
            for (int c = 0; c < columns.size(); c++) {
                styled(head, c, columns.get(c), bold);
            }

            int r = HEADER_ROW + 1;
            for (WeighingOperation o : operations) {
                for (WasteMovement m : lines.getOrDefault(o.getId(), List.of())) {
                    Row x = sheet.createRow(r++);
                    text(x, 0, o.getDate().format(DATE));
                    text(x, 1, o.getType() == WeighingOperationType.IN ? "Intrare" : "Ieșire");
                    number(x, 2, BigDecimal.valueOf(o.getNumber()), kg);
                    text(x, 3, o.getOrderNumber());
                    text(x, 4, o.getWorkPoint().getName());
                    text(x, 5, role(o));
                    text(x, 6, o.getPartner() != null ? o.getPartner().getName()
                            : o.getNaturalPerson() != null ? o.getNaturalPerson().getName() : null);
                    text(x, 7, m.getArticle() == null ? null : m.getArticle().getName());
                    text(x, 8, m.getWasteCode().getCode() + (m.getWasteCode().isHazardous() ? "*" : ""));
                    text(x, 9, m.getWasteCode().getName());
                    number(x, 10, m.getQuantity(), kg);
                    text(x, 11, o.getVehicleRegistration());
                    text(x, 12, o.getDriverName());
                    number(x, 13, m.getGrossKg(), kg);
                    number(x, 14, m.getTareKg(), kg);
                    number(x, 15, m.getNetKg(), kg);
                    number(x, 16, m.getQuantity(), kg);
                    number(x, 17, o.getGrossKg(), kg);
                    number(x, 18, o.getTareKg(), kg);
                    text(x, 19, m.getOperationCode() == null ? null : m.getOperationCode().name());
                    text(x, 20, status(o.getStatus()));
                    text(x, 21, day(o.getFinalizedAt()));
                    text(x, 22, day(o.getCancelledAt()));
                    text(x, 23, o.getCancelledBy() == null ? null : userNames.get(o.getCancelledBy()));
                    text(x, 24, o.getCancelReason());
                    if (pricesVisible) {
                        number(x, 25, m.getUnitPrice(), lei);
                        number(x, 26, m.getTotalValue(), lei);
                    }
                }
            }
            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the depot register (xlsx)", ex);
        }
    }

    /** Cum numește exportul depozitului cele trei roluri. */
    static String role(WeighingOperation o) {
        if (o.getType() == WeighingOperationType.OUT) {
            return "Destinatar";
        }
        if (o.getNaturalPerson() != null) {
            return "Partener PF";
        }
        return o.getPartner() == null ? null : "Client / generator";
    }

    static String status(WeighingOperationStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "În lucru";
            case FINALIZED -> "Finalizată";
            case CANCELLED -> "Anulată";
        };
    }

    private static String day(Instant at) {
        return at == null ? null : at.atZone(BUCHAREST).toLocalDate().format(DATE);
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
}
