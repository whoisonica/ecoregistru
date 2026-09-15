package ro.ecoregistru.service.importer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.PartnerRequest;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.ImportResultResponse;
import ro.ecoregistru.controller.response.ImportResultResponse.RowError;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PartnerService;
import ro.ecoregistru.service.WasteMovementService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;
import static ro.ecoregistru.service.importer.ImportTemplate.*;

/**
 * P2.15 — importul de istoric din şablonul nostru: întâi partenerii, apoi mişcările.
 *
 * <p><b>Fiecare rând trece prin exact drumul unui formular.</b> Partenerul intră prin
 * {@link PartnerService#create}, mişcarea prin {@link WasteMovementService#create} — aceleaşi reguli
 * (codul R/D pe ieşire, registrul la colector, rolul partenerului), aceleaşi mesaje, acelaşi jurnal de
 * audit. Importul nu are reguli proprii în afară de citirea celulelor, deci nu poate băga în evidenţă
 * ce ecranul ar refuza.
 *
 * <p><b>Totul sau nimic, într-o singură tranzacţie.</b> „Verifică" rulează importul întreg şi îl
 * întoarce înapoi; „Importă" îl păstrează numai dacă n-a ieşit nicio eroare. O jumătate de an importat
 * ar fi mai rea decât nimic: fişa ar ieşi cu cifre, doar că greşite.
 *
 * <p><b>Acelaşi fişier de două ori nu dublează nimic.</b> Fiecare rând de mişcare poartă un
 * {@code clientGeneratedId} derivat din firmă, conţinutul fişierului şi numărul rândului — adică
 * idempotenţa pe care {@code create} o are deja pentru sincronizarea offline.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExcelImportService {

    /** Heroku taie cererea la 30 de secunde; două mii de rânduri pe foaie încap cu marjă. */
    public static final int MAX_ROWS = 2000;

    /**
     * Câţi octeţi despachetaţi acceptă o foaie înainte s-o citim în model. {@link #MAX_ROWS} se
     * verifică abia după ce POI a construit tot registrul în memorie — prea târziu pentru un fişier
     * mic care se umflă mult: 40.000 de rânduri intră în ~1,3 MB comprimaţi, dar dau o foaie de
     * 16 MB, iar modelul XSSF din ea trece de heap-ul de 300 MB al dyno-ului şi îl doboară pentru
     * toţi clienţii. Şablonul cu 2.000 de rânduri stă la ~0,76 MB despachetat, deci 8 MB e ~10×
     * peste maximul legitim şi mult sub pragul care sufocă heap-ul. Comprimarea nu e axa bună —
     * rânduri identice se string de 1000:1 — deci se măsoară octeţii despachetaţi, oprind pe loc.
     */
    static final long MAX_ENTRY_BYTES = 8L * 1024 * 1024;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d.M.yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE);

    PartnerService partnerService;
    WasteMovementService movementService;
    PartnerRepository partnerRepository;
    WorkPointRepository workPointRepository;
    WasteCodeRepository wasteCodeRepository;
    WasteMovementRepository movementRepository;

    @Transactional
    public ImportResultResponse run(MultipartFile file, boolean save) {
        UUID tenantId = TenantContext.require();
        byte[] bytes = bytesOf(file);
        List<RowError> errors = new ArrayList<>();
        int[] counts = new int[4]; // parteneri noi, existenţi, mişcări noi, existente

        try (Workbook wb = open(bytes)) {
            Sheet partners = sheet(wb, PARTNERS);
            Sheet movements = sheet(wb, MOVEMENTS);
            if (partners == null && movements == null) {
                throw new BadRequestException(IMPORT_TEMPLATE_MISMATCH);
            }
            requireHeader(partners, PARTNER_COLUMNS);
            requireHeader(movements, MOVEMENT_COLUMNS);
            requireSize(partners);
            requireSize(movements);

            PartnerIndex index = new PartnerIndex(partnerRepository.findAllByCompany_Id(tenantId));
            if (partners != null) {
                importPartners(partners, index, errors, counts);
            }
            if (movements != null) {
                importMovements(movements, tenantId, fingerprint(bytes), index, errors, counts);
            }
        } catch (IOException e) {
            throw new BadRequestException(IMPORT_FILE_UNREADABLE);
        }

        boolean saved = save && errors.isEmpty();
        if (!saved) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return new ImportResultResponse(saved, counts[0], counts[1], counts[2], counts[3], List.copyOf(errors));
    }

    // --- partners ---

    private void importPartners(Sheet sheet, PartnerIndex index, List<RowError> errors, int[] counts) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Cells c = new Cells(sheet, r, PARTNER_COLUMNS, errors);
            if (c.blank()) continue;

            String name = c.required(0, 255);
            String cui = c.text(1, 32);
            if (index.find(cui, name) != null) {
                counts[1]++;
                continue;
            }
            PartnerType type = c.choice(2, PARTNER_TYPES);
            boolean client = Boolean.TRUE.equals(c.choice(3, YES_NO));
            boolean supplier = Boolean.TRUE.equals(c.choice(4, YES_NO));
            boolean carrier = Boolean.TRUE.equals(c.choice(5, YES_NO));
            String authorization = c.text(6, 255);
            LocalDate expiry = c.date(7);
            String address = c.text(8, 1000);
            String tradeRegister = c.text(9, 255);
            if (!c.ok()) continue;

            c.attempt(() -> {
                var created = partnerService.create(new PartnerRequest(
                        name, cui, authorization, expiry, null, null, null, null,
                        type, client, supplier, carrier, null, address, null, tradeRegister,
                        false, null, null, null));
                index.add(created.id(), cui, name);
                counts[0]++;
            });
        }
    }

    // --- movements ---

    private void importMovements(Sheet sheet, UUID tenantId, String fingerprint, PartnerIndex partners,
                                 List<RowError> errors, int[] counts) {
        Map<String, WorkPoint> workPoints = new HashMap<>();
        workPointRepository.findAllByCompany_Id(tenantId).forEach(wp -> workPoints.put(fold(wp.getName()), wp));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Cells c = new Cells(sheet, r, MOVEMENT_COLUMNS, errors);
            if (c.blank()) continue;

            LocalDate date = c.date(0);
            c.requirePresent(0, date);
            WorkPoint workPoint = c.lookup(1, name -> workPoints.get(fold(name)),
                    "nu există în firmă. Punctele de lucru se adaugă din Setări, înainte de import.");
            WasteCode code = c.lookup(2, this::wasteCode,
                    "nu e în Lista europeană a deșeurilor.");
            WasteOperation operation = c.choice(3, OPERATIONS);
            c.requirePresent(3, operation);
            BigDecimal quantity = c.number(4);
            c.requirePresent(4, quantity);
            Unit unit = c.choice(5, UNITS);
            c.requirePresent(5, unit);
            WasteOperationCode operationCode = c.enumName(6, WasteOperationCode.class);
            WasteRegister register = c.choice(7, REGISTERS);
            String partnerRef = c.text(8, 255);
            UUID partnerId = partnerRef == null ? null : partners.find(partnerRef, partnerRef);
            if (partnerRef != null && partnerId == null) {
                c.error(8, "„" + partnerRef + "” nu e nici în foaia Parteneri, nici în firmă.");
            }
            String document = c.text(9, 255);
            PhysicalState physicalState = c.choice(10, PHYSICAL_STATES);
            StorageType storageType = c.enumName(11, StorageType.class);
            TreatmentMethod treatmentMethod = c.enumName(12, TreatmentMethod.class);
            TransportMeans transportMeans = c.enumName(13, TransportMeans.class);
            WasteDestination destination = c.enumName(14, WasteDestination.class);
            String notes = c.text(15, 2000);
            if (!c.ok()) continue;

            UUID rowId = UUID.nameUUIDFromBytes(
                    (tenantId + ":" + fingerprint + ":" + r).getBytes(StandardCharsets.UTF_8));
            if (movementRepository.findByCompany_IdAndClientGeneratedId(tenantId, rowId).isPresent()) {
                counts[3]++;
                continue;
            }
            c.attempt(() -> {
                movementService.create(new WasteMovementRequest(
                        rowId, workPoint.getId(), date, code.getId(), quantity, false, null, unit,
                        operation, register, physicalState, storageType, treatmentMethod, transportMeans,
                        destination, operationCode, partnerId, null, document, notes,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null,
                        null, null, null, null, null, null));
                counts[2]++;
            });
        }
    }

    /** „15 01 01", „150101", „15.01.01*" — toate ajung la forma din nomenclator. */
    private WasteCode wasteCode(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 6) return null;
        return wasteCodeRepository.findByCode(
                digits.substring(0, 2) + " " + digits.substring(2, 4) + " " + digits.substring(4)).orElse(null);
    }

    // --- the workbook ---

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException(IMPORT_FILE_UNREADABLE);
        }
    }

    private static Workbook open(byte[] bytes) {
        guardInflatedSize(bytes);
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            // Un PDF redenumit .xlsx, un fişier stricat, un .xlsx cu parolă.
            throw new BadRequestException(IMPORT_FILE_UNREADABLE);
        }
    }

    /**
     * Opreşte un fişier care s-ar umfla peste {@link #MAX_ENTRY_BYTES} <b>înainte</b> ca POI să-l
     * citească în model. Despachetează în flux, cu un tampon fix, şi se opreşte la prima intrare
     * care trece pragul — deci memoria e mărginită orice ar fi în arhivă. Mărimea din antetul zip
     * nu e de încredere (fişierele scrise în flux o lasă -1), aşa că se numără octeţii reali.
     */
    private static void guardInflatedSize(byte[] bytes) {
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buf = new byte[8192];
            while (zip.getNextEntry() != null) {
                long inflated = 0;
                int n;
                while ((n = zip.read(buf)) > 0) {
                    inflated += n;
                    if (inflated > MAX_ENTRY_BYTES) {
                        throw new BadRequestException(IMPORT_TOO_MANY_ROWS);
                    }
                }
            }
        } catch (IOException e) {
            throw new BadRequestException(IMPORT_FILE_UNREADABLE);
        }
    }

    private static Sheet sheet(Workbook wb, String name) {
        for (Sheet s : wb) {
            if (fold(s.getSheetName()).equals(fold(name))) return s;
        }
        return null;
    }

    private static void requireHeader(Sheet sheet, List<String> columns) {
        if (sheet == null) return;
        Row header = sheet.getRow(0);
        DataFormatter text = new DataFormatter(Locale.ROOT);
        for (int i = 0; i < columns.size(); i++) {
            String found = header == null ? "" : text.formatCellValue(header.getCell(i));
            if (!fold(found).equals(fold(columns.get(i)))) {
                throw new BadRequestException(IMPORT_TEMPLATE_MISMATCH);
            }
        }
    }

    private static void requireSize(Sheet sheet) {
        if (sheet != null && sheet.getLastRowNum() > MAX_ROWS) {
            throw new BadRequestException(IMPORT_TOO_MANY_ROWS);
        }
    }

    private static String fingerprint(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Partenerii firmei plus cei creaţi din fişier: după CUI, apoi după denumire. */
    private static final class PartnerIndex {
        private final Map<String, UUID> byCui = new HashMap<>();
        private final Map<String, UUID> byName = new HashMap<>();

        PartnerIndex(List<Partner> existing) {
            existing.forEach(p -> add(p.getId(), p.getCui(), p.getName()));
        }

        void add(UUID id, String cui, String name) {
            if (cui != null && !cuiKey(cui).isEmpty()) byCui.putIfAbsent(cuiKey(cui), id);
            if (name != null) byName.putIfAbsent(fold(name), id);
        }

        UUID find(String cui, String name) {
            UUID found = cui == null || cuiKey(cui).isEmpty() ? null : byCui.get(cuiKey(cui));
            return found != null || name == null ? found : byName.get(fold(name));
        }

        private static String cuiKey(String cui) {
            return cui.toUpperCase(Locale.ROOT).replaceAll("^RO|\\s", "");
        }
    }

    /** Un rând, citit celulă cu celulă; fiecare problemă devine o eroare cu rândul şi coloana ei. */
    private static final class Cells {
        private final String sheet;
        private final Row row;
        private final int excelRow;
        private final List<String> columns;
        private final List<RowError> errors;
        /** Nu e thread-safe, deci unul pe rând, nu unul pe clasă. */
        private final DataFormatter text = new DataFormatter(Locale.ROOT);
        private boolean ok = true;

        Cells(Sheet sheet, int index, List<String> columns, List<RowError> errors) {
            this.sheet = sheet.getSheetName();
            this.row = sheet.getRow(index);
            this.excelRow = index + 1;
            this.columns = columns;
            this.errors = errors;
        }

        boolean ok() {
            return ok;
        }

        boolean blank() {
            if (row == null) return true;
            for (int i = 0; i < columns.size(); i++) {
                if (raw(i) != null) return false;
            }
            return true;
        }

        void error(int column, String message) {
            ok = false;
            errors.add(new RowError(sheet, excelRow, "„" + columns.get(column).replace(" *", "") + "”: " + message));
        }

        /** Regulile aplicaţiei, cu mesajul lor, pe rândul care le-a încălcat. */
        void attempt(Runnable write) {
            try {
                write.run();
            } catch (BusinessException | BadRequestException | NotFoundException | UnprocessableEntityException e) {
                ok = false;
                errors.add(new RowError(sheet, excelRow, e.getMessage()));
            }
        }

        private Cell cell(int i) {
            return row == null ? null : row.getCell(i);
        }

        private String raw(int i) {
            Cell cell = cell(i);
            if (cell == null) return null;
            String value = text.formatCellValue(cell).trim();
            return value.isEmpty() ? null : value;
        }

        String text(int i, int max) {
            String value = raw(i);
            if (value != null && value.length() > max) {
                error(i, "are " + value.length() + " de caractere; încap cel mult " + max + ".");
                return null;
            }
            return value;
        }

        String required(int i, int max) {
            String value = text(i, max);
            requirePresent(i, value);
            return value;
        }

        void requirePresent(int i, Object value) {
            // O valoare greşită are deja eroarea ei; aici ajunge doar lipsa.
            if (value == null && raw(i) == null) error(i, "e obligatoriu.");
        }

        LocalDate date(int i) {
            Cell cell = cell(i);
            if (raw(i) == null) return null;
            CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
            if (type == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String value = raw(i);
            for (DateTimeFormatter f : DATE_FORMATS) {
                try {
                    return LocalDate.parse(value, f);
                } catch (DateTimeParseException ignored) {
                    // următorul format
                }
            }
            error(i, "„" + value + "” nu e o dată. Scrie-o ca zz.ll.aaaa.");
            return null;
        }

        BigDecimal number(int i) {
            Cell cell = cell(i);
            if (raw(i) == null) return null;
            CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
            BigDecimal value = null;
            if (type == CellType.NUMERIC) {
                value = BigDecimal.valueOf(cell.getNumericCellValue());
            } else {
                String s = raw(i).replaceAll("\\s", "");
                if (s.contains(",")) s = s.replace(".", "").replace(",", ".");
                try {
                    value = new BigDecimal(s);
                } catch (NumberFormatException ignored) {
                    // cade mai jos
                }
            }
            if (value == null || value.signum() <= 0) {
                error(i, "„" + raw(i) + "” nu e o cantitate mai mare decât zero.");
                return null;
            }
            return value;
        }

        <E> E choice(int i, Map<String, E> labels) {
            String value = raw(i);
            if (value == null) return null;
            for (Map.Entry<String, E> e : labels.entrySet()) {
                if (fold(e.getKey()).equals(fold(value))
                        || (e.getValue() instanceof Enum<?> en && en.name().equalsIgnoreCase(value.trim()))) {
                    return e.getValue();
                }
            }
            error(i, "„" + value + "” nu e una dintre valorile listei: " + String.join(", ", labels.keySet()) + ".");
            return null;
        }

        <E extends Enum<E>> E enumName(int i, Class<E> type) {
            String value = raw(i);
            if (value == null) return null;
            // „RM — Recipient metalic" sau „R 13": contează codul dinaintea liniuţei, fără spaţii.
            String key = value.split("[—–-]", 2)[0].replaceAll("\\s", "");
            for (E e : type.getEnumConstants()) {
                if (e.name().equalsIgnoreCase(key)) return e;
            }
            error(i, "„" + value + "” nu e un cod cunoscut.");
            return null;
        }

        <T> T lookup(int i, Function<String, T> finder, String notFound) {
            String value = raw(i);
            if (value == null) {
                error(i, "e obligatoriu.");
                return null;
            }
            T found = finder.apply(value);
            if (found == null) error(i, "„" + value + "” " + notFound);
            return found;
        }
    }
}
