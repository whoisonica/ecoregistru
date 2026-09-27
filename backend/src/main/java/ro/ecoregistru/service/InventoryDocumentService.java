package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Inventory;
import ro.ecoregistru.entity.StockOpening;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ScaleRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.InventoryDocumentsGenerator;

import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/** D3.5 — PDF-urile inventarului și ale notei de preluare, cu aceleași reguli de acces ca ecranul. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryDocumentService {

    public static final List<String> DOCUMENTS = List.of("decizie", "declaratie", "lista", "pv");

    InventoryService inventories;
    StockOpeningService openings;
    CompanyRepository companyRepository;
    ScaleRepository scaleRepository;
    ScaleService scaleService;
    InventoryDocumentsGenerator generator;

    @Transactional(readOnly = true)
    public byte[] inventory(UUID id, String document) {
        Inventory inv = inventories.requireForDocuments(id);
        Company company = company();
        return switch (document) {
            case "decizie" -> generator.decision(company, inv);
            case "declaratie" -> generator.declaration(company, inv);
            case "lista" -> generator.list(company, inv);
            case "pv" -> generator.pv(company, inv,
                    inventories.during(inv).stream().map(o -> new InventoryDocumentsGenerator.DuringNote(
                            InventoryService.typeLabel(o.getType()) + " nr. " + o.getNumber() + " din "
                                    + o.getDate().format(InventoryService.DATE),
                            o.getPartner() != null ? o.getPartner().getName()
                                    : o.getTargetWorkPoint() != null ? o.getTargetWorkPoint().getName() : null)).toList(),
                    scaleRepository.findAllForCompany(inv.getCompanyId()).stream()
                            .filter(s -> s.getWorkPoint() != null && s.getWorkPoint().getId().equals(inv.getWorkPoint().getId()))
                            .map(s -> new InventoryDocumentsGenerator.ScaleNote(s.getName(), s.getSerialNumber(),
                                    scaleState(scaleService.verdictAt(s, inv.getStartsOn()))))
                            .toList());
            default -> throw new BadRequestException(EXPORT_FORMAT_UNSUPPORTED);
        };
    }

    @Transactional(readOnly = true)
    public byte[] openingNote(UUID id) {
        StockOpening opening = openings.requireForDocuments(id);
        return generator.openingNote(company(), opening);
    }

    private Company company() {
        return companyRepository.findById(TenantContext.require()).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    /** Pct. 8 lit. f) din Norme: comisia verifică dacă instrumentele de măsură sunt verificate metrologic. */
    private static String scaleState(ScaleLegality.Verdict v) {
        return switch (v.state()) {
            case VALID -> "verificat metrologic" + (v.validUntil() == null ? "" : ", valabil până la "
                    + v.validUntil().format(InventoryService.DATE));
            case NO_VERIFICATION -> "fără verificare metrologică";
            case EXPIRED -> "verificarea metrologică expirată";
            case REJECTED -> "respins la verificarea metrologică";
            case REPAIRED, INCIDENT -> "de reverificat (reparație sau incident)";
            case NOT_DECLARED -> "nedeclarat la BRML";
            case SEALED -> "sigilat";
            case OUT_OF_USE -> "scos din uz";
        };
    }
}
