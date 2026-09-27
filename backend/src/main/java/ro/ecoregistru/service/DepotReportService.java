package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.DepotReportKind;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.DepotRegisterGenerator;
import ro.ecoregistru.service.export.DepotReport;
import ro.ecoregistru.service.export.DepotReport.Column;
import ro.ecoregistru.service.export.DepotReport.Section;
import ro.ecoregistru.service.export.DepotReportPdf;
import ro.ecoregistru.service.export.DepotReportXlsx;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEPOT_REPORT_FORMAT_UNAVAILABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEPOT_REPORT_PARTNER_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEPOT_REPORT_PERIOD_INVALID;
import static ro.ecoregistru.exception.ErrorMessageEnum.EXPORT_FORMAT_UNSUPPORTED;
import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * D4.7 — cele unsprezece rapoarte fixe ale depozitului (Cântar → Rapoarte), pe un interval de cel mult un an și pe un
 * depozit sau pe toate. Fiecare metodă umple un {@link DepotReport}; fișierul îl scrie {@link DepotReportXlsx} sau
 * {@link DepotReportPdf}. Registrul de intrări-ieșiri rămâne pe generatorul lui (D1.14).
 *
 * <p>Regulile de acces sunt ale modulului, nu altele noi: {@link DepotAccess} taie depozitele (un depozit străin e 404),
 * banii cer pe cine aprobă <b>și</b> vede prețurile (ca {@code retentions}), CNP-urile întregi pe cine aprobă. Banii sunt
 * pe toată firma: declarația AFM, D100/D205 și plafonul de numerar nu se fac pe depozit.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepotReportService {

    /** Un an întreg, bisect inclus; mai mult ar încărca prea multe operațiuni odată (lecția BUG-017). */
    static final int MAX_DAYS = 366;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM.yyyy");
    private static final Set<Role> APPROVERS = EnumSet.of(Role.PLATFORM_ADMIN, Role.ADMIN, Role.CONSULTANT);
    private static final Set<WasteOperation> INBOUND = EnumSet.of(WasteOperation.COLLECTED,
            WasteOperation.TRANSFERRED_IN, WasteOperation.OPENING_BALANCE, WasteOperation.INVENTORY_SURPLUS,
            WasteOperation.PROCESSING_OUTPUT);

    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;
    AppUserRepository userRepository;
    WeighingOperationRepository operationRepository;
    WasteMovementRepository movementRepository;
    DepotAccess depotAccess;
    WeighingOperationService operations;
    DepotReportXlsx xlsx;
    DepotReportPdf pdf;

    public record Rendered(byte[] body, String fileName, String contentType) {
    }

    /** Ce e comun unui raport: firma, perioada, depozitul ales (sau null) și ce vede cel care îl cere. */
    private record Scope(UUID tenantId, Company company, LocalDate from, LocalDate to, WorkPoint depot,
                         Set<UUID> allowed, boolean prices) {

        /** Operațiunea pleacă din depozitul ales sau vine în el, și utilizatorul o vede. */
        boolean shows(WeighingOperation o) {
            return WeighingOperationService.visible(o, allowed)
                    && (depot == null || WeighingOperationService.visible(o, Set.of(depot.getId())));
        }

        /** O linie a depozitului ales și a depozitelor utilizatorului. */
        boolean shows(WasteMovement m) {
            UUID wp = m.getWorkPoint().getId();
            return (allowed == null || allowed.contains(wp)) && (depot == null || depot.getId().equals(wp));
        }
    }

    @Transactional(readOnly = true)
    public Rendered render(DepotReportKind kind, LocalDate from, LocalDate to, UUID workPointId, UUID articleId,
                           UUID partnerId, String format) {
        UUID tenantId = TenantContext.require();
        boolean asPdf = "pdf".equalsIgnoreCase(format);
        if (!asPdf && format != null && !"xlsx".equalsIgnoreCase(format)) {
            throw new BusinessException(EXPORT_FORMAT_UNSUPPORTED);
        }
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new BusinessException(DEPOT_REPORT_PERIOD_INVALID);
        }
        if (asPdf && !kind.pdf()) {
            throw new BusinessException(DEPOT_REPORT_FORMAT_UNAVAILABLE);
        }
        if (kind == DepotReportKind.PARTNER_CARD && partnerId == null) {
            throw new BusinessException(DEPOT_REPORT_PARTNER_REQUIRED);
        }
        Company company = companyRepository.findById(tenantId).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        boolean prices = company.getPriceVisibility().visibleTo(SecurityUtils.currentUser().getRole());
        requireAccess(kind, prices);
        WorkPoint depot = workPointId == null ? null : depotAccess.require(workPointRepository
                .findByIdAndCompany_Id(workPointId, tenantId).orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
        Scope scope = new Scope(tenantId, company, from, to, kind.wholeCompany() ? null : depot, depotAccess.allowed(),
                prices);

        String name = kind.slug() + "-" + from + "_" + to + (scope.depot() == null ? "" : "-" + slug(depot.getName()));
        if (kind == DepotReportKind.REGISTER) {
            return new Rendered(operations.renderRegister(from, to, workPointId, period(scope)), name + ".xlsx", XLSX);
        }
        DepotReport report = switch (kind) {
            case SCALE_LOG -> scaleLog(scope);
            case ISSUED_DOCUMENTS -> issuedDocuments(scope);
            case CANCELLED -> cancelled(scope);
            case STOCK_CARD -> stockCard(scope, articleId);
            case TRANSFERS -> transfers(scope);
            case AFM -> afm(scope, depot);
            case INCOME_TAX -> incomeTax(scope, depot);
            case CASH_PF -> cash(scope, depot);
            case INDIVIDUALS -> individuals(scope);
            case PARTNER_CARD -> partnerCard(scope, partnerId);
            case REGISTER -> throw new IllegalStateException("handled above");
        };
        return asPdf ? new Rendered(pdf.render(report), name + ".pdf", "application/pdf")
                : new Rendered(xlsx.render(report), name + ".xlsx", XLSX);
    }

    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static void requireAccess(DepotReportKind kind, boolean prices) {
        boolean approver = APPROVERS.contains(SecurityUtils.currentUser().getRole());
        switch (kind.access()) {
            case ANY -> {
            }
            case APPROVER -> {
                if (!approver) {
                    throw new AccessDeniedException("Raportul arată CNP-uri întregi: îl scoate cine aprobă.");
                }
            }
            case MONEY -> {
                if (!approver || !prices) {
                    throw new AccessDeniedException("Raportul arată banii depozitului: îl scoate cine aprobă și vede prețurile.");
                }
            }
        }
    }

    // --- registre ---------------------------------------------------------------------------------------------------

    /** Jurnalul de cântar: o linie pe fiecare cântărire, cu brut, tara, neto, final și impuritățile; fără balotare. */
    private DepotReport scaleLog(Scope s) {
        List<WeighingOperation> ops = operationsOf(s, null).stream()
                .filter(o -> o.getType() != WeighingOperationType.PROCESSING).toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        List<List<Object>> rows = new ArrayList<>();
        for (WeighingOperation o : ops) {
            for (WasteMovement m : lines.getOrDefault(o.getId(), List.of())) {
                if (!s.shows(m)) {
                    continue;
                }
                boolean receipt = m.getOperation() == WasteOperation.TRANSFERRED_IN;
                var scale = receipt ? o.getReceiptScale() : o.getScale();
                String state = receipt ? o.getReceiptScaleState() : o.getScaleState();
                BigDecimal net = m.getNetKg() == null ? m.getQuantity() : m.getNetKg();
                rows.add(Arrays.asList(date(m.getDate()), o.getNumber(), DepotRegisterGenerator.type(o, m),
                        m.getWorkPoint().getName(), counterpart(o, m), o.getVehicleRegistration(), o.getDriverName(),
                        scale == null ? null : scale.getName(),
                        state == null ? null : DepotRegisterGenerator.SCALE_STATES.getOrDefault(state, state),
                        receipt ? o.getReceiptScaleOverrideReason() : o.getScaleOverrideReason(),
                        receipt ? o.getReceiptGrossKg() : o.getGrossKg(), receipt ? o.getReceiptTareKg() : o.getTareKg(),
                        article(m), code(m), m.getGrossKg(), m.getTareKg(), net, m.getQuantity(),
                        net == null ? null : net.subtract(m.getQuantity()),
                        DepotRegisterGenerator.status(o.getStatus())));
            }
        }
        return report(s, "Jurnalul de cântar", List.of(new Section(null, List.of(
                Column.text("Data"), Column.integer("Nr."), Column.text("Tip"), Column.text("Depozit"),
                Column.text("Partener"), Column.text("Mașină"), Column.text("Șofer / delegat"), Column.text("Cântar"),
                Column.text("Starea cântarului"), Column.text("Motiv cântar"), Column.kg("Brut operațiune (kg)"),
                Column.kg("Tara operațiune (kg)"), Column.text("Sortiment"), Column.text("Cod deșeu"),
                Column.kg("Brut (kg)"), Column.kg("Tara (kg)"), Column.kg("Neto (kg)"), Column.kg("Final (kg)"),
                Column.kg("Impurități (kg)"), Column.text("Stare")), rows, null)),
                List.of("Impuritățile = neto − final (ce s-a scăzut la recepție). Balotarea nu se cântărește și nu apare aici."),
                List.of());
    }

    /**
     * Documentele numerotate ale firmei din perioadă — Anexa 3 (de la cântar și din mișcările de mână: carnetul e unul),
     * borderourile și NIR-urile —, plus numerele care lipsesc din fiecare serie, citite pe toată firma.
     */
    private DepotReport issuedDocuments(Scope s) {
        List<WeighingOperation> ops = operationsOf(s, null);
        List<List<Object>> rows = new ArrayList<>();
        for (WeighingOperation o : ops) {
            String state = DepotRegisterGenerator.status(o.getStatus());
            if (o.getAnexa3Number() != null) {
                rows.add(document("Anexa 3", o.getAnexa3Series(), o.getAnexa3Number(), o, state));
            }
            if (o.getBorderouNumber() != null) {
                rows.add(document("Borderou", null, o.getBorderouNumber(), o, state));
            }
            if (o.getReceptionNoteNumber() != null) {
                rows.add(document("NIR", null, o.getReceptionNoteNumber(), o, state));
            }
        }
        for (WasteMovement m : movementRepository.findAnexa3IssuedBetween(s.tenantId(), s.from(), s.to())) {
            if (s.shows(m)) {
                rows.add(Arrays.asList("Anexa 3", m.getAnexa3Series(), m.getAnexa3Number(), date(m.getDate()),
                        m.getWorkPoint().getName(), "Mișcare de mână", null,
                        m.getPartner() == null ? null : m.getPartner().getName(),
                        m.isDeleted() ? "Ștearsă" : "Validă", null));
            }
        }
        List<String> order = List.of("Anexa 3", "Borderou", "NIR");
        rows.sort(Comparator.<List<Object>>comparingInt(r -> order.indexOf((String) r.get(0)))
                .thenComparingInt(r -> (Integer) r.get(2)));

        List<List<Object>> gaps = new ArrayList<>();
        gaps(gaps, "Anexa 3", Stream.concat(operationRepository.findAllAnexa3Numbers(s.tenantId()).stream(),
                movementRepository.findAllAnexa3Numbers(s.tenantId()).stream()).toList());
        gaps(gaps, "Borderou", operationRepository.findAllBorderouNumbers(s.tenantId()));
        gaps(gaps, "NIR", operationRepository.findAllReceptionNoteNumbers(s.tenantId()));

        return report(s, "Registrul documentelor emise", List.of(
                new Section(null, List.of(Column.text("Document"), Column.text("Serie"), Column.integer("Număr"),
                        Column.text("Data"), Column.text("Depozit"), Column.text("Operațiune"),
                        Column.integer("Nr. operațiune"), Column.text("Partener"), Column.text("Stare"),
                        Column.text("Motiv anulare")), rows, null),
                new Section("Numere lipsă din serie (pe toată firma)", List.of(Column.text("Document"),
                        Column.integer("Număr"), Column.text("Stare")), gaps, null, "Niciun număr lipsă.")),
                List.of("Seriile sunt ale firmei, nu ale depozitului: golurile se caută de la 1 la cel mai mare număr dat.",
                        "Un document de pe o operațiune anulată își păstrează numărul (nu se refolosește)."),
                List.of());
    }

    private static List<Object> document(String kind, String series, Integer number, WeighingOperation o, String state) {
        return Arrays.asList(kind, series, number, date(o.getDate()), o.getWorkPoint().getName(), typeOf(o),
                o.getNumber(), counterpart(o, null), state, o.getCancelReason());
    }

    private static void gaps(List<List<Object>> out, String kind, List<Integer> numbers) {
        Set<Integer> given = new TreeSet<>(numbers);
        int max = given.stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int n = 1; n <= max; n++) {
            if (!given.contains(n)) {
                out.add(Arrays.asList(kind, n, "Lipsește din serie"));
            }
        }
    }

    /** Operațiunile anulate, cu ce documente rămăseseră pe ele, cine și când a anulat și de ce. */
    private DepotReport cancelled(Scope s) {
        List<WeighingOperation> ops = operationsOf(s, null).stream()
                .filter(o -> o.getStatus() == WeighingOperationStatus.CANCELLED).toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(ops.stream().map(WeighingOperation::getCancelledBy).filter(Objects::nonNull)
                .distinct().toList()).forEach(u -> names.put(u.getId(), WeighingOperationService.fullName(u)));
        List<List<Object>> rows = new ArrayList<>();
        for (WeighingOperation o : ops) {
            BigDecimal kg = lines.getOrDefault(o.getId(), List.of()).stream()
                    .filter(m -> m.getOperation() != WasteOperation.TRANSFERRED_IN)
                    .map(DepotReportService::kg).reduce(BigDecimal.ZERO, BigDecimal::add);
            String documents = Stream.of(
                            o.getAnexa3Number() == null ? null : "Anexa 3 nr. " + o.getAnexa3Number(),
                            o.getBorderouNumber() == null ? null : "Borderou nr. " + o.getBorderouNumber(),
                            o.getReceptionNoteNumber() == null ? null : "NIR nr. " + o.getReceptionNoteNumber())
                    .filter(Objects::nonNull).collect(Collectors.joining("; "));
            rows.add(Arrays.asList(o.getNumber(), typeOf(o), date(o.getDate()), o.getWorkPoint().getName(),
                    counterpart(o, null), kg, documents.isEmpty() ? null : documents,
                    o.getCancelledAt() == null ? null : date(o.getCancelledAt().atZone(BUCHAREST).toLocalDate()),
                    o.getCancelledBy() == null ? null : names.get(o.getCancelledBy()), o.getCancelReason()));
        }
        return report(s, "Operațiuni anulate", List.of(new Section(null, List.of(Column.integer("Nr."),
                Column.text("Tip"), Column.text("Data"), Column.text("Depozit"), Column.text("Partener"),
                Column.kg("Cantitate (kg)"), Column.text("Documente rămase"), Column.text("Anulată la"),
                Column.text("Anulată de"), Column.text("Motiv")), rows, null)), List.of(), List.of());
    }

    private static final java.time.ZoneId BUCHAREST = java.time.ZoneId.of("Europe/Bucharest");

    // --- stoc -------------------------------------------------------------------------------------------------------

    /**
     * Fișa de stoc pe sortiment × cod: soldul la sfârșitul zilei dinaintea lui „de la”, fiecare mișcare cu soldul după ea,
     * soldul final. Aceleași linii ca {@code stockAt}, deci soldul final e cel de pe ecranul Stoc la „până la”.
     */
    private DepotReport stockCard(Scope s, UUID articleId) {
        UUID wp = s.depot() == null ? null : s.depot().getId();
        Map<String, BigDecimal> opening = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (var line : movementRepository.stockAt(s.tenantId(), wp, s.from().minusDays(1))) {
            if ((s.allowed() != null && !s.allowed().contains(line.getWorkPointId()))
                    || (articleId != null && !articleId.equals(line.getArticleId()))) {
                continue;
            }
            String key = line.getArticleId() + "|" + line.getWasteCodeId();
            opening.merge(key, line.getKg(), BigDecimal::add);
            labels.put(key, label(line.getArticleName(), line.getCode(), line.getName()));
        }
        Map<String, List<WasteMovement>> moves = new LinkedHashMap<>();
        for (WasteMovement m : movementRepository.stockLinesBetween(s.tenantId(), wp, s.from(), s.to())) {
            if (!s.shows(m) || (articleId != null && (m.getArticle() == null || !articleId.equals(m.getArticle().getId())))) {
                continue;
            }
            String key = (m.getArticle() == null ? null : m.getArticle().getId()) + "|" + m.getWasteCode().getId();
            moves.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
            labels.put(key, label(m.getArticle() == null ? null : m.getArticle().getName(), m.getWasteCode().getCode(),
                    m.getWasteCode().getName()));
        }
        List<Section> sections = new ArrayList<>();
        Set<String> keys = new TreeSet<>(Comparator.comparing((String k) -> labels.get(k)).thenComparing(k -> k));
        keys.addAll(moves.keySet());
        opening.forEach((k, v) -> {
            if (v.signum() != 0) {
                keys.add(k);
            }
        });
        for (String key : keys) {
            BigDecimal balance = opening.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal in = BigDecimal.ZERO;
            BigDecimal out = BigDecimal.ZERO;
            List<List<Object>> rows = new ArrayList<>();
            rows.add(Arrays.asList("Sold inițial", null, null, null, null, null, balance));
            List<WasteMovement> list = new ArrayList<>(moves.getOrDefault(key, List.of()));
            list.sort(Comparator.comparing(WasteMovement::getDate)
                    .thenComparing(m -> INBOUND.contains(m.getOperation()) ? 0 : 1)
                    .thenComparing(m -> m.getWeighingOperation() == null ? 0 : m.getWeighingOperation().getNumber()));
            for (WasteMovement m : list) {
                boolean inbound = INBOUND.contains(m.getOperation());
                BigDecimal kg = kg(m);
                balance = inbound ? balance.add(kg) : balance.subtract(kg);
                if (inbound) {
                    in = in.add(kg);
                } else {
                    out = out.add(kg);
                }
                rows.add(Arrays.asList(date(m.getDate()), stockDocument(m), m.getWorkPoint().getName(),
                        m.getWeighingOperation() == null ? (m.getPartner() == null ? null : m.getPartner().getName())
                                : counterpart(m.getWeighingOperation(), m),
                        inbound ? kg : null, inbound ? null : kg, balance));
            }
            sections.add(new Section(labels.get(key), List.of(Column.text("Data"), Column.text("Document"),
                    Column.text("Depozit"), Column.text("Partener"), Column.kg("Intrare (kg)"), Column.kg("Ieșire (kg)"),
                    Column.kg("Sold (kg)")), rows, Arrays.asList("Sold final", null, null, null, in, out, balance)));
        }
        if (sections.isEmpty()) {
            sections.add(new Section(null, List.of(Column.text("Sortiment")), List.of(), null));
        }
        return report(s, "Fișa de stoc pe sortiment", sections,
                List.of("Cantități în kg (tonele din mișcările de mână sunt trecute în kg). Soldul final e stocul de pe "
                                + "ecranul Stoc la " + date(s.to()) + ".",
                        "Contează operațiunile finalizate și transferurile plecate; cele în lucru sau anulate, nu."),
                List.of("Gestionar", "Verificat"));
    }

    private static String stockDocument(WasteMovement m) {
        return switch (m.getOperation()) {
            case OPENING_BALANCE -> "Sold preluat";
            case INVENTORY_SURPLUS -> "Plus la inventar";
            case INVENTORY_SHORTAGE -> "Minus la inventar";
            default -> m.getWeighingOperation() == null ? "Mișcare de mână"
                    : DepotRegisterGenerator.type(m.getWeighingOperation(), m) + " nr. " + m.getWeighingOperation().getNumber();
        };
    }

    /** Transferurile plecate în perioadă și cele încă pe drum la „până la”, cu diferența la recepție. */
    private DepotReport transfers(Scope s) {
        List<WeighingOperation> ops = operationRepository
                .findForScreen(s.tenantId(), WeighingOperationType.TRANSFER, LocalDate.of(1900, 1, 1), s.to()).stream()
                .filter(s::shows)
                .filter(o -> o.getStatus() == WeighingOperationStatus.IN_TRANSIT
                        || o.getStatus() == WeighingOperationStatus.FINALIZED)
                .filter(o -> !o.getDate().isBefore(s.from()) || onTheRoad(o, s.to()))
                .sorted(Comparator.comparing(WeighingOperation::getDate).thenComparingInt(WeighingOperation::getNumber))
                .toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        List<List<Object>> rows = new ArrayList<>();
        for (WeighingOperation o : ops) {
            boolean road = onTheRoad(o, s.to());
            List<WasteMovement> ls = lines.getOrDefault(o.getId(), List.of());
            BigDecimal sent = sum(ls, WasteOperation.TRANSFERRED_OUT);
            BigDecimal received = road ? null : sum(ls, WasteOperation.TRANSFERRED_IN);
            BigDecimal difference = received == null ? null : received.subtract(sent);
            boolean outside = difference != null && o.getToleranceKg() != null
                    && difference.abs().compareTo(o.getToleranceKg()) > 0;
            rows.add(Arrays.asList(o.getNumber(), date(o.getDate()), o.getWorkPoint().getName(),
                    o.getTargetWorkPoint() == null ? null : o.getTargetWorkPoint().getName(),
                    road ? "În tranzit" : "Recepționat", road ? null : date(o.getReceivedOn()),
                    (int) ChronoUnit.DAYS.between(o.getDate(), road ? s.to() : o.getReceivedOn()),
                    sent, received, difference, o.getToleranceKg(), outside ? "DA" : null, o.getNirNumber(),
                    o.getDifferenceReason()));
        }
        return report(s, "Transferuri în tranzit și diferențe la recepție", List.of(new Section(null, List.of(
                Column.integer("Nr."), Column.text("Plecat la"), Column.text("Din"), Column.text("Spre"),
                Column.text("Stare la " + date(s.to())), Column.text("Recepționat la"), Column.integer("Zile pe drum"),
                Column.kg("Trimis (kg)"), Column.kg("Primit (kg)"), Column.kg("Diferență (kg)"),
                Column.kg("Toleranță (kg)"), Column.text("În afara toleranței"), Column.text("NIR"),
                Column.text("Motivul diferenței")), rows, null)),
                List.of("Diferența = primit − trimis. Un transfer plecat înainte de perioadă apare dacă era încă pe drum la "
                        + date(s.to()) + "."),
                List.of());
    }

    private static boolean onTheRoad(WeighingOperation o, LocalDate at) {
        return !o.getDate().isAfter(at) && (o.getReceivedOn() == null || o.getReceivedOn().isAfter(at));
    }

    // --- bani -------------------------------------------------------------------------------------------------------

    /** 2% AFM (OUG 196/2005 art. 9): intrările finalizate, lună cu lună, cu scadența de 25 a lunii următoare. */
    private DepotReport afm(Scope s, WorkPoint chosen) {
        Map<YearMonth, List<WeighingOperation>> months = new TreeMap<>(operationsOf(s, WeighingOperationType.IN).stream()
                .filter(o -> o.getStatus() == WeighingOperationStatus.FINALIZED && positive(o.getAfmContribution()))
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.getDate()))));
        List<Section> sections = new ArrayList<>();
        List<List<Object>> summary = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal afm = BigDecimal.ZERO;
        for (var month : months.entrySet()) {
            List<List<Object>> rows = new ArrayList<>();
            BigDecimal mBase = BigDecimal.ZERO;
            BigDecimal mAfm = BigDecimal.ZERO;
            for (WeighingOperation o : month.getValue()) {
                rows.add(Arrays.asList(o.getNumber(), date(o.getDate()), o.getWorkPoint().getName(), counterpart(o, null),
                        o.getPartner() != null ? o.getPartner().getCui() : "persoană fizică", o.getAfmBase(),
                        o.getAfmContribution()));
                mBase = mBase.add(o.getAfmBase());
                mAfm = mAfm.add(o.getAfmContribution());
            }
            String due = date(month.getKey().plusMonths(1).atDay(25));
            sections.add(new Section("Luna " + month.getKey().format(MONTH) + " — de plătit până la " + due,
                    List.of(Column.integer("Nr."), Column.text("Data"), Column.text("Depozit"), Column.text("Furnizor"),
                            Column.text("CUI"), Column.lei("Baza (lei)"), Column.lei("2% (lei)")),
                    rows, Arrays.asList("Total luna", null, null, null, null, mBase, mAfm)));
            summary.add(Arrays.asList(month.getKey().format(MONTH), mBase, mAfm, due));
            base = base.add(mBase);
            afm = afm.add(mAfm);
        }
        sections.add(new Section("Pe luni", List.of(Column.text("Luna"), Column.lei("Baza (lei)"),
                Column.lei("Contribuția 2% (lei)"), Column.text("Scadența")), summary,
                Arrays.asList("Total", base, afm, null)));
        return report(s, "Contribuția de 2% la Fondul pentru mediu", sections,
                money("OUG 196/2005 art. 9 alin. (1) lit. a): 2% din valoarea deșeurilor cumpărate, reținut la sursă, "
                        + "declarat și plătit până pe 25 a lunii următoare.", chosen),
                List.of("Întocmit", "Administrator"));
    }

    /** D100 (lunar, 25 a lunii următoare) și D205 (anual, pe beneficiar, ultima zi a lui februarie). */
    private DepotReport incomeTax(Scope s, WorkPoint chosen) {
        Map<YearMonth, List<WeighingOperation>> months = new TreeMap<>(operationsOf(s, WeighingOperationType.IN).stream()
                .filter(o -> o.getStatus() == WeighingOperationStatus.FINALIZED && positive(o.getIncomeTax()))
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.getDate()))));
        List<List<Object>> d100 = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (var month : months.entrySet()) {
            BigDecimal mBase = month.getValue().stream().map(WeighingOperation::getIncomeTaxBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mTax = month.getValue().stream().map(WeighingOperation::getIncomeTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            d100.add(Arrays.asList(month.getKey().format(MONTH), month.getValue().size(), mBase, mTax,
                    date(month.getKey().plusMonths(1).atDay(25))));
            base = base.add(mBase);
            tax = tax.add(mTax);
        }
        int year = s.to().getYear();
        List<List<Object>> d205 = new ArrayList<>();
        for (var b : operationRepository.findTaxedBeneficiaries(s.tenantId(), LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31))) {
            d205.add(Arrays.asList(b.getName(), b.getCnp(), b.getBase(), b.getTax()));
        }
        LocalDate d205Due = LocalDate.of(year + 1, 2, 1).with(TemporalAdjusters.lastDayOfMonth());
        return report(s, "Impozitul reținut la achiziția de metale de la persoane fizice", List.of(
                new Section("D100 — pe luni", List.of(Column.text("Luna"), Column.integer("Operațiuni"),
                        Column.lei("Baza (lei)"), Column.lei("Impozit 10% (lei)"), Column.text("Scadența")), d100,
                        Arrays.asList("Total", null, base, tax, null)),
                new Section("D205 — anul " + year + " (termen " + date(d205Due) + ")", List.of(Column.text("Beneficiar"),
                        Column.text("CNP"), Column.lei("Baza (lei)"), Column.lei("Impozit reținut (lei)")), d205, null,
                        "Niciun beneficiar în " + year + ".")),
                money("Codul fiscal art. 114 alin. (2) lit. m²) și art. 115: 10% din valoarea metalelor cumpărate de la "
                        + "persoane fizice. D205 cuprinde tot anul datei „până la”.", chosen),
                List.of("Întocmit", "Administrator"));
    }

    /**
     * Plățile în numerar către persoane fizice, pe zi × persoană, ca verificarea de la cântar ({@code cash-check}):
     * valoarea minus 2% și impozitul, orice operațiune neanulată; peste 10.000 lei pe zi e marcat (Legea 70/2015 art. 4).
     */
    private DepotReport cash(Scope s, WorkPoint chosen) {
        List<WeighingOperation> ops = operationsOf(s, WeighingOperationType.IN).stream()
                .filter(o -> o.getNaturalPerson() != null && o.getPaymentMethod() == PaymentMethod.NUMERAR
                        && o.getStatus() != WeighingOperationStatus.CANCELLED)
                .toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        Map<String, List<WeighingOperation>> days = new TreeMap<>(ops.stream().collect(Collectors.groupingBy(
                o -> o.getDate() + "|" + o.getNaturalPerson().getName() + "|" + o.getNaturalPerson().getId())));
        List<List<Object>> rows = new ArrayList<>();
        for (List<WeighingOperation> group : days.values()) {
            BigDecimal paid = BigDecimal.ZERO;
            for (WeighingOperation o : group) {
                paid = paid.add(paid(o, lines.getOrDefault(o.getId(), List.of())));
            }
            WeighingOperation first = group.get(0);
            rows.add(Arrays.asList(date(first.getDate()), first.getNaturalPerson().getName(), group.size(),
                    group.stream().map(o -> String.valueOf(o.getNumber())).collect(Collectors.joining(", ")), paid,
                    paid.compareTo(WeighingDocumentService.CASH_DAILY_LIMIT) > 0 ? "DA" : null));
        }
        return report(s, "Plăți în numerar către persoane fizice", List.of(new Section(null, List.of(
                Column.text("Data"), Column.text("Persoana"), Column.integer("Operațiuni"), Column.text("Numere"),
                Column.lei("Plătit în numerar (lei)"), Column.text("Peste 10.000 lei")), rows, null)),
                money("Legea 70/2015 art. 4: cel mult 10.000 lei pe zi în numerar către aceeași persoană. Suma plătită = "
                        + "valoarea minus 2% AFM și impozitul reținut.", chosen),
                List.of());
    }

    // --- parteneri --------------------------------------------------------------------------------------------------

    /** Borderourile și NIR-urile intrărilor de la persoane fizice, apoi totalul pe fiecare persoană. */
    private DepotReport individuals(Scope s) {
        List<WeighingOperation> ops = operationsOf(s, WeighingOperationType.IN).stream()
                .filter(o -> o.getNaturalPerson() != null && o.getStatus() == WeighingOperationStatus.FINALIZED)
                .toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        boolean prices = s.prices();
        List<List<Object>> documents = new ArrayList<>();
        Map<UUID, Object[]> perPerson = new LinkedHashMap<>();
        for (WeighingOperation o : ops) {
            List<WasteMovement> ls = lines.getOrDefault(o.getId(), List.of()).stream().filter(s::shows).toList();
            BigDecimal kg = ls.stream().map(DepotReportService::kg).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal value = ls.stream().map(WasteMovement::getTotalValue).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal afm = orZero(o.getAfmContribution());
            BigDecimal tax = orZero(o.getIncomeTax());
            BigDecimal paid = value.subtract(afm).subtract(tax);
            var person = o.getNaturalPerson();
            List<Object> row = new ArrayList<>(Arrays.asList(o.getBorderouNumber(), o.getReceptionNoteNumber(),
                    date(o.getDate()), o.getWorkPoint().getName(), person.getName(), person.getCnp(), kg));
            if (prices) {
                row.addAll(Arrays.asList(value, afm, tax, paid));
            }
            row.add(payment(o.getPaymentMethod()));
            documents.add(row);
            Object[] total = perPerson.computeIfAbsent(person.getId(), k -> new Object[]{person.getName(), person.getCnp(),
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            total[2] = (Integer) total[2] + 1;
            total[3] = ((BigDecimal) total[3]).add(kg);
            total[4] = ((BigDecimal) total[4]).add(value);
            total[5] = ((BigDecimal) total[5]).add(tax);
            total[6] = ((BigDecimal) total[6]).add(paid);
        }
        List<Column> docColumns = new ArrayList<>(List.of(Column.integer("Borderou nr."), Column.integer("NIR nr."),
                Column.text("Data"), Column.text("Depozit"), Column.text("Persoana"), Column.text("CNP"),
                Column.kg("Cantitate (kg)")));
        List<Column> personColumns = new ArrayList<>(List.of(Column.text("Persoana"), Column.text("CNP"),
                Column.integer("Operațiuni"), Column.kg("Cantitate (kg)")));
        if (prices) {
            docColumns.addAll(List.of(Column.lei("Valoare (lei)"), Column.lei("2% AFM (lei)"),
                    Column.lei("Impozit (lei)"), Column.lei("Plătit (lei)")));
            personColumns.addAll(List.of(Column.lei("Valoare (lei)"), Column.lei("Impozit (lei)"),
                    Column.lei("Plătit (lei)")));
        }
        docColumns.add(Column.text("Plata"));
        List<List<Object>> persons = perPerson.values().stream()
                .map(t -> (List<Object>) new ArrayList<>(Arrays.asList(t).subList(0, prices ? 7 : 4)))
                .sorted(Comparator.comparing(r -> (String) r.get(0))).toList();
        return report(s, "Borderouri și situația pe persoane fizice", List.of(
                        new Section("Borderouri și NIR-uri", docColumns, documents, null),
                        new Section("Pe persoană, în perioadă", personColumns, persons, null)),
                List.of("Borderoul (OMFP 2634/2015, 14-4-13) pentru liniile plătite, NIR-ul (14-3-1A) pentru cele preluate "
                        + "gratuit. CNP-ul se cere doar la metale (OUG 31/2011)."),
                List.of("Întocmit", "Administrator"));
    }

    /** Tot ce a adus sau a luat o firmă în perioadă, linie cu linie, cu impuritățile; prețul numai pentru cine îl vede. */
    private DepotReport partnerCard(Scope s, UUID partnerId) {
        Partner partner = partnerRepository.findByIdAndCompany_Id(partnerId, s.tenantId())
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        List<WeighingOperation> ops = operationsOf(s, null).stream()
                .filter(o -> o.getStatus() == WeighingOperationStatus.FINALIZED && o.getPartner() != null
                        && partner.getId().equals(o.getPartner().getId()))
                .toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(ops);
        boolean prices = s.prices();
        List<List<Object>> rows = new ArrayList<>();
        Map<String, Object[]> totals = new TreeMap<>();
        for (WeighingOperation o : ops) {
            for (WasteMovement m : lines.getOrDefault(o.getId(), List.of())) {
                if (!s.shows(m)) {
                    continue;
                }
                BigDecimal net = m.getNetKg() == null ? m.getQuantity() : m.getNetKg();
                BigDecimal impurities = net.subtract(m.getQuantity());
                String direction = o.getType() == WeighingOperationType.IN ? "Intrare" : "Ieșire";
                List<Object> row = new ArrayList<>(Arrays.asList(date(m.getDate()), direction, o.getNumber(),
                        m.getWorkPoint().getName(), article(m), code(m), net, m.getQuantity(), impurities));
                if (prices) {
                    row.addAll(Arrays.asList(m.getUnitPrice(), m.getTotalValue()));
                }
                rows.add(row);
                Object[] t = totals.computeIfAbsent(direction + "|" + article(m) + "|" + code(m),
                        k -> new Object[]{direction, article(m), code(m), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                t[3] = ((BigDecimal) t[3]).add(m.getQuantity());
                t[4] = ((BigDecimal) t[4]).add(impurities);
                t[5] = ((BigDecimal) t[5]).add(orZero(m.getTotalValue()));
            }
        }
        List<Column> columns = new ArrayList<>(List.of(Column.text("Data"), Column.text("Direcția"),
                Column.integer("Nr."), Column.text("Depozit"), Column.text("Sortiment"), Column.text("Cod deșeu"),
                Column.kg("Neto (kg)"), Column.kg("Final (kg)"), Column.kg("Impurități (kg)")));
        List<Column> totalColumns = new ArrayList<>(List.of(Column.text("Direcția"), Column.text("Sortiment"),
                Column.text("Cod deșeu"), Column.kg("Final (kg)"), Column.kg("Impurități (kg)")));
        if (prices) {
            columns.addAll(List.of(Column.lei("Preț (lei/kg)"), Column.lei("Valoare (lei)")));
            totalColumns.add(Column.lei("Valoare (lei)"));
        }
        List<List<Object>> perArticle = totals.values().stream()
                .map(t -> (List<Object>) new ArrayList<>(Arrays.asList(t).subList(0, prices ? 6 : 5))).toList();
        DepotReport base = report(s, "Fișa partenerului", List.of(new Section(null, columns, rows, null),
                new Section("Pe sortiment", totalColumns, perArticle, null)),
                List.of("Impuritățile = neto − final. Contează operațiunile finalizate."), List.of());
        List<String> heading = new ArrayList<>(base.heading());
        heading.add("Partener: " + partner.getName() + (partner.getCui() == null ? "" : " · CUI " + partner.getCui()));
        return new DepotReport(base.title(), heading, base.sections(), base.notes(), base.signatures());
    }

    // --- ajutoare ---------------------------------------------------------------------------------------------------

    private List<WeighingOperation> operationsOf(Scope s, WeighingOperationType type) {
        return operationRepository.findForScreen(s.tenantId(), type, s.from(), s.to()).stream()
                .filter(s::shows)
                .sorted(Comparator.comparing(WeighingOperation::getDate).thenComparing(WeighingOperation::getType)
                        .thenComparingInt(WeighingOperation::getNumber))
                .toList();
    }

    private Map<UUID, List<WasteMovement>> linesOf(List<WeighingOperation> ops) {
        if (ops.isEmpty()) {
            return Map.of();
        }
        return movementRepository.findAllByWeighingOperation_IdInOrderByLineNoAsc(
                        ops.stream().map(WeighingOperation::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getWeighingOperation().getId()));
    }

    private DepotReport report(Scope s, String title, List<Section> sections, List<String> notes, List<String> signatures) {
        Company c = s.company();
        List<String> heading = new ArrayList<>();
        heading.add(c.getCui() == null ? c.getName() : c.getName() + " · CUI " + c.getCui());
        heading.add(period(s));
        heading.add(s.depot() == null ? "Depozit: toate" : "Depozit: " + s.depot().getName());
        return new DepotReport(title, heading, sections, notes, signatures);
    }

    private static String period(Scope s) {
        return "Perioada: " + date(s.from()) + " – " + date(s.to());
    }

    /** Banii sunt ai firmei: dacă s-a ales un depozit, nota spune de ce nu s-a aplicat. */
    private static List<String> money(String basis, WorkPoint chosen) {
        return chosen == null ? List.of(basis)
                : List.of(basis, "Se declară pe toată firma, deci raportul cuprinde toate depozitele, nu doar "
                + chosen.getName() + ".");
    }

    private static BigDecimal paid(WeighingOperation o, List<WasteMovement> lines) {
        DepotRetentions.Amounts retained = o.getAfmContribution() != null && o.getIncomeTax() != null
                ? new DepotRetentions.Amounts(o.getAfmBase(), o.getAfmContribution(), o.getIncomeTaxBase(), o.getIncomeTax())
                : DepotRetentions.of(o, lines);
        BigDecimal value = lines.stream().map(WasteMovement::getTotalValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return value.subtract(retained.afm()).subtract(retained.incomeTax());
    }

    private static BigDecimal sum(List<WasteMovement> lines, WasteOperation operation) {
        return lines.stream().filter(m -> m.getOperation() == operation).map(DepotReportService::kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(BigDecimal.valueOf(1000)) : m.getQuantity();
    }

    private static String typeOf(WeighingOperation o) {
        return switch (o.getType()) {
            case IN -> "Intrare";
            case OUT -> "Ieșire";
            case TRANSFER -> "Transfer";
            case PROCESSING -> "Balotare";
            case ADJUSTMENT -> "Ajustare";
        };
    }

    /** Cu cine s-a făcut: partenerul, persoana fizică sau, la transfer, celălalt depozit. */
    private static String counterpart(WeighingOperation o, WasteMovement m) {
        if (o.getType() == WeighingOperationType.TRANSFER) {
            boolean received = m != null && m.getOperation() == WasteOperation.TRANSFERRED_IN;
            WorkPoint other = received ? o.getWorkPoint() : o.getTargetWorkPoint();
            return other == null ? null : other.getName();
        }
        if (o.getPartner() != null) {
            return o.getPartner().getName();
        }
        return o.getNaturalPerson() == null ? null : o.getNaturalPerson().getName();
    }

    private static String article(WasteMovement m) {
        return m.getArticle() == null ? null : m.getArticle().getName();
    }

    private static String code(WasteMovement m) {
        return m.getWasteCode().getCode() + (m.getWasteCode().isHazardous() ? "*" : "");
    }

    private static String label(String article, String code, String name) {
        return (article == null ? "Fără sortiment" : article) + " — " + code + " " + name;
    }

    private static String payment(PaymentMethod method) {
        return method == null ? null : method == PaymentMethod.NUMERAR ? "Numerar" : "Virament";
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String date(LocalDate day) {
        return day == null ? null : day.format(DATE);
    }

    /** „Depozit Central Florești” → „depozit-central-floresti”, pentru numele fișierului. */
    static String slug(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
