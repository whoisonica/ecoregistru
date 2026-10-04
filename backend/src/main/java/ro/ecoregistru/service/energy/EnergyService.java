package ro.ecoregistru.service.energy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.EnergyCarriersRequest;
import ro.ecoregistru.controller.request.EnergyConsumptionRequest;
import ro.ecoregistru.controller.request.EnergyContactRequest;
import ro.ecoregistru.controller.request.EnergyDeclarationRequest;
import ro.ecoregistru.controller.request.EnergyMeasureRequest;
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.EnergyCarrier;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.energy.EnergyYear.Cell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/** Reads and writes the energy sheet (Anexa 1) of the current tenant; the figures come from {@link EnergyYear}. */
@Service
@RequiredArgsConstructor
public class EnergyService {

    private static final int FIRST_YEAR = 2020;

    private final CompanyRepository companyRepository;
    private final EnergyCarrierUsedRepository carrierRepository;
    private final EnergyConsumptionRepository consumptionRepository;
    private final EnergyDeclarationRepository declarationRepository;
    private final EnergySavingMeasureRepository measureRepository;

    @Transactional(readOnly = true)
    public EnergySheetResponse sheet(int year) {
        checkYear(year);
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        return build(company, year);
    }

    @Transactional
    public EnergySheetResponse saveCarriers(EnergyCarriersRequest request) {
        Company company = company();
        Set<EnergyCarrier> wanted = EnumSet.noneOf(EnergyCarrier.class);
        wanted.addAll(request.carriers());
        Map<EnergyCarrier, EnergyCarrierUsed> existing = new EnumMap<>(EnergyCarrier.class);
        carrierRepository.findAllByCompany_Id(company.getId()).forEach(c -> existing.put(c.getCarrier(), c));

        // Unticking removes only this row: the months stay, so ticking again brings them back.
        existing.forEach((carrier, row) -> {
            if (!wanted.contains(carrier)) {
                carrierRepository.delete(row);
            }
        });
        for (EnergyCarrier carrier : wanted) {
            if (!existing.containsKey(carrier)) {
                carrierRepository.save(EnergyCarrierUsed.builder()
                        .company(company).carrier(carrier).updatedAt(Instant.now()).build());
            }
        }
        carrierRepository.flush();
        return build(company, LocalDate.now(DeadlineService.ZONE).getYear());
    }

    @Transactional
    public EnergySheetResponse saveCell(EnergyConsumptionRequest request) {
        checkYear(request.year());
        Company company = company();
        EnergyCarrier carrier = request.carrier();
        boolean ticked = carrierRepository.findAllByCompany_Id(company.getId()).stream()
                .anyMatch(c -> c.getCarrier() == carrier);
        if (!ticked) {
            throw new BadRequestException(ENERGY_CARRIER_NOT_USED);
        }
        if (request.tep() != null && carrier.coefficient().isPresent()) {
            throw new BadRequestException(ENERGY_TEP_NOT_ALLOWED);
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
        Set<EnergyCarrier> used = EnumSet.noneOf(EnergyCarrier.class);
        carrierRepository.findAllByCompany_Id(companyId).forEach(c -> used.add(c.getCarrier()));

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

        EnergySheetResponse.Contact contact = new EnergySheetResponse.Contact(
                company.getFax(), company.getWebsite(), company.getActivitySector(),
                company.getEnergyContactName(), company.getEnergyContactEmail(),
                company.getEnergyContactPhone(), company.getEnergyContactMobile(),
                company.getEnergyContactAttestedOn());

        return new EnergySheetResponse(year,
                Arrays.stream(EnergyCarrier.values()).filter(used::contains).toList(),
                shown, energyYear.totals(), energyYear.totalTep(), energyYear.monthsComplete(),
                energyYear.overThreshold(), declaration, contact, null);
    }
}
