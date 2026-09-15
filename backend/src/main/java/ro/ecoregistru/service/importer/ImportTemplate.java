package ro.ecoregistru.service.importer;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ro.ecoregistru.enums.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.*;

/**
 * P2.15 — şablonul de import, şi tot ce ştie despre coloanele lui.
 *
 * <p>Un singur şablon, al nostru (decizia proprietarului, 15.09.2026), nu o potrivire de coloane pe
 * Excelul fiecărui client: implementarea o facem noi, deci cine copiază datele în el e omul nostru.
 * Coloanele sunt pe poziţii fixe, iar antetul se verifică la citire — un fişier cu alt antet e
 * refuzat întreg, nu citit pe jumătate cu coloanele alunecate.
 *
 * <p>Etichetele listelor sunt cele de pe ecran ({@code strings.ts}), ca omul să nu înveţe două
 * vocabulare. La citire se acceptă şi numele din enum şi textul fără diacritice.
 */
@Component
public class ImportTemplate {

    public static final String PARTNERS = "Parteneri";
    public static final String MOVEMENTS = "Mișcări";
    static final String INSTRUCTIONS = "Instrucțiuni";

    static final List<String> PARTNER_COLUMNS = List.of(
            "Denumire *", "CUI", "Tip", "Client", "Furnizor", "Transportator",
            "Nr. autorizație", "Autorizația expiră la", "Adresă", "Nr. Registrul Comerțului");

    static final List<String> MOVEMENT_COLUMNS = List.of(
            "Data *", "Punct de lucru *", "Cod deșeu *", "Operațiune *", "Cantitate *", "UM *",
            "Cod R/D", "Registru", "Partener (CUI sau denumire)", "Nr. document", "Stare fizică",
            "Tip stocare", "Mod tratare", "Mijloc de transport", "Destinație", "Observații");

    static final Map<String, PartnerType> PARTNER_TYPES = labels(
            "Generator", PartnerType.GENERATOR, "Colector", PartnerType.COLLECTOR,
            "Valorificator", PartnerType.RECOVERER);

    static final Map<String, Boolean> YES_NO = labels("Da", true, "Nu", false);

    /** Fără „Ieşire neclasificată": nu se alege nici pe ecran ({@code WasteOperation#isSelectable}). */
    static final Map<String, WasteOperation> OPERATIONS = labels(
            "Generare", WasteOperation.GENERATED, "Preluare de la terți", WasteOperation.COLLECTED,
            "Valorificare", WasteOperation.RECOVERED, "Eliminare", WasteOperation.DISPOSED);

    static final Map<String, Unit> UNITS = labels("kg", Unit.KG, "tone", Unit.TONS);

    static final Map<String, WasteRegister> REGISTERS = labels(
            "Deșeu propriu", WasteRegister.ANEXA_1, "Preluat de la terți", WasteRegister.ART_48);

    static final Map<String, PhysicalState> PHYSICAL_STATES = labels(
            "Solid", PhysicalState.SOLID, "Lichid", PhysicalState.LIQUID, "Nămol", PhysicalState.SLUDGE,
            "Păstos", PhysicalState.PASTY, "Pulbere", PhysicalState.POWDER, "Gazos", PhysicalState.GASEOUS);

    /** Câte rânduri primesc listă derulantă. Importul acceptă {@link ExcelImportService#MAX_ROWS}. */
    private static final int VALIDATED_ROWS = 2000;

    public byte[] render() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));

            Sheet partners = dataSheet(wb, PARTNERS, PARTNER_COLUMNS, header);
            list(partners, 2, PARTNER_TYPES.keySet());
            list(partners, 3, YES_NO.keySet());
            list(partners, 4, YES_NO.keySet());
            list(partners, 5, YES_NO.keySet());
            partners.setDefaultColumnStyle(7, date);

            Sheet movements = dataSheet(wb, MOVEMENTS, MOVEMENT_COLUMNS, header);
            movements.setDefaultColumnStyle(0, date);
            list(movements, 3, OPERATIONS.keySet());
            list(movements, 5, UNITS.keySet());
            list(movements, 6, names(WasteOperationCode.values()));
            list(movements, 7, REGISTERS.keySet());
            list(movements, 10, PHYSICAL_STATES.keySet());
            list(movements, 11, names(StorageType.values()));
            list(movements, 12, names(TreatmentMethod.values()));
            list(movements, 13, names(TransportMeans.values()));
            list(movements, 14, names(WasteDestination.values()));

            instructions(wb.createSheet(INSTRUCTIONS));
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render the import template", e);
        }
    }

    private static Sheet dataSheet(Workbook wb, String name, List<String> columns, CellStyle header) {
        Sheet sheet = wb.createSheet(name);
        Row row = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, Math.max(14, columns.get(i).length() + 4) * 256);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private static void list(Sheet sheet, int column, Collection<String> values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidation validation = helper.createValidation(
                helper.createExplicitListConstraint(values.toArray(String[]::new)),
                new CellRangeAddressList(1, VALIDATED_ROWS, column, column));
        validation.setShowErrorBox(true);
        validation.setSuppressDropDownArrow(true);
        sheet.addValidationData(validation);
    }

    private static void instructions(Sheet sheet) {
        String[] lines = {
                "Importul din Excel — WasteHouse",
                "",
                "1. Completează foile „Parteneri” și „Mișcări”. Nu schimba antetul și nu muta coloanele: un fișier cu alt antet e refuzat.",
                "2. Coloanele cu * sunt obligatorii. Un rând gol se sare.",
                "3. Partenerii se potrivesc după CUI, apoi după denumire. Unul care există deja în firmă nu se dublează și nu se modifică.",
                "4. Pe o mișcare, „Partener” se scrie cu CUI-ul sau denumirea din foaia „Parteneri” ori din aplicație.",
                "5. „Punct de lucru” e numele exact din Setări → Puncte de lucru. Punctele de lucru nu se importă.",
                "6. „Cod deșeu” se scrie ca în Lista europeană: 15 01 01 sau 150101 (asteriscul nu contează).",
                "7. „Cod R/D” e obligatoriu la Valorificare (R1–R13) și la Eliminare (D1–D15), și interzis în rest.",
                "8. „Registru” se completează doar la o firmă colector, pe ieșiri: „Deșeu propriu” sau „Preluat de la terți”.",
                "9. Datele: zz.ll.aaaa. Cantitățile: cu virgulă sau punct zecimal.",
                "10. Întâi „Verifică”: aplicația arată fiecare rând greșit și nu salvează nimic. „Importă” salvează doar un fișier fără nicio eroare — totul sau nimic.",
                "11. Același fișier importat de două ori nu dublează mișcările. Un fișier modificat și reimportat, da.",
                "12. Întrebările despre ambalaje (pus pe piață, material, categorie) și datele de transport pentru Anexa 3 se completează după import, în aplicație.",
                "",
                "Coduri: Tip stocare, Mod tratare, Mijloc de transport și Destinație sunt codurile din HG 856/2002, anexa 1, cap. 2 (ex. RM, TM, AS, Vr).",
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 150 * 256);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    @SuppressWarnings("unchecked")
    private static <E> Map<String, E> labels(Object... pairs) {
        Map<String, E> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (E) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    /** Fără diacritice, fără majuscule, spaţii strânse: „Preluare de la terţi" = „preluare de la terti". */
    static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
