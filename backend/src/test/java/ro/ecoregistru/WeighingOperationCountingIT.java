package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.Art48RegisterService;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.Art48Register;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * D1.3 — doar operațiunile finalizate contează. O mișcare fără operațiune contează ca până acum;
 * o linie de operațiune în lucru sau anulată nu intră în nimic din ce adună sau tipărește.
 *
 * <p>Fiecare interogare de cititor are testul ei, ca proba negativă (filtrul scos dintr-una) să
 * pice exact testul acelei interogări. Fixture-ul are patru rânduri pe cantități care nu se pot
 * confunda la adunare: 100 fără operațiune, 200 finalizat, 400 în lucru, 800 anulat. Controlul
 * pozitiv e fetch-ul simplu, care le vede pe toate patru.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WeighingOperationCountingIT {

    private static final LocalDate MARCH = LocalDate.of(2026, 3, 10);

    @Autowired WasteMovementRepository movementRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired Art48RegisterService art48RegisterService;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired PlatformTransactionManager transactionManager;

    Company company;
    UUID tenantId;
    UUID userId;
    WorkPoint depot;
    Partner supplier;
    List<WasteCode> codes;
    int nextNumber = 1;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Depozit Numărat " + suffix + " SRL").cui("RON" + suffix)
                // BOTH: are și registrul art. 48, și Anexa 1, deci ambii cititori au ce tipări.
                .type(CompanyType.BOTH)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        userId = appUserRepository.save(AppUser.builder()
                .email("numarat+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build()).getId();
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        supplier = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix)
                .type(PartnerType.GENERATOR).active(true).createdAt(Instant.now()).build());
        codes = wasteCodeRepository.findAll().subList(0, 4);
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /** Patru rânduri, fiecare pe codul lui, ca setul de coduri să arate cine a scăpat. */
    private void fixture() {
        collected(null, codes.get(0), "100");
        collected(operation(WeighingOperationStatus.FINALIZED), codes.get(1), "200");
        collected(operation(WeighingOperationStatus.IN_PROGRESS), codes.get(2), "400");
        collected(operation(WeighingOperationStatus.CANCELLED), codes.get(3), "800");
    }

    @Test
    void thePlainFetchSeesAllFourRows() {
        fixture();
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId)).hasSize(4);
    }

    @Test
    void findCountedBetweenSkipsOperationsInProgressOrCancelled() {
        fixture();
        assertThat(kg(movementRepository.findCountedBetween(tenantId, MARCH, MARCH)))
                .containsExactlyInAnyOrder(100, 200);
    }

    @Test
    void findCountedSkipsOperationsInProgressOrCancelled() {
        fixture();
        assertThat(kg(movementRepository.findCounted(tenantId))).containsExactlyInAnyOrder(100, 200);
    }

    @Test
    void theDistinctCodesAreOnlyTheCountedOnes() {
        fixture();
        assertThat(movementRepository.findDistinctWasteCodes(tenantId, MARCH, MARCH))
                .containsExactlyInAnyOrder(codes.get(0).getCode(), codes.get(1).getCode());
    }

    @Test
    void theMonthSummaryCountsOnlyTheCountedRows() {
        fixture();
        var totals = movementRepository.summarise(tenantId, MARCH, MARCH);
        assertThat(totals.getMovements()).isEqualTo(2);
        assertThat(totals.getQuantityKg()).isEqualByComparingTo("300");
    }

    /** Cap-coadă, pe documentul pe care îl citește inspectorul. */
    @Test
    void theArt48RegisterPrintsOnlyTheCountedRows() {
        fixture();
        Art48Register register = new TransactionTemplate(transactionManager)
                .execute(status -> art48RegisterService.build(2026, null));
        assertThat(register.entries()).extracting(e -> e.kg().intValue())
                .containsExactlyInAnyOrder(100, 200);
    }

    /**
     * Finalizarea atinge doar capul operațiunii, nu linia. Dacă cache-ul de evidență s-ar uita doar
     * la linii, foaia ar rămâne la 100 după ce operațiunea de 400 s-a finalizat.
     */
    @Test
    void finalizingAfterTheLastRebuildReachesTheSheet() {
        generated(null, "100");
        WeighingOperation op = operation(WeighingOperationStatus.IN_PROGRESS);
        generated(op, "400");
        evidenceCalculator.regenerateYear(2026);
        assertThat(marchGenerated()).isEqualByComparingTo("100");

        op.setStatus(WeighingOperationStatus.FINALIZED);
        op.setFinalizedAt(Instant.now());
        op.setFinalizedBy(userId);
        operationRepository.saveAndFlush(op);

        assertThat(marchGenerated()).isEqualByComparingTo("500");
    }

    @Test
    void cancellingAfterTheLastRebuildLeavesTheSheet() {
        generated(null, "100");
        WeighingOperation op = operation(WeighingOperationStatus.FINALIZED);
        generated(op, "400");
        evidenceCalculator.regenerateYear(2026);
        assertThat(marchGenerated()).isEqualByComparingTo("500");

        op.setStatus(WeighingOperationStatus.CANCELLED);
        op.setCancelledAt(Instant.now());
        op.setCancelledBy(userId);
        op.setCancelReason("Cântărire greșită");
        operationRepository.saveAndFlush(op);

        assertThat(marchGenerated()).isEqualByComparingTo("100");
    }

    // --- helpers ---

    private WeighingOperation operation(WeighingOperationStatus status) {
        WeighingOperation.WeighingOperationBuilder op = WeighingOperation.builder()
                .company(company).workPoint(depot).type(WeighingOperationType.IN)
                .number(nextNumber++).date(MARCH).partner(supplier)
                .status(status).createdBy(userId);
        if (status == WeighingOperationStatus.FINALIZED) {
            op.finalizedAt(Instant.now()).finalizedBy(userId);
        }
        if (status == WeighingOperationStatus.CANCELLED) {
            op.cancelledAt(Instant.now()).cancelledBy(userId).cancelReason("Dublură");
        }
        return operationRepository.saveAndFlush(op.build());
    }

    private void collected(WeighingOperation op, WasteCode code, String kg) {
        movementRepository.saveAndFlush(line(op, code, kg)
                .operation(WasteOperation.COLLECTED).register(WasteRegister.ART_48)
                .partner(supplier).build());
    }

    private void generated(WeighingOperation op, String kg) {
        movementRepository.saveAndFlush(line(op, codes.get(0), kg)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1).build());
    }

    private WasteMovement.WasteMovementBuilder line(WeighingOperation op, WasteCode code, String kg) {
        return WasteMovement.builder()
                .company(company).workPoint(depot).date(MARCH).wasteCode(code)
                .quantity(new BigDecimal(kg)).netKg(op == null ? null : new BigDecimal(kg))
                .unit(Unit.KG).weighingOperation(op)
                .deleted(false).createdBy(userId);
    }

    private BigDecimal marchGenerated() {
        List<Anexa1Sheet> sheets = evidenceCalculator.anexa1(2026, depot.getId());
        assertThat(sheets).hasSize(1);
        return sheets.get(0).rows().get(2).generated();
    }

    private static List<Integer> kg(List<WasteMovement> movements) {
        return movements.stream().map(m -> m.getQuantity().intValue()).toList();
    }
}
