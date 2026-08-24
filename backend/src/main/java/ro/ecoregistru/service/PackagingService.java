package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.PackagingMarketRequest;
import ro.ecoregistru.controller.response.PackagingMarketResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.PackagingMarketEntry;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PackagingMarketEntryRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.PackagingDeclaration;
import ro.ecoregistru.service.export.PackagingDeclarationBuilder;
import ro.ecoregistru.service.export.PackagingDeclarationGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;

/**
 * The packaging module: Anexa 1 Ambalaje (Ordinul 794/2012) and the figures behind it.
 *
 * <p>Two halves with different owners. <b>Tabelul 2</b> — the packaging waste handed over — is
 * computed from the movements, so nobody types it twice. <b>Tabelul 1</b> — what the company put
 * on the national market — cannot be computed from anything the application holds: it is about
 * goods sold, not waste recorded. So it is stored, one row per material and year, and what nobody
 * answered prints as an empty cell.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PackagingService {

    PackagingMarketEntryRepository entryRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    PackagingDeclarationBuilder builder;
    PackagingDeclarationGenerator generator;

    /** The market figures for a year, one row per material, in the order the form prints them. */
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
     * Saves one material row. An all-empty row is stored as all-empty rather than as zeroes: the
     * difference between "nothing was put on the market" and "nobody has answered yet" is the
     * client's to state, and the form prints it as they left it.
     */
    @Transactional
    public PackagingMarketResponse saveMarketEntry(PackagingMarketRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        PackagingMarketEntry entry = entryRepository
                .findByCompany_IdAndYearAndMaterial(tenantId, request.year(), request.material())
                .orElseGet(() -> PackagingMarketEntry.builder()
                        .company(company).year(request.year()).material(request.material())
                        .build());

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

    /** The whole declaration, assembled: the stored table 1 plus the computed table 2. */
    @Transactional(readOnly = true)
    public PackagingDeclaration declaration(int year) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        List<PackagingMarketEntry> entries = entryRepository
                .findAllByCompany_IdAndYear(tenantId, year);
        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return builder.build(company, year, entries, movements);
    }

    @Transactional(readOnly = true)
    public byte[] render(int year) {
        return generator.render(declaration(year));
    }

    /**
     * The packaging waste handed over in a year, as the screen shows it before printing — so the
     * client can see what table 2 will say without downloading a PDF to find out.
     */
    @Transactional(readOnly = true)
    public List<PackagingDeclaration.HandoverRow> handovers(int year) {
        return declaration(year).handoverRows().stream()
                .sorted(Comparator.comparing(r -> r.material().ordinal()))
                .toList();
    }
}
