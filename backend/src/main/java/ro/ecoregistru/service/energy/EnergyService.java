package ro.ecoregistru.service.energy;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.exception.ServiceUnavailableException;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.MovementAttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.EnergyCarriersRequest;
import ro.ecoregistru.controller.request.EnergyConsumptionRequest;
import ro.ecoregistru.controller.request.EnergyContactRequest;
import ro.ecoregistru.controller.request.EnergyDeclarationRequest;
import ro.ecoregistru.controller.request.EnergyMeasureRequest;
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.controller.response.EnergyYearSummary;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.EnergyCarrier;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.energy.EnergyYear.CarrierTotal;
import ro.ecoregistru.service.energy.EnergyYear.Cell;
import ro.ecoregistru.service.export.EnergyAnnex1;
import ro.ecoregistru.service.export.EnergyAnnex1XlsxGenerator;
import ro.ecoregistru.service.export.EnergyAnnex1PdfGenerator;
import ro.ecoregistru.service.export.EnergyDeclarationDocxGenerator;
import ro.ecoregistru.service.export.EnergyDeclarationPdfGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/** Reads and writes the energy sheet (Anexa 1) of the current tenant; the figures come from {@link EnergyYear}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyService {

    private static final int FIRST_YEAR = 2020;

    private final CompanyRepository companyRepository;
    private final EnergyCarrierUsedRepository carrierRepository;
    private final EnergyConsumptionRepository consumptionRepository;
    private final EnergyDeclarationRepository declarationRepository;
    private final EnergySavingMeasureRepository measureRepository;
    private final CloudinaryStorageService storageService;
    private final EnergyAnnex1XlsxGenerator annex1Generator;
    private final EnergyDeclarationDocxGenerator declarationGenerator;
    private final EnergyAnnex1PdfGenerator annex1PdfGenerator;
    private final EnergyDeclarationPdfGenerator declarationPdfGenerator;
    private final ReportingDeadlineRepository deadlineRepository;

    @Transactional(readOnly = true)
    public EnergySheetResponse sheet(int year) {
        checkYear(year);
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        return build(company, year);
    }

    /**
     * Anexa 1 of the year as {@code .xlsx}. Refused at 1000 tep and above: the annex is the form for consumers
     * under 1000 tep, and above it the law asks for more than a form.
     */
    @Transactional(readOnly = true)
    public byte[] annex1(int year) {
        return annex1Generator.render(annex1Data(year));
    }

    /**
     * Aceeași Anexa 1, ca PDF de deschis în tab (din 05.10.2026): aceleași date și același refuz de la 1000 tep în
     * sus, pentru că vin din același {@link #annex1Data}.
     */
    @Transactional(readOnly = true)
    public byte[] annex1Pdf(int year) {
        return annex1PdfGenerator.render(annex1Data(year));
    }

    /** What both renderings of Anexa 1 print, built once; 422 at 1000 tep and above. */
    private EnergyAnnex1 annex1Data(int year) {
        checkYear(year);
        Company company = company();
        EnergySheetResponse sheet = build(company, year);
        if (sheet.overThreshold()) {
            throw new UnprocessableEntityException(ENERGY_OVER_THRESHOLD);
        }
        // A ticked carrier is in the map, with null while months are missing; an unticked one is absent.
        Map<EnergyCarrier, BigDecimal> quantities = new EnumMap<>(EnergyCarrier.class);
        Map<EnergyCarrier, BigDecimal> teps = new EnumMap<>(EnergyCarrier.class);
        for (CarrierTotal total : sheet.totals()) {
            quantities.put(total.carrier(), total.quantity());
            teps.put(total.carrier(), total.tep());
        }
        return new EnergyAnnex1(year, company, quantities,
                teps.get(EnergyCarrier.COAL), teps.get(EnergyCarrier.OTHER_FUEL), sheet.declaration());
    }

    /** The Declarație (.docx) of the year, with the company's details as stored. */
    @Transactional(readOnly = true)
    public byte[] declarationDocx(int year) {
        checkYear(year);
        return declarationGenerator.render(company(), year);
    }

    /** Aceeași Declarație, ca PDF de deschis în tab (din 05.10.2026). */
    @Transactional(readOnly = true)
    public byte[] declarationPdf(int year) {
        checkYear(year);
        return declarationPdfGenerator.render(company(), year);
    }

    /**
     * The carriers of one year. Only that year's rows change: the sheets, Anexa 1 and dossiers of the other years keep
     * theirs. An empty list removes the year's own set, so it reads the latest earlier one again.
     */
    @Transactional
    public EnergySheetResponse saveCarriers(EnergyCarriersRequest request) {
        int year = request.year();
        checkYear(year);
        Company company = company();
        Set<EnergyCarrier> wanted = EnumSet.noneOf(EnergyCarrier.class);
        wanted.addAll(request.carriers());
        storeCarriers(company, year, wanted);
        return build(company, year);
    }

    private void storeCarriers(Company company, int year, Set<EnergyCarrier> wanted) {
        Map<EnergyCarrier, EnergyCarrierUsed> existing = new EnumMap<>(EnergyCarrier.class);
        carrierRepository.findAllByCompany_IdAndYear(company.getId(), year)
                .forEach(c -> existing.put(c.getCarrier(), c));

        // Unticking removes only this row: the months stay, so ticking again brings them back.
        existing.forEach((carrier, row) -> {
            if (!wanted.contains(carrier)) {
                carrierRepository.delete(row);
            }
        });
        for (EnergyCarrier carrier : wanted) {
            if (!existing.containsKey(carrier)) {
                carrierRepository.save(EnergyCarrierUsed.builder()
                        .company(company).year(year).carrier(carrier).updatedAt(Instant.now()).build());
            }
        }
        carrierRepository.flush();
    }

    /** The year in force: its own set when it has rows, otherwise the set of the latest earlier year that has one. */
    private CarrierSet carriersOf(UUID companyId, int year) {
        List<EnergyCarrierUsed> rows =
                carrierRepository.findAllByCompany_IdAndYearLessThanEqualOrderByYearDesc(companyId, year);
        Set<EnergyCarrier> set = EnumSet.noneOf(EnergyCarrier.class);
        if (rows.isEmpty()) {
            return new CarrierSet(set, false);
        }
        int from = rows.get(0).getYear();
        rows.stream().filter(r -> r.getYear() == from).forEach(r -> set.add(r.getCarrier()));
        return new CarrierSet(set, from != year);
    }

    private record CarrierSet(Set<EnergyCarrier> carriers, boolean inherited) {}

    @Transactional
    public EnergySheetResponse saveCell(EnergyConsumptionRequest request) {
        checkYear(request.year());
        Company company = company();
        EnergyCarrier carrier = request.carrier();
        CarrierSet used = carriersOf(company.getId(), request.year());
        if (!used.carriers().contains(carrier)) {
            throw new BadRequestException(ENERGY_CARRIER_NOT_USED);
        }
        if (request.tep() != null && carrier.coefficient().isPresent()) {
            throw new BadRequestException(ENERGY_TEP_NOT_ALLOWED);
        }
        // The first figure on an inherited set makes the set the year's own: from now on, changing an earlier
        // year's carriers no longer changes a sheet that already has figures.
        if (used.inherited() && request.quantity() != null) {
            storeCarriers(company, request.year(), used.carriers());
        }

        EnergyConsumption existing = consumptionRepository
                .findByCompany_IdAndYearAndCarrierAndMonth(
                        company.getId(), request.year(), carrier, request.month())
                .orElse(null);
        if (request.quantity() == null) {
            if (existing != null) {
                consumptionRepository.delete(existing);
                consumptionRepository.flush();
            }
        } else {
            EnergyConsumption cell = existing != null ? existing : EnergyConsumption.builder()
                    .company(company).year(request.year()).carrier(carrier).month(request.month()).build();
            cell.setQuantity(request.quantity());
            cell.setTep(request.tep());
            cell.setUpdatedAt(Instant.now());
            consumptionRepository.save(cell);
        }
        return build(company, request.year());
    }

    @Transactional
    public EnergySheetResponse saveDeclaration(EnergyDeclarationRequest request) {
        checkYear(request.year());
        Company company = company();
        EnergyDeclaration declaration = declarationRepository
                .findByCompany_IdAndYear(company.getId(), request.year())
                .orElseGet(() -> EnergyDeclaration.builder().company(company).year(request.year()).build());
        declaration.setSme(request.sme());
        declaration.setAuditDate(request.auditDate());
        declaration.setAuditor(request.auditor());
        declaration.setAuditScope(request.auditScope());
        declaration.setAuditSharePct(request.auditSharePct());
        declaration.setPoimInterest(request.poimInterest());
        declaration.setPoimProject(request.poimProject());
        declaration.setUpdatedAt(Instant.now());
        declaration = declarationRepository.save(declaration);

        // Delete first and flush, so the new positions 1..n never meet the old ones.
        measureRepository.deleteByDeclaration_Id(declaration.getId());
        measureRepository.flush();
        List<EnergyMeasureRequest> measures = request.measures() == null ? List.of() : request.measures();
        int position = 1;
        for (EnergyMeasureRequest m : measures) {
            measureRepository.save(EnergySavingMeasure.builder()
                    .declaration(declaration).position(position++).name(m.name())
                    .costEstimated(m.costEstimated()).costActual(m.costActual())
                    .savingsTepEstimated(m.savingsTepEstimated()).savingsTepActual(m.savingsTepActual())
                    .savingsCostEstimated(m.savingsCostEstimated()).savingsCostActual(m.savingsCostActual())
                    .updatedAt(Instant.now()).build());
        }
        measureRepository.flush();
        return build(company, request.year());
    }

    @Transactional
    public EnergySheetResponse saveContact(EnergyContactRequest request) {
        Company company = company();
        company.setFax(request.fax());
        company.setWebsite(request.website());
        company.setActivitySector(request.activitySector());
        company.setEnergyContactName(request.name());
        company.setEnergyContactEmail(request.email());
        company.setEnergyContactPhone(request.phone());
        company.setEnergyContactMobile(request.mobile());
        company.setEnergyContactAttestedOn(request.attestedOn());
        companyRepository.saveAndFlush(company);
        return build(company, LocalDate.now(DeadlineService.ZONE).getYear());
    }

    /** The filing receipt of EfEnClima for the year: new file up first, row saved, the old file dropped last. */
    @Transactional
    public EnergySheetResponse attachReceipt(int year, MultipartFile file) {
        checkYear(year);
        Company company = company();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ENERGY_RECEIPT_PDF_ONLY);
        }
        if (file.getSize() > MovementAttachmentService.MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException(ATTACHMENT_TOO_LARGE);
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType()) || !startsWithPdfMagic(file)) {
            throw new BadRequestException(ENERGY_RECEIPT_PDF_ONLY);
        }
        EnergyDeclaration declaration = declarationRepository.findByCompany_IdAndYear(company.getId(), year)
                .orElseGet(() -> EnergyDeclaration.builder().company(company).year(year).build());
        String oldId = declaration.getReceiptPublicId();
        String oldResource = declaration.getReceiptResourceType();
        String oldDelivery = declaration.getReceiptDeliveryType();

        var stored = storageService.upload(file, "energy/" + company.getId() + "/" + year);
        Instant now = Instant.now();
        declaration.setReceiptPublicId(stored.publicId());
        declaration.setReceiptResourceType(stored.resourceType());
        declaration.setReceiptDeliveryType(stored.deliveryType());
        declaration.setReceiptFormat(stored.format());
        declaration.setReceiptFileName(MovementAttachmentService.safeFileName(file.getOriginalFilename()));
        declaration.setReceiptSizeBytes(file.getSize());
        declaration.setReceiptUploadedAt(now);
        declaration.setUpdatedAt(now);
        try {
            declarationRepository.saveAndFlush(declaration);
        } catch (RuntimeException e) {
            dropQuietly(stored.publicId(), stored.resourceType(), stored.deliveryType());
            throw e;
        }

        // The old file goes only once the new row is committed; a rollback must leave it in place.
        if (oldId != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dropQuietly(oldId, oldResource, oldDelivery);
                }
            });
        }
        return build(company, year);
    }

    private void dropQuietly(String publicId, String resourceType, String deliveryType) {
        try {
            storageService.delete(publicId, resourceType, deliveryType);
        } catch (RuntimeException e) {
            log.warn("Energy receipt file {} could not be deleted", publicId, e);
        }
    }

    private static boolean startsWithPdfMagic(MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] head = in.readNBytes(4);
            return head.length == 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
        } catch (java.io.IOException e) {
            return false;
        }
    }

    public record ReceiptContent(byte[] bytes, String fileName) {}

    @Transactional(readOnly = true)
    public ReceiptContent receiptContent(int year) {
        checkYear(year);
        EnergyDeclaration d = declarationRepository.findByCompany_IdAndYear(TenantContext.require(), year)
                .filter(x -> x.getReceiptPublicId() != null)
                .orElseThrow(() -> new NotFoundException(ATTACHMENT_NOT_FOUND));
        try {
            String url = storageService.signedUrl(d.getReceiptPublicId(), d.getReceiptResourceType(),
                    d.getReceiptDeliveryType(), d.getReceiptFormat());
            return new ReceiptContent(storageService.fetch(url), d.getReceiptFileName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException(ATTACHMENT_FETCH_FAILED);
        } catch (Exception e) {
            log.warn("Energy receipt fetch failed for year={}", year, e);
            throw new ServiceUnavailableException(ATTACHMENT_FETCH_FAILED);
        }
    }

    /** The receipt bytes, for the filing package. */
    public byte[] receiptBytes(int year) {
        return receiptContent(year).bytes();
    }

    /**
     * The year's filing package as a zip: Anexa 1 (missing at 1000 tep and above, where the form does not apply),
     * the Declarație, and the EfEnClima receipt when there is one.
     */
    @Transactional(readOnly = true)
    public byte[] dossier(int year) {
        checkYear(year);
        Company company = company();
        EnergySheetResponse sheet = build(company, year);
        var out = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(out, java.nio.charset.StandardCharsets.UTF_8)) {
            if (!sheet.overThreshold()) {
                addEntry(zip, "Anexa 1 consum energie " + year + ".xlsx", annex1(year));
            }
            addEntry(zip, "Declaratie energie " + year + ".docx", declarationDocx(year));
            if (sheet.receipt() != null) {
                addEntry(zip, "Confirmare depunere EfEnClima " + year + ".pdf", receiptBytes(year));
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void addEntry(java.util.zip.ZipOutputStream zip, String name, byte[] bytes)
            throws java.io.IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    /** The years with data, newest first, each with the day the deadline was ticked and the day of the receipt. */
    @Transactional(readOnly = true)
    public List<EnergyYearSummary> years() {
        Company company = company();
        return consumptionRepository.yearsWithData(company.getId()).stream().map(year -> {
            EnergySheetResponse sheet = build(company, year);
            LocalDate filedOn = deadlineRepository.findByCompany_IdAndReportTypeAndDueDate(
                            company.getId(), ReportType.ENERGY_ANNUAL, LocalDate.of(year + 1, 6, 30))
                    .filter(d -> d.getStatus() == DeadlineStatus.DONE && d.getCompletedAt() != null)
                    .map(d -> d.getCompletedAt().atZone(DeadlineService.ZONE).toLocalDate())
                    .orElse(null);
            // Without a ticked deadline row (the 30.06.2026 one exists only computed, it can't be ticked), the
            // receipt is the proof of filing: the frontend shows its day instead.
            LocalDate receiptOn = sheet.receipt() == null ? null
                    : sheet.receipt().uploadedAt().atZone(DeadlineService.ZONE).toLocalDate();
            return new EnergyYearSummary(year, sheet.monthsComplete(), sheet.overThreshold(), filedOn, receiptOn,
                    sheet.receipt() != null);
        }).toList();
    }

    private Company company() {
        return companyRepository.findById(TenantContext.require())
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    private void checkYear(int year) {
        if (year < FIRST_YEAR || year > LocalDate.now(DeadlineService.ZONE).getYear()) {
            throw new UnprocessableEntityException(ENERGY_YEAR_INVALID);
        }
    }

    private EnergySheetResponse build(Company company, int year) {
        UUID companyId = company.getId();
        Set<EnergyCarrier> used = carriersOf(companyId, year).carriers();

        // Rows of an unticked carrier stay in the database but are not part of the sheet.
        List<Cell> cells = consumptionRepository.findAllByCompany_IdAndYear(companyId, year).stream()
                .filter(c -> used.contains(c.getCarrier()))
                .map(c -> new Cell(c.getCarrier(), c.getMonth(), c.getQuantity(), c.getTep()))
                .sorted(Comparator.comparing(Cell::carrier).thenComparingInt(Cell::month))
                .toList();
        EnergyYear energyYear = EnergyYear.of(year, used, cells);

        List<Cell> shown = cells.stream()
                .map(c -> c.tep() != null ? c : new Cell(c.carrier(), c.month(), c.quantity(),
                        c.carrier().coefficient()
                                .map(k -> c.quantity().multiply(k).setScale(4, RoundingMode.HALF_UP))
                                .orElse(null)))
                .toList();

        EnergySheetResponse.Declaration declaration = declarationRepository
                .findByCompany_IdAndYear(companyId, year)
                .map(d -> new EnergySheetResponse.Declaration(
                        d.getSme(), d.getAuditDate(), d.getAuditor(), d.getAuditScope(), d.getAuditSharePct(),
                        d.getPoimInterest(), d.getPoimProject(),
                        measureRepository.findAllByDeclaration_IdOrderByPositionAsc(d.getId()).stream()
                                .map(m -> new EnergySheetResponse.Measure(m.getPosition(), m.getName(),
                                        m.getCostEstimated(), m.getCostActual(),
                                        m.getSavingsTepEstimated(), m.getSavingsTepActual(),
                                        m.getSavingsCostEstimated(), m.getSavingsCostActual()))
                                .toList()))
                .orElseGet(() -> new EnergySheetResponse.Declaration(
                        null, null, null, null, null, null, null, List.of()));
        EnergySheetResponse.Receipt receipt = declarationRepository.findByCompany_IdAndYear(companyId, year)
                .filter(d -> d.getReceiptPublicId() != null)
                .map(d -> new EnergySheetResponse.Receipt(
                        d.getReceiptFileName(), d.getReceiptSizeBytes(), d.getReceiptUploadedAt()))
                .orElse(null);

        EnergySheetResponse.Contact contact = new EnergySheetResponse.Contact(
                company.getFax(), company.getWebsite(), company.getActivitySector(),
                company.getEnergyContactName(), company.getEnergyContactEmail(),
                company.getEnergyContactPhone(), company.getEnergyContactMobile(),
                company.getEnergyContactAttestedOn());

        return new EnergySheetResponse(year,
                Arrays.stream(EnergyCarrier.values()).filter(used::contains).toList(),
                shown, energyYear.totals(), energyYear.totalTep(), energyYear.knownTep(),
                energyYear.monthsComplete(), energyYear.overThreshold(), declaration, contact, receipt);
    }
}
