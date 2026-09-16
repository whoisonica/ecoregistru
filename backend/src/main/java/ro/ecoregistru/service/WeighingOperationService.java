package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.DepotRetentionReport;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Operațiunile de depozit (V46): capul numerotat (D1.2) și cântarul pe linii (D1.4). Stările și
 * plata vin în punctele următoare ale feliei.
 *
 * <p><b>Numerotarea</b> e pe firmă și pe tip, fără goluri la creările obișnuite: maximul + 1, sub un
 * lacăt consultativ ținut până la commit ({@link WeighingOperationRepository#lockNumbering}).
 *
 * <p><b>Liniile</b> sunt {@link WasteMovement}-uri în registrul art. 48, ca stocul și registrele să le
 * citească fără rescriere. Se salvează tot formularul odată: cât timp operațiunea e în lucru,
 * liniile vechi se șterg și se scriu cele trimise. Nu se pierde nimic numărat, fiindcă o linie de
 * operațiune în lucru nu contează nicăieri (D1.3).
 *
 * <p><b>Prețurile</b> (D1.8) le vede doar cine are voie după {@link ro.ecoregistru.enums.PriceVisibility}:
 * celorlalți răspunsul le vine gol, iar ce trimit ei nu schimbă prețul salvat.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeighingOperationService {

    WeighingOperationRepository operationRepository;
    WasteMovementRepository movementRepository;
    WasteArticleRepository articleRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;
    NaturalPersonRepository naturalPersonRepository;
    DriverRepository driverRepository;
    ro.ecoregistru.repository.AppUserRepository userRepository;
    ro.ecoregistru.service.export.DepotRegisterGenerator registerGenerator;

    @Transactional
    public WeighingOperationResponse create(WeighingOperationRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
        WeighingOperationType type = requireAvailableType(request.type());
        if (request.date() == null) {
            throw new BusinessException(WEIGHING_OPERATION_DATE_REQUIRED);
        }
        if (request.partnerId() != null && request.naturalPersonId() != null) {
            throw new BusinessException(WEIGHING_OPERATION_ONE_COUNTERPARTY);
        }
        if (request.naturalPersonId() != null && type != WeighingOperationType.IN) {
            throw new BusinessException(WEIGHING_OPERATION_PERSON_ONLY_IN);
        }

        WorkPoint workPoint = workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        Partner partner = request.partnerId() == null ? null
                : partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        NaturalPerson person = request.naturalPersonId() == null ? null
                : naturalPersonRepository.findByIdAndCompany_Id(request.naturalPersonId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(NATURAL_PERSON_NOT_FOUND));
        Driver driver = request.driverId() == null ? null
                : driverRepository.findByIdAndCompany_Id(request.driverId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(DRIVER_NOT_FOUND));

        operationRepository.lockNumbering(tenantId + ":" + type);
        Integer max = operationRepository.findMaxNumber(tenantId, type);

        WeighingOperation operation = WeighingOperation.builder()
                .company(company)
                .workPoint(workPoint)
                .type(type)
                .number(max == null ? 1 : max + 1)
                .date(request.date())
                .partner(partner)
                .naturalPerson(person)
                .origin(resolveOrigin(request, partner, person))
                .driver(driver)
                .driverName(firstNonBlank(request.driverName(), driver == null ? null : driver.getName()))
                .vehicleRegistration(firstNonBlank(request.vehicleRegistration(),
                        driver == null ? null : driver.getVehicleRegistration()))
                .orderNumber(blankToNull(request.orderNumber()))
                .paymentMethod(request.paymentMethod())
                .receiptNumber(blankToNull(request.receiptNumber()))
                // Declarația e a persoanei fizice; pe o operațiune cu partener n-are obiect.
                .ownHousehold(person == null ? null : request.ownHousehold())
                .notes(blankToNull(request.notes()))
                .status(WeighingOperationStatus.IN_PROGRESS)
                .createdBy(SecurityUtils.currentUser().getId())
                .build();
        operationRepository.save(operation);
        return toResponse(operation, List.of(), pricesVisible(company));
    }

    /**
     * Capul unei operațiuni în lucru: data, depozitul, de la cine, șoferul, plata. Tipul nu se
     * schimbă — numerotarea e separată pe intrări și ieșiri, deci o intrare greșit pornită se
     * anulează, nu se convertește.
     *
     * <p>Liniile poartă instantaneul capului (data, depozitul, partenerul, mașina), fiindcă ele sunt
     * cele care ajung în registre; de aceea se rescriu odată cu el. Regulile care atârnă de
     * vânzător se verifică din nou aici: un carton devenit „de la o persoană fizică” trece, o șină
     * de cale ferată nu.
     */
    @Transactional
    public WeighingOperationResponse update(UUID id, WeighingOperationRequest request) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (operation.getStatus() != WeighingOperationStatus.IN_PROGRESS) {
            throw new BusinessException(WEIGHING_OPERATION_NOT_EDITABLE);
        }
        if (request.type() != null && request.type() != operation.getType()) {
            throw new BusinessException(WEIGHING_OPERATION_TYPE_NOT_EDITABLE);
        }
        if (request.date() == null) {
            throw new BusinessException(WEIGHING_OPERATION_DATE_REQUIRED);
        }
        if (request.partnerId() != null && request.naturalPersonId() != null) {
            throw new BusinessException(WEIGHING_OPERATION_ONE_COUNTERPARTY);
        }
        if (request.naturalPersonId() != null && operation.getType() != WeighingOperationType.IN) {
            throw new BusinessException(WEIGHING_OPERATION_PERSON_ONLY_IN);
        }

        WorkPoint workPoint = workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        Partner partner = request.partnerId() == null ? null
                : partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        NaturalPerson person = request.naturalPersonId() == null ? null
                : naturalPersonRepository.findByIdAndCompany_Id(request.naturalPersonId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(NATURAL_PERSON_NOT_FOUND));
        Driver driver = request.driverId() == null ? null
                : driverRepository.findByIdAndCompany_Id(request.driverId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(DRIVER_NOT_FOUND));

        operation.setWorkPoint(workPoint);
        operation.setDate(request.date());
        operation.setPartner(partner);
        operation.setNaturalPerson(person);
        operation.setOrigin(resolveOrigin(request, partner, person));
        operation.setDriver(driver);
        operation.setDriverName(firstNonBlank(request.driverName(), driver == null ? null : driver.getName()));
        operation.setVehicleRegistration(firstNonBlank(request.vehicleRegistration(),
                driver == null ? null : driver.getVehicleRegistration()));
        operation.setOrderNumber(blankToNull(request.orderNumber()));
        operation.setPaymentMethod(request.paymentMethod());
        operation.setReceiptNumber(blankToNull(request.receiptNumber()));
        operation.setOwnHousehold(person == null ? null : request.ownHousehold());
        operation.setNotes(blankToNull(request.notes()));

        List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        for (WasteMovement line : lines) {
            if (person != null && line.getArticle() != null && line.getArticle().isForbiddenFromIndividuals()) {
                throw new BusinessException(WEIGHING_LINE_ARTICLE_FORBIDDEN_FROM_INDIVIDUALS);
            }
            line.setWorkPoint(workPoint);
            line.setDate(request.date());
            line.setPartner(partner);
            line.setDriverName(operation.getDriverName());
            line.setVehicleRegistration(operation.getVehicleRegistration());
        }
        requireMetalIdentity(operation, lines);
        operationRepository.saveAndFlush(operation);
        return toResponse(operation, lines, pricesVisible(operation.getCompany()));
    }

    /**
     * D1.4 — cântarul. Totul se validează înainte să se atingă ceva, apoi liniile vechi se șterg și
     * se scriu cele noi, în ordinea trimisă.
     */
    @Transactional
    public WeighingOperationResponse replaceLines(UUID id, WeighingLinesRequest request) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (operation.getStatus() != WeighingOperationStatus.IN_PROGRESS) {
            throw new BusinessException(WEIGHING_OPERATION_NOT_EDITABLE);
        }
        requireNonNegative(request.grossKg());
        requireNonNegative(request.tareKg());
        if (request.grossKg() != null && request.tareKg() != null
                && request.tareKg().compareTo(request.grossKg()) >= 0) {
            throw new BusinessException(WEIGHING_OPERATION_TARE_ABOVE_GROSS);
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessException(WEIGHING_LINES_REQUIRED);
        }

        UUID userId = SecurityUtils.currentUser().getId();
        boolean pricesVisible = pricesVisible(operation.getCompany());
        List<WasteMovement> previous = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        Map<UUID, Iterator<BigDecimal>> keptPrices = pricesVisible ? Map.of() : pricesByArticle(previous);
        List<WasteMovement> lines = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            WeighingLinesRequest.Line sent = request.lines().get(i);
            BigDecimal unitPrice = pricesVisible ? sent.unitPrice() : nextKept(keptPrices, sent.articleId());
            lines.add(line(operation, sent, unitPrice, i + 1, tenantId, userId));
        }

        requireMetalIdentity(operation, lines);
        operation.setGrossKg(request.grossKg());
        operation.setTareKg(request.tareKg());
        movementRepository.deleteAll(previous);
        movementRepository.flush();
        movementRepository.saveAll(lines);
        return toResponse(operation, lines, pricesVisible);
    }

    /**
     * D1.5 — finalizarea: din acest moment liniile intră în stoc și în registre (D1.3) și nu se mai
     * modifică. O fac doar cei care aprobă (decizia proprietarului, 15.09.2026: admin și consultant);
     * operatorul de la cântar introduce, dar nu dă drumul în registre.
     */
    @Transactional
    public WeighingOperationResponse finalizeOperation(UUID id) {
        UUID tenantId = TenantContext.require();
        var user = SecurityUtils.currentUser();
        requireApprover(user);
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (operation.getStatus() != WeighingOperationStatus.IN_PROGRESS) {
            throw new BusinessException(WEIGHING_OPERATION_NOT_EDITABLE);
        }
        List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        if (lines.isEmpty()) {
            throw new BusinessException(WEIGHING_OPERATION_NO_LINES);
        }
        // A doua oară aici: fișa persoanei se poate edita între cântărire și finalizare.
        requireMetalIdentity(operation, lines);
        requireOwnHouseholdDeclaration(operation, lines);
        // D1.9 și D1.10 — reținerile se calculează acum și rămân așa: o cotă schimbată mâine nu
        // rescrie declarația de luna trecută.
        DepotRetentions.Amounts retained = DepotRetentions.of(operation, lines);
        operation.setAfmBase(retained.afmBase());
        operation.setAfmContribution(retained.afm());
        operation.setIncomeTaxBase(retained.incomeTaxBase());
        operation.setIncomeTax(retained.incomeTax());
        operation.setStatus(WeighingOperationStatus.FINALIZED);
        operation.setFinalizedAt(java.time.Instant.now());
        operation.setFinalizedBy(user.getId());
        operationRepository.saveAndFlush(operation);
        return toResponse(operation, lines, pricesVisible(operation.getCompany()));
    }

    /**
     * D1.5 — anularea: cere motiv, păstrează cine și când, nu șterge nimic. Merge și pe o operațiune
     * finalizată (documentul justificativ nu se șterge, se anulează — Legea 82/1991); liniile ies
     * atunci din stoc și din registre, iar cache-ul de evidență o vede prin {@code updatedAt}.
     */
    @Transactional
    public WeighingOperationResponse cancel(UUID id, String reason) {
        UUID tenantId = TenantContext.require();
        var user = SecurityUtils.currentUser();
        requireApprover(user);
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (operation.getStatus() == WeighingOperationStatus.CANCELLED) {
            throw new BusinessException(WEIGHING_OPERATION_ALREADY_CANCELLED);
        }
        String motive = blankToNull(reason);
        if (motive == null) {
            throw new BusinessException(WEIGHING_OPERATION_CANCEL_REASON_REQUIRED);
        }
        operation.setStatus(WeighingOperationStatus.CANCELLED);
        operation.setCancelledAt(java.time.Instant.now());
        operation.setCancelledBy(user.getId());
        operation.setCancelReason(motive);
        operationRepository.saveAndFlush(operation);
        return toResponse(operation, movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id),
                pricesVisible(operation.getCompany()));
    }

    /**
     * D1.7 — la metal, borderoul cere de la persoana fizică numele, seria și numărul actului, CNP-ul și
     * domiciliul (OUG 31/2011 art. 1 alin. (1^2) lit. b) pct. (ii)). La hârtie sau plastic nicio lege nu
     * le cere, deci acolo ajunge numele (Legea 190/2018 art. 4; `surse-oficiale.md` §18.3).
     */
    private static void requireMetalIdentity(WeighingOperation operation, List<WasteMovement> lines) {
        NaturalPerson person = operation.getNaturalPerson();
        if (person == null || lines.stream().noneMatch(l -> l.getArticle() != null && l.getArticle().isMetal())) {
            return;
        }
        boolean complete = person.getCnp() != null && ro.ecoregistru.util.ValidCnp.Validator.isValidCnp(person.getCnp())
                && person.getIdentification() != null && !person.getIdentification().isBlank()
                && person.getAddress() != null && !person.getAddress().isBlank();
        if (!complete) {
            throw new BusinessException(NATURAL_PERSON_METAL_IDENTITY_REQUIRED);
        }
    }

    /**
     * Metalul cumpărat de la o persoană fizică poate proveni numai din gospodăria ei (OUG 31/2011
     * art. 1 alin. (1^1), amendă 100.000–150.000 lei), iar borderoul poartă declarația ei. Fără bifă
     * nu se finalizează: documentul ar pleca necompletat.
     */
    private static void requireOwnHouseholdDeclaration(WeighingOperation operation, List<WasteMovement> lines) {
        if (operation.getNaturalPerson() == null || Boolean.TRUE.equals(operation.getOwnHousehold())) {
            return;
        }
        if (lines.stream().anyMatch(l -> l.getArticle() != null && l.getArticle().isMetal())) {
            throw new BusinessException(WEIGHING_OPERATION_OWN_HOUSEHOLD_REQUIRED);
        }
    }

    /** Controllerul are aceeași regulă; aici e jumătatea care ține și fără HTTP. */
    private static void requireApprover(ro.ecoregistru.entity.AppUser user) {
        if (!APPROVERS.contains(user.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Doar administratorul sau consultantul finalizează și anulează operațiuni.");
        }
    }

    private static final java.util.Set<ro.ecoregistru.enums.Role> APPROVERS = java.util.EnumSet.of(
            ro.ecoregistru.enums.Role.PLATFORM_ADMIN, ro.ecoregistru.enums.Role.ADMIN,
            ro.ecoregistru.enums.Role.CONSULTANT);

    @Transactional(readOnly = true)
    public WeighingOperationResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        return toResponse(operation, movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id),
                pricesVisible(operation.getCompany()));
    }

    /** Toate operațiunile firmei; filtrele ecranului trec prin {@link #list(WeighingOperationType, Integer, Integer)}. */
    @Transactional(readOnly = true)
    public List<WeighingOperationResponse> list() {
        return list(null, null, null);
    }

    /**
     * Lista ecranului: o direcție (sau amândouă), o lună sau un an (sau tot). Ordinea e cea a
     * cântarului — ultima operațiune sus, iar în aceeași zi numărul mare primul.
     */
    @Transactional(readOnly = true)
    public List<WeighingOperationResponse> list(WeighingOperationType type, Integer year, Integer month) {
        UUID tenantId = TenantContext.require();
        java.time.LocalDate from = java.time.LocalDate.of(1900, 1, 1);
        java.time.LocalDate to = java.time.LocalDate.of(9999, 12, 31);
        if (year != null) {
            java.time.YearMonth period = month == null ? null : java.time.YearMonth.of(year, month);
            from = period == null ? java.time.LocalDate.of(year, 1, 1) : period.atDay(1);
            to = period == null ? java.time.LocalDate.of(year, 12, 31) : period.atEndOfMonth();
        }
        List<WeighingOperation> operations = operationRepository.findForScreen(tenantId, type, from, to);
        if (operations.isEmpty()) {
            return List.of();
        }
        boolean pricesVisible = pricesVisible(operations.get(0).getCompany());
        Map<UUID, List<WasteMovement>> lines = movementRepository.findAllByWeighingOperation_IdInOrderByLineNoAsc(
                        operations.stream().map(WeighingOperation::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getWeighingOperation().getId()));
        return operations.stream()
                .map(o -> toResponse(o, lines.getOrDefault(o.getId(), List.of()), pricesVisible))
                .toList();
    }

    /**
     * D1.14 — registrul intrărilor și ieșirilor, o lună sau un an, amândouă direcțiile, toate stările,
     * în ordinea cântarului: zi cu zi, intrările înaintea ieșirilor, numărul crescător. Îl scoate oricine
     * vede lista; prețul intră doar pentru cine îl vede (D1.8).
     */
    @Transactional(readOnly = true)
    public byte[] renderRegister(int year, Integer month) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        java.time.YearMonth period = month == null ? null : java.time.YearMonth.of(year, month);
        java.time.LocalDate from = period == null ? java.time.LocalDate.of(year, 1, 1) : period.atDay(1);
        java.time.LocalDate to = period == null ? java.time.LocalDate.of(year, 12, 31) : period.atEndOfMonth();
        List<WeighingOperation> operations = new ArrayList<>(operationRepository.findForScreen(tenantId, null, from, to));
        operations.sort(java.util.Comparator.comparing(WeighingOperation::getDate)
                .thenComparing(WeighingOperation::getType)
                .thenComparingInt(WeighingOperation::getNumber));
        Map<UUID, List<WasteMovement>> lines = operations.isEmpty() ? Map.of()
                : movementRepository.findAllByWeighingOperation_IdInOrderByLineNoAsc(
                                operations.stream().map(WeighingOperation::getId).toList())
                        .stream().collect(Collectors.groupingBy(m -> m.getWeighingOperation().getId()));
        Map<UUID, String> userNames = new HashMap<>();
        userRepository.findAllById(operations.stream().map(WeighingOperation::getCancelledBy)
                        .filter(java.util.Objects::nonNull).distinct().toList())
                .forEach(u -> userNames.put(u.getId(), fullName(u)));
        String label = period == null ? "Anul " + year
                : "Luna " + String.format("%02d.%d", month, year);
        String name = company.getCui() == null ? company.getName() : company.getName() + " · CUI " + company.getCui();
        return registerGenerator.xlsx(name, label, operations, lines, userNames, pricesVisible(company));
    }

    private static String fullName(ro.ecoregistru.entity.AppUser user) {
        String name = java.util.stream.Stream.of(user.getFirstName(), user.getLastName())
                .filter(v -> v != null && !v.isBlank()).collect(Collectors.joining(" "));
        return name.isEmpty() ? user.getEmail() : name;
    }

    /**
     * D1.9 și D1.10 — raportul reținerilor. Cu lună, e cel lunar (AFM și D100, 25 a lunii următoare);
     * fără lună, e anul întreg, cu beneficiarii pentru D205.
     *
     * <p>Îl vede cine <b>administrează</b> firma și cine <b>vede prețurile</b>: sunt bani calculați din
     * prețuri (D1.8), iar lista de beneficiari poartă CNP-uri întregi.
     */
    @Transactional(readOnly = true)
    public DepotRetentionReport retentions(int year, Integer month) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        requireApprover(SecurityUtils.currentUser());
        if (!pricesVisible(company)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Raportul reținerilor arată prețurile depozitului.");
        }
        // O lună în afara lui 1–12 cade pe `DateTimeException`, tratată deja ca 400 (AdviceController).
        java.time.YearMonth period = month == null ? null : java.time.YearMonth.of(year, month);
        java.time.LocalDate from = period == null ? java.time.LocalDate.of(year, 1, 1) : period.atDay(1);
        java.time.LocalDate to = period == null ? java.time.LocalDate.of(year, 12, 31) : period.atEndOfMonth();
        // Lunar: 25 a lunii următoare. Anual: ultima zi a lui februarie, pentru declarația pe beneficiar.
        java.time.LocalDate dueDate = month == null
                ? java.time.LocalDate.of(year + 1, 2, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                : to.plusDays(1).withDayOfMonth(25);

        var totals = operationRepository.sumRetentions(tenantId, from, to);
        var beneficiaries = operationRepository.findTaxedBeneficiaries(tenantId, from, to).stream()
                .map(b -> new DepotRetentionReport.Beneficiary(b.getPersonId(), b.getName(), b.getCnp(),
                        b.getBase(), b.getTax()))
                .toList();
        return new DepotRetentionReport(year, month, from, to, dueDate,
                DepotRetentions.AFM_RATE, totals.getAfmBase(), totals.getAfm(),
                DepotRetentions.INCOME_TAX_RATE, totals.getIncomeTaxBase(), totals.getIncomeTax(),
                totals.getOperations(), beneficiaries);
    }

    private WasteMovement line(WeighingOperation operation, WeighingLinesRequest.Line line, BigDecimal unitPrice,
                               int lineNo, UUID tenantId, UUID userId) {
        if (line.articleId() == null) {
            throw new BusinessException(WEIGHING_LINE_ARTICLE_REQUIRED);
        }
        WasteArticle article = articleRepository.findByIdAndCompany_Id(line.articleId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
        // OUG 31/2011 art. 1 alin. (1): amendă 100.000–150.000 lei și revocarea autorizației de colectare.
        if (operation.getNaturalPerson() != null && article.isForbiddenFromIndividuals()) {
            throw new BusinessException(WEIGHING_LINE_ARTICLE_FORBIDDEN_FROM_INDIVIDUALS);
        }
        BigDecimal net = resolveNet(line);
        BigDecimal finalKg = line.finalKg() == null ? net : line.finalKg();
        if (finalKg.signum() <= 0) {
            throw new BusinessException(WEIGHING_LINE_FINAL_NOT_POSITIVE);
        }
        if (finalKg.compareTo(net) > 0) {
            throw new BusinessException(WEIGHING_LINE_FINAL_ABOVE_NET);
        }
        if (unitPrice != null && unitPrice.signum() < 0) {
            throw new BusinessException(WEIGHING_LINE_PRICE_NEGATIVE);
        }

        return WasteMovement.builder()
                .company(operation.getCompany())
                .workPoint(operation.getWorkPoint())
                .date(operation.getDate())
                .wasteCode(article.getWasteCode())
                .article(article)
                .quantity(finalKg)
                .unit(Unit.KG)
                .operation(resolveOperation(operation, line.operationCode()))
                .register(WasteRegister.ART_48)
                .operationCode(line.operationCode())
                .partner(operation.getPartner())
                .driverName(operation.getDriverName())
                .vehicleRegistration(operation.getVehicleRegistration())
                .weighingOperation(operation)
                .lineNo(lineNo)
                .grossKg(line.grossKg())
                .tareKg(line.tareKg())
                .netKg(net)
                .unitPrice(unitPrice)
                .totalValue(unitPrice == null ? null
                        : finalKg.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP))
                .notes(blankToNull(line.notes()))
                .deleted(false)
                .createdBy(userId)
                .build();
    }

    /** D1.8 — vede prețurile cel care lucrează acum pe firma asta? Regula e în {@code PriceVisibility}. */
    private static boolean pricesVisible(Company company) {
        return company.getPriceVisibility().visibleTo(SecurityUtils.currentUser().getRole());
    }

    /**
     * D1.8 — cine nu vede prețurile nici nu le poate schimba sau șterge. Formularul lui vine fără preț,
     * deci prețul se ia din liniile salvate, pe sortiment și în ordine: a doua linie de „Cupru” trimisă
     * primește prețul celei de-a doua linii de „Cupru” salvate. O linie fără pereche rămâne fără preț,
     * iar valoarea se recalculează din cantitatea finală nouă.
     */
    private static Map<UUID, Iterator<BigDecimal>> pricesByArticle(List<WasteMovement> previous) {
        Map<UUID, List<BigDecimal>> prices = new HashMap<>();
        for (WasteMovement m : previous) {
            if (m.getArticle() != null) {
                prices.computeIfAbsent(m.getArticle().getId(), k -> new ArrayList<>()).add(m.getUnitPrice());
            }
        }
        Map<UUID, Iterator<BigDecimal>> kept = new HashMap<>();
        prices.forEach((article, list) -> kept.put(article, list.iterator()));
        return kept;
    }

    private static BigDecimal nextKept(Map<UUID, Iterator<BigDecimal>> kept, UUID articleId) {
        Iterator<BigDecimal> prices = articleId == null ? null : kept.get(articleId);
        return prices != null && prices.hasNext() ? prices.next() : null;
    }

    /**
     * Neto e brut − tara când amândouă sunt cântărite; altfel se trece direct. Dacă vin toate trei,
     * trebuie să se potrivească: un neto scris peste o cântărire care spune altceva e o greșeală de
     * tastare, nu o alegere.
     */
    private static BigDecimal resolveNet(WeighingLinesRequest.Line line) {
        requireNonNegative(line.grossKg());
        requireNonNegative(line.tareKg());
        if (line.grossKg() != null && line.tareKg() != null) {
            BigDecimal net = line.grossKg().subtract(line.tareKg());
            if (net.signum() <= 0) {
                throw new BusinessException(WEIGHING_LINE_NET_NOT_POSITIVE);
            }
            if (line.netKg() != null && line.netKg().compareTo(net) != 0) {
                throw new BusinessException(WEIGHING_LINE_NET_MISMATCH);
            }
            return net;
        }
        if (line.netKg() == null) {
            throw new BusinessException(WEIGHING_LINE_NET_REQUIRED);
        }
        if (line.netKg().signum() <= 0) {
            throw new BusinessException(WEIGHING_LINE_NET_NOT_POSITIVE);
        }
        return line.netKg();
    }

    /**
     * Intrarea e o preluare, fără cod. Ieșirea poartă pe fiecare linie codul R/D (decizia
     * proprietarului, 15.09.2026), iar familia codului dă operația, ca la mișcarea obișnuită.
     */
    private static WasteOperation resolveOperation(WeighingOperation operation, WasteOperationCode code) {
        if (operation.getType() == WeighingOperationType.IN) {
            if (code != null) {
                throw new BusinessException(OPERATION_CODE_NOT_ALLOWED);
            }
            return WasteOperation.COLLECTED;
        }
        if (code == null) {
            throw new BusinessException(WEIGHING_LINE_OPERATION_CODE_REQUIRED);
        }
        var allowed = operation.getCompany().getAuthorizedOperationCodes();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(code)) {
            throw new BusinessException(OPERATION_CODE_NOT_IN_PROFILE);
        }
        return code.isRecovery() ? WasteOperation.RECOVERED : WasteOperation.DISPOSED;
    }

    private static void requireNonNegative(BigDecimal weight) {
        if (weight != null && weight.signum() < 0) {
            throw new BusinessException(WEIGHING_LINE_WEIGHT_NEGATIVE);
        }
    }

    private static WeighingOperationType requireAvailableType(WeighingOperationType type) {
        if (type == null) {
            throw new BusinessException(WEIGHING_OPERATION_TYPE_REQUIRED);
        }
        if (!type.isAvailable()) {
            throw new BusinessException(WEIGHING_OPERATION_TYPE_UNAVAILABLE);
        }
        return type;
    }

    /**
     * La o persoană fizică originea e mereu „populație”. La un partener, ce s-a ales pe operațiune,
     * altfel ce spune fișa lui; gol dacă nu spune nimeni — nu se ghicește o rubrică de registru.
     */
    private static PackagingOrigin resolveOrigin(WeighingOperationRequest request, Partner partner,
                                                 NaturalPerson person) {
        if (person != null) {
            return PackagingOrigin.POPULATIE;
        }
        if (request.origin() == PackagingOrigin.POPULATIE) {
            return null;
        }
        return request.origin() != null ? request.origin()
                : partner == null ? null : partner.getPackagingOrigin();
    }

    private static String firstNonBlank(String typed, String fallback) {
        String value = blankToNull(typed);
        return value != null ? value : blankToNull(fallback);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static WeighingOperationResponse toResponse(WeighingOperation o, List<WasteMovement> lines,
                                                        boolean pricesVisible) {
        Partner partner = o.getPartner();
        NaturalPerson person = o.getNaturalPerson();
        return new WeighingOperationResponse(o.getId(), o.getType(), o.getNumber(), o.getDate(),
                o.getWorkPoint().getId(), o.getWorkPoint().getName(),
                partner == null ? null : partner.getId(), partner == null ? null : partner.getName(),
                person == null ? null : person.getId(), person == null ? null : person.getName(),
                o.getOrigin(), o.getDriverName(), o.getVehicleRegistration(), o.getOrderNumber(),
                o.getStatus(), o.getNotes(), o.getGrossKg(), o.getTareKg(),
                o.getPaymentMethod(), o.getReceiptNumber(), o.getOwnHousehold(),
                pricesVisible ? o.getAfmBase() : null, pricesVisible ? o.getAfmContribution() : null,
                pricesVisible ? o.getIncomeTaxBase() : null, pricesVisible ? o.getIncomeTax() : null,
                DepotRetentions.AFM_RATE, DepotRetentions.INCOME_TAX_RATE,
                o.getCancelReason(),
                lines.stream().map(m -> toLine(m, pricesVisible)).toList());
    }

    /** D1.8 — prețul și valoarea pleacă doar către cine le vede. */
    private static WeighingOperationResponse.Line toLine(WasteMovement m, boolean pricesVisible) {
        WasteArticle article = m.getArticle();
        return new WeighingOperationResponse.Line(m.getId(), m.getLineNo() == null ? 0 : m.getLineNo(),
                article == null ? null : article.getId(), article == null ? null : article.getName(),
                m.getWasteCode().getCode(), m.getGrossKg(), m.getTareKg(), m.getNetKg(), m.getQuantity(),
                pricesVisible ? m.getUnitPrice() : null, pricesVisible ? m.getTotalValue() : null,
                m.getOperationCode());
    }
}
