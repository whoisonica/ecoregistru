package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.PackagingMarketRequest;
import ro.ecoregistru.controller.response.PackagingMarketResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.PackagingMarketEntry;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.mapper.WasteMovementMapper;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.repository.PackagingMarketEntryRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.PackagingDeclaration;
import ro.ecoregistru.service.export.PackagingDeclarationBuilder;
import ro.ecoregistru.service.export.PackagingDeclarationGenerator;
import ro.ecoregistru.service.export.PackagingDeclarationXlsGenerator;
import ro.ecoregistru.service.export.PackagingAnexa3;
import ro.ecoregistru.service.export.PackagingAnexa3Builder;
import ro.ecoregistru.service.export.PackagingAnexa3Generator;
import ro.ecoregistru.service.export.PackagingAnexa3XlsGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ro.ecoregistru.exception.BusinessException;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.PACKAGING_OPERATOR_ROLE_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * The packaging module: Anexa 1 Ambalaje (Ordinul 794/2012) and everything the tab centralises.
 *
 * <p><b>One source, the movements.</b> Both tables of the declaration are computed from the
 * movements recorded on {@code 15 01 xx} codes — tabelul 2 from the handovers, tabelul 1 from the
 * material and kind of packaging those movements now carry ({@code V26}). Nothing is typed twice,
 * and the screen shows the same movements the document will use, so a figure that looks wrong can
 * be traced to the line that produced it.
 *
 * <p><b>The stored market rows survive as an override.</b> Tabelul 1 is legally about goods put on
 * the market, not about waste, so a company that knows its market figure differs from what the
 * movements show may state it — and then the form prints what they stated, for that material only.
 * Nothing writes those rows by itself.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PackagingService {

    PackagingMarketEntryRepository entryRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    WasteMovementMapper movementMapper;
    PackagingDeclarationBuilder builder;
    PackagingDeclarationGenerator pdfGenerator;
    PackagingDeclarationXlsGenerator xlsGenerator;
    WorkPointRepository workPointRepository;
    PackagingAnexa3Builder anexa3Builder;
    PackagingAnexa3Generator anexa3PdfGenerator;
    PackagingAnexa3XlsGenerator anexa3XlsGenerator;

    /**
     * The packaging movements of a year — every movement on a {@code 15 01 xx} code, newest first,
     * exactly as the tab lists them.
     *
     * <p>This is the register the declaration is built from, so the screen can show what is missing
     * on each line: the material where the code does not settle it, the kind of packaging, the
     * weight that has not come back from the recipient, the R/D code.
     */
    @Transactional(readOnly = true)
    public List<WasteMovementResponse> movements(int year) {
        return yearMovements(TenantContext.require(), year).stream()
                .filter(m -> PackagingMaterial.isPackagingCode(m.getWasteCode().getCode()))
                .sorted(Comparator.comparing(WasteMovement::getDate).reversed())
                .map(movementMapper::toResponse)
                .toList();
    }

    /**
     * The stored overrides for a year, one row per material, empty where the client has not
     * overridden anything. The computed figures live on the declaration, not here.
     */
    @Transactional(readOnly = true)
    public List<PackagingMarketResponse> marketEntries(int year) {
        UUID tenantId = TenantContext.require();
        List<PackagingMarketEntry> stored = entryRepository.findAllByCompany_IdAndYear(tenantId, year);

        List<PackagingMarketResponse> rows = new ArrayList<>();
        for (PackagingMaterial material : PackagingMaterial.values()) {
            PackagingMarketEntry e = stored.stream()
                    .filter(x -> x.getMaterial() == material)
                    .findFirst().orElse(null);
            rows.add(new PackagingMarketResponse(
                    material, year,
                    e == null ? null : e.getSalesPackaging(),
                    e == null ? null : e.getPrimaryTotal(),
                    e == null ? null : e.getPrimaryReusable(),
                    e == null ? null : e.getSecondaryTotal(),
                    e == null ? null : e.getSecondaryReusable(),
                    e == null ? null : e.getHazardousContent()));
        }
        return rows;
    }

    /**
     * Overrides one material row of tabelul 1, or drops the override when every figure comes back
     * empty — an all-empty row means "go back to what the movements say", not "declare nothing".
     */
    @Transactional
    public PackagingMarketResponse saveMarketEntry(PackagingMarketRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        PackagingMarketEntry existing = entryRepository
                .findByCompany_IdAndYearAndMaterial(tenantId, request.year(), request.material())
                .orElse(null);

        if (isEmpty(request)) {
            if (existing != null) {
                entryRepository.delete(existing);
            }
            return new PackagingMarketResponse(request.material(), request.year(),
                    null, null, null, null, null, null);
        }

        PackagingMarketEntry entry = existing != null ? existing : PackagingMarketEntry.builder()
                .company(company).year(request.year()).material(request.material())
                .build();

        entry.setSalesPackaging(request.salesPackaging());
        entry.setPrimaryTotal(request.primaryTotal());
        entry.setPrimaryReusable(request.primaryReusable());
        entry.setSecondaryTotal(request.secondaryTotal());
        entry.setSecondaryReusable(request.secondaryReusable());
        entry.setHazardousContent(request.hazardousContent());
        entry.setUpdatedAt(Instant.now());
        entryRepository.save(entry);

        return new PackagingMarketResponse(
                entry.getMaterial(), entry.getYear(), entry.getSalesPackaging(),
                entry.getPrimaryTotal(), entry.getPrimaryReusable(),
                entry.getSecondaryTotal(), entry.getSecondaryReusable(),
                entry.getHazardousContent());
    }

    private boolean isEmpty(PackagingMarketRequest r) {
        return r.salesPackaging() == null && r.primaryTotal() == null && r.primaryReusable() == null
                && r.secondaryTotal() == null && r.secondaryReusable() == null
                && r.hazardousContent() == null;
    }

    /** The whole declaration, assembled from the year's movements plus any stored overrides. */
    @Transactional(readOnly = true)
    public PackagingDeclaration declaration(int year) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        List<PackagingMarketEntry> entries = entryRepository
                .findAllByCompany_IdAndYear(tenantId, year);
        return builder.build(company, year, entries, yearMovements(tenantId, year));
    }

    /**
     * Renders the declaration. {@code .xls} is the form the authority receives — the format
     * Ordinul 794/2012 art. 6 names on sight — and the PDF is the paper copy the same article asks
     * for alongside it. Anything else falls to the PDF rather than inventing a third form.
     */
    @Transactional(readOnly = true)
    public byte[] render(int year, ExportFormat format) {
        PackagingDeclaration declaration = declaration(year);
        return format == ExportFormat.XLS
                ? xlsGenerator.render(declaration)
                : pdfGenerator.render(declaration);
    }

    /** Tabelul 1 as the screen shows it before printing: computed, with the overrides applied. */
    @Transactional(readOnly = true)
    public List<PackagingDeclaration.MarketRow> marketRows(int year) {
        return declaration(year).marketRows();
    }

    /**
     * Tabelul 2 as the screen shows it before printing — so the client can see what the form will
     * say without downloading it to find out.
     */
    @Transactional(readOnly = true)
    public List<PackagingDeclaration.HandoverRow> handovers(int year) {
        return declaration(year).handoverRows();
    }

    /** The movements neither table could use, and what each of them is missing. */
    @Transactional(readOnly = true)
    public List<PackagingDeclaration.UnclassifiedRow> unclassified(int year) {
        return declaration(year).unclassified();
    }

    // ------------------------------------------------------------------ anexa 3

    /**
     * Anexa 3 la Ordinul 794/2012 for one work point and one year, assembled but not yet rendered
     * — the screen shows exactly this before anybody downloads a file.
     *
     * <p>Per work point because art. 4 alin. (4) says so ("Raportarea se realizează pentru fiecare
     * punct de lucru în parte") and alin. (3) sends each to the agency in whose area it lies, so
     * two work points in two counties are two reports to two addressees. Passing {@code null} for
     * the work point builds the whole company instead, which is what the screen shows an account
     * that has only ever had one.
     */
    @Transactional(readOnly = true)
    public PackagingAnexa3 anexa3(int year, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        WorkPoint workPoint = workPointId == null ? null : workPointRepository.findById(workPointId)
                .filter(wp -> wp.getCompany().getId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        return anexa3Builder.build(company, workPoint, year, yearMovements(tenantId, year));
    }

    /**
     * Renders it. {@code .xls} is what the authority receives and the PDF is the paper copy beside
     * it — art. 6 asks for both, in those words.
     *
     * <p>Refuses when the company profile does not say which table applies. The generators would
     * throw anyway; catching it here turns it into the message that names what to go and answer,
     * instead of a 500 from inside a spreadsheet library.
     */
    @Transactional(readOnly = true)
    public byte[] renderAnexa3(int year, UUID workPointId, ExportFormat format) {
        PackagingAnexa3 document = anexa3(year, workPointId);
        if (!document.printable()) {
            throw new BusinessException(PACKAGING_OPERATOR_ROLE_REQUIRED);
        }
        return format == ExportFormat.XLS
                ? anexa3XlsGenerator.render(document)
                : anexa3PdfGenerator.render(document);
    }

    private List<WasteMovement> yearMovements(UUID tenantId, int year) {
        return movementRepository.findAllByCompany_IdAndDeletedFalseAndDateBetween(
                tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }
}
