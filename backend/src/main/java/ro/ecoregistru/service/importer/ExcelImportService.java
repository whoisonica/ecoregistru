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
import ro.ecoregistru.controller.request.WorkPointRequest;
import ro.ecoregistru.controller.response.ImportResultResponse;
import ro.ecoregistru.controller.response.ImportResultResponse.RowError;
import ro.ecoregistru.entity.ImportBatch;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.ImportBatchRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PartnerService;
import ro.ecoregistru.service.WasteMovementService;
import ro.ecoregistru.service.WorkPointService;
import ro.ecoregistru.security.SecurityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
 *
 * <p><b>Nici un fişier corectat şi reîncărcat</b> (16.09.2026). Amprenta se schimbă la orice virgulă, deci
 * rândurile se compară şi după conţinut cu mişcările deja în firmă: aceeaşi dată, punct de lucru, cod,
 * cantitate (în kg), operaţiune, partener şi document. Potrivirea e pe număr de apariţii — două predări
 * identice în firmă acoperă două rânduri, nu toate — şi numai cu ce exista înainte de import, ca două rânduri
 * identice din acelaşi fişier să intre amândouă. Rândul sărit apare ca avertisment, nu în tăcere.
 *
 * <p><b>Un import salvat se poate anula</b> ({@link ImportBatchService}): mişcările lui poartă importul (V62).
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
    ro.ecoregistru.repository.MonthlyEvidenceRepository evidenceRepository;
    WorkPointService workPointService;
    ImportBatchRepository batchRepository;

    @Transactional
    public ImportResultResponse run(MultipartFile file, boolean save) {
        UUID tenantId = TenantContext.require();
        if (save) {
            evidenceRepository.lockForRebuild(tenantId); // BUG-048: nu scrie în mijlocul unei refaceri
        }
        byte[] bytes = bytesOf(file);
        List<RowError> errors = new ArrayList<>();
        List<RowError> warnings = new ArrayList<>();
        List<UUID> createdMovements = new ArrayList<>();
        int[] counts = new int[6]; // parteneri noi, existenţi, mişcări noi, existente, puncte de lucru noi, existente

        try (Workbook wb = open(bytes)) {
            Sheet partners = sheet(wb, PARTNERS);
            Sheet movements = sheet(wb, MOVEMENTS);
            Sheet workPoints = sheet(wb, WORK_POINTS);
            if (partners == null && movements == null && workPoints == null) {
                throw new BadRequestException(IMPORT_TEMPLATE_MISMATCH);
            }
            requireHeader(partners, PARTNER_COLUMNS, 0);
            requireHeader(movements, MOVEMENT_COLUMNS, 0);
            requireHeader(workPoints, WORK_POINT_COLUMNS, 0);
            boolean movementExtras = movements != null && hasColumnsFrom(movements, MOVEMENT_COLUMNS.size());
            if (movementExtras) {
                requireHeader(movements, MOVEMENT_EXTRA_COLUMNS, MOVEMENT_COLUMNS.size());
            }
            requireSize(partners);
            requireSize(movements);
            requireSize(workPoints);

            if (workPoints != null) {
                importWorkPoints(workPoints, tenantId, errors, counts);
            }

            PartnerIndex index = new PartnerIndex(partnerRepository.findAllByCompany_Id(tenantId));
            if (partners != null) {
                importPartners(partners, index, errors, counts);
            }
            if (movements != null) {
                importMovements(movements, movementExtras, tenantId, fingerprint(bytes), index, errors, warnings,
                        createdMovements, counts);
            }
        } catch (IOException e) {
            throw new BadRequestException(IMPORT_FILE_UNREADABLE);
        }

        boolean saved = save && errors.isEmpty();
        if (!saved) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } else if (counts[0] + counts[2] + counts[4] > 0) {
            ImportBatch batch = batchRepository.save(ImportBatch.builder()
                    .companyId(tenantId).fileName(fileName(file))
                    .createdBy(SecurityUtils.currentUser().getId()).createdAt(Instant.now())
                    .workPointsNew(counts[4]).partnersNew(counts[0]).movementsNew(counts[2]).build());
            if (!createdMovements.isEmpty()) {
                movementRepository.tagImportBatch(batch.getId(), createdMovements);
            }
        }
        return new ImportResultResponse(saved, counts[0], counts[1], counts[2], counts[3], counts[4], counts[5],
                List.copyOf(errors), List.copyOf(warnings));
    }

    // --- work points ---

    /**
     * A doua felie: punctele de lucru, înaintea mișcărilor care le numesc. Prin {@link WorkPointService#create},
     * deci cu secțiile implicite. Unul care există deja după nume se sare. Importul îl face numai platforma
     * ({@code ImportController}), care poate adăuga puncte de lucru oricum.
     */
    private void importWorkPoints(Sheet sheet, UUID tenantId, List<RowError> errors, int[] counts) {
        Set<String> existing = new HashSet<>();
        workPointRepository.findAllByCompany_Id(tenantId).forEach(wp -> existing.add(fold(wp.getName())));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Cells c = new Cells(sheet, r, WORK_POINT_COLUMNS, errors);
            if (c.blank()) continue;
            String name = c.required(0, 255);
            String address = c.text(1, 512); // BUG-055: work_points.address e VARCHAR(512)
            if (!c.ok()) continue;
            if (existing.contains(fold(name))) {
                counts[5]++;
                continue;
            }
            c.attempt(() -> {
                workPointService.create(new WorkPointRequest(name, address));
                existing.add(fold(name));
                counts[4]++;
            });
        }
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
            String authorization = c.text(6, 128); // BUG-055: coloanele partenerului
            LocalDate expiry = c.date(7);
            String address = c.text(8, 500);
            String tradeRegister = c.text(9, 50);
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

    /** R3 — de la cea mai veche până la cea mai nouă dată din fişier; {@code null} dacă n-are niciuna. */
    private record DateWindow(LocalDate from, LocalDate to) {}

    /**
     * O trecere ieftină peste foaie, numai pe coloana datei, ca deduplicarea să citească din bază
     * doar intervalul fişierului.
     *
     * <p>Foloseşte <b>acelaşi</b> {@code Cells.date} ca trecerea adevărată, cu o listă de erori
     * aruncată: dacă ar fi parsat datele altfel, fereastra ar fi putut rata un rând pe care apoi
     * trecerea principală îl consideră duplicat — adică exact bugul pe care îl repară.
     */
    private DateWindow dateWindowOf(Sheet sheet, List<String> columns) {
        List<RowError> ignored = new ArrayList<>();
        LocalDate from = null;
        LocalDate to = null;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Cells c = new Cells(sheet, r, columns, ignored);
            if (c.blank()) continue;
            LocalDate d = c.date(0);
            if (d == null) continue;
            if (from == null || d.isBefore(from)) from = d;
            if (to == null || d.isAfter(to)) to = d;
        }
        return from == null ? null : new DateWindow(from, to);
    }

    // --- movements ---

    private void importMovements(Sheet sheet, boolean extras, UUID tenantId, String fingerprint,
                                 PartnerIndex partners, List<RowError> errors, List<RowError> warnings,
                                 List<UUID> created, int[] counts) {
        List<String> columns = new ArrayList<>(MOVEMENT_COLUMNS);
        if (extras) columns.addAll(MOVEMENT_EXTRA_COLUMNS);
        // Ce era în firmă înainte de import, numărat pe conţinut. Rândurile create acum nu intră aici.
        //
        // R3 — numai fereastra de date a fişierului, nu toate mişcările firmei. Interogarea de
        // dinainte (`findAllByCompany_IdAndDeletedFalse`) aducea în memorie fiecare rând viu, plus
        // atingeri leneşe pe punct, cod şi partener pentru fiecare: la o firmă cu doi ani importaţi
        // deja, asta e exact bomba de memorie pe care `guardInflatedSize` o opreşte la intrare şi
        // pe care deduplicarea o reintroducea. Cheia de conţinut are data în ea, deci un rând din
        // afara ferestrei n-ar putea oricum să se potrivească cu vreunul din fişier.
        Map<String, Integer> alreadyThere = new HashMap<>();
        DateWindow window = dateWindowOf(sheet, columns);
        List<WasteMovement> existing = window == null
                ? List.of()
                : movementRepository.findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, window.from(), window.to());
        existing.forEach(m -> alreadyThere.merge(
                contentKey(m.getDate(), m.getWorkPoint().getId(), m.getWasteCode().getId(), m.getQuantity(),
                        m.getUnit(), m.getOperation(), m.getPartner() == null ? null : m.getPartner().getId(),
                        m.getDocumentReference()), 1, Integer::sum));
        Map<String, WorkPoint> workPoints = new HashMap<>();
        workPointRepository.findAllByCompany_Id(tenantId).forEach(wp -> workPoints.put(fold(wp.getName()), wp));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Cells c = new Cells(sheet, r, columns, errors);
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
            if (quantity != null && quantity.stripTrailingZeros().scale() > 3) {
                c.error(4, "are mai mult de trei zecimale."); // BUG-055: s-ar fi rotunjit în tăcere
            }
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
            String notes = c.text(15, 1000); // BUG-055: waste_movements.notes e VARCHAR(1000)

            // A doua felie: ambalajele și transportul. Fără coloane, totul rămâne null, ca înainte.
            int x = MOVEMENT_COLUMNS.size();
            Boolean packagingOnMarket = extras ? c.choice(x, YES_NO) : null;
            PackagingMaterial packagingMaterial = extras ? c.choice(x + 1, PACKAGING_MATERIALS) : null;
            PackagingCategory packagingCategory = extras ? c.choice(x + 2, PACKAGING_CATEGORIES) : null;
            Boolean packagingReusable = extras ? c.choice(x + 3, YES_NO) : null;
            Boolean packagingHazardous = extras ? c.choice(x + 4, YES_NO) : null;
            PackagingOrigin packagingOrigin = extras ? c.choice(x + 5, PACKAGING_ORIGINS) : null;
            LocalDate loadDate = extras ? c.date(x + 6) : null;
            LocalDate unloadDate = extras ? c.date(x + 7) : null;
            String carrierRef = extras ? c.text(x + 8, 255) : null;
            UUID carrierId = carrierRef == null ? null : partners.find(carrierRef, carrierRef);
            if (carrierRef != null && carrierId == null) {
                c.error(x + 8, "„" + carrierRef + "” nu e nici în foaia Parteneri, nici în firmă.");
            }
            String driverName = extras ? c.text(x + 9, 255) : null;
            String driverIdentification = extras ? c.text(x + 10, 100) : null;
            String vehicleRegistration = extras ? c.text(x + 11, 50) : null;
            if (!c.ok()) continue;

            UUID rowId = UUID.nameUUIDFromBytes(
                    (tenantId + ":" + fingerprint + ":" + r).getBytes(StandardCharsets.UTF_8));
            String key = contentKey(date, workPoint.getId(), code.getId(), quantity, unit, operation, partnerId, document);
            if (movementRepository.findByCompany_IdAndClientGeneratedId(tenantId, rowId).isPresent()) {
                alreadyThere.computeIfPresent(key, (k, n) -> n > 1 ? n - 1 : null);
                counts[3]++;
                continue;
            }
            if (alreadyThere.containsKey(key)) {
                alreadyThere.computeIfPresent(key, (k, n) -> n > 1 ? n - 1 : null);
                counts[3]++;
                warnings.add(new RowError(sheet.getSheetName(), r + 1, "Pare deja în firmă: o mișcare cu aceeași dată, "
                        + "punct de lucru, cod, cantitate, operațiune, partener și document. Nu s-a importat a doua oară."));
                continue;
            }
            c.attempt(() -> {
                var movement = movementService.create(new WasteMovementRequest(
                        rowId, workPoint.getId(), date, code.getId(), quantity, false, null, unit,
                        operation, register, physicalState, storageType, treatmentMethod, transportMeans,
                        destination, operationCode, partnerId, null, document, notes,
                        loadDate, unloadDate, null, carrierId, driverName, driverIdentification, null,
                        vehicleRegistration, null, null,
                        null, null, null, null,
                        packagingOnMarket, packagingMaterial, packagingCategory, packagingReusable,
                        packagingHazardous, packagingOrigin));
                created.add(movement.id());
                counts[2]++;
            });
        }
    }

    /** Cheia de conţinut a unei mişcări; cantitatea în kg, fără zerouri de coadă, documentul fără majuscule. */
    private static String contentKey(LocalDate date, UUID workPoint, UUID code, BigDecimal quantity, Unit unit,
                                     WasteOperation operation, UUID partner, String document) {
        BigDecimal kg = quantity == null ? null
                : (unit == Unit.TONS ? quantity.multiply(BigDecimal.valueOf(1000)) : quantity).stripTrailingZeros();
        return String.join("|", String.valueOf(date), String.valueOf(workPoint), String.valueOf(code),
                kg == null ? "" : kg.toPlainString(), String.valueOf(operation), String.valueOf(partner),
                document == null ? "" : fold(document));
    }

    private static String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? null : name.length() > 255 ? name.substring(0, 255) : name;
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

    private static void requireHeader(Sheet sheet, List<String> columns, int from) {
        if (sheet == null) return;
        Row header = sheet.getRow(0);
        DataFormatter text = new DataFormatter(Locale.ROOT);
        for (int i = 0; i < columns.size(); i++) {
            String found = header == null ? "" : text.formatCellValue(header.getCell(from + i));
            if (!fold(found).equals(fold(columns.get(i)))) {
                throw new BadRequestException(IMPORT_TEMPLATE_MISMATCH);
            }
        }
    }

    /** Are antetul ceva scris de la coloana {@code from} încolo? Așa se recunoaște șablonul cu a doua felie. */
    private static boolean hasColumnsFrom(Sheet sheet, int from) {
        Row header = sheet.getRow(0);
        if (header == null) return false;
        DataFormatter text = new DataFormatter(Locale.ROOT);
        for (int i = from; i < Math.max(from, header.getLastCellNum()); i++) {
            if (!text.formatCellValue(header.getCell(i)).isBlank()) return true;
        }
        return false;
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
