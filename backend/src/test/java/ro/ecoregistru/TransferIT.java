package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.request.TransferReceiptRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.MovementDirection;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.MovementQueryService;
import ro.ecoregistru.service.ScaleService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D2.5 — transferul între depozitele firmei: la plecare iese din A și e „în tranzit”, la recepție intră în B cu
 * greutatea lui B; diferența peste toleranța celor două cântare cere NIR și decizia comisiei; destinația e alt
 * depozit, autorizat; pe firmă transferul nu umflă nici intrările, nici ieșirile.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class TransferIT {

    @Autowired WeighingOperationService operations;
    @Autowired ScaleService scales;
    @Autowired MovementQueryService movementQuery;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired ro.ecoregistru.service.WeighingDocumentService documents;
    @Autowired ro.ecoregistru.service.Art48RegisterService art48;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint depotA;
    WorkPoint depotB;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Transfer " + suffix() + " SRL").cui("ROT" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().stream().filter(c -> !c.isHazardous()).findFirst().orElseThrow())
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void aTransferLeavesAAtDispatchIsInTransitAndEntersBAtReception() {
        UUID scaleA = legalScale(depotA);
        UUID scaleB = legalScale(depotB);
        UUID id = transfer(scaleA, "1000");
        assertThat(lines(depotA, MovementDirection.OUT)).as("în lucru nu contează nicăieri").isEmpty();

        WeighingOperationResponse dispatched = operations.finalizeOperation(id);
        assertThat(dispatched.status()).isEqualTo(WeighingOperationStatus.IN_TRANSIT);
        assertThat(dispatched.transfer().targetWorkPointId()).isEqualTo(depotB.getId());
        assertThat(dispatched.transfer().dispatchedAt()).isNotNull();
        assertThat(lines(depotA, MovementDirection.OUT)).extracting(WasteMovementResponse::operation)
                .containsExactly(WasteOperation.TRANSFERRED_OUT);
        assertThat(lines(depotB, MovementDirection.IN)).as("în tranzit nu e în stocul lui B").isEmpty();

        WeighingOperationResponse received = operations.receive(id, receipt(dispatched, scaleB, "998", null, null));
        assertThat(received.status()).isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(received.transfer().sentKg()).isEqualByComparingTo("1000");
        assertThat(received.transfer().receivedKg()).isEqualByComparingTo("998");
        assertThat(received.transfer().differenceKg()).isEqualByComparingTo("-2");
        // Clasa III, e = 20 kg, o singură cântărire pe fiecare parte: 2 × 0,5 e = 20 kg pe cântar.
        assertThat(received.transfer().toleranceKg()).isEqualByComparingTo("40");
        assertThat(received.transfer().nirNumber()).isNull();
        assertThat(received.lines()).hasSize(1);
        assertThat(received.transfer().receivedLines()).singleElement()
                .satisfies(l -> assertThat(l.finalKg()).isEqualByComparingTo("998"));
        assertThat(lines(depotB, MovementDirection.IN)).extracting(WasteMovementResponse::quantity)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class).containsExactly(new BigDecimal("998"));

        // Pe firmă transferul se anulează cu el însuși: nici intrare, nici ieșire.
        assertThat(lines(null, null)).isEmpty();
        assertThat(movementQuery.summary(today.getYear(), today.getMonthValue()).movements()).isZero();
    }

    @Test
    void aDifferenceOverTheToleranceNeedsTheNirAndTheCommissionDecisionEitherWay() {
        UUID scaleA = legalScale(depotA);
        UUID scaleB = legalScale(depotB);
        WeighingOperationResponse short1 = operations.finalizeOperation(transfer(scaleA, "1000"));
        assertThatThrownBy(() -> operations.receive(short1.id(), receipt(short1, scaleB, "900", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.TRANSFER_DIFFERENCE_NEEDS_NIR));
        assertThatThrownBy(() -> operations.receive(short1.id(), receipt(short1, scaleB, "900", "NIR 12", " ")))
                .isInstanceOf(BusinessException.class);
        WeighingOperationResponse closed = operations.receive(short1.id(),
                receipt(short1, scaleB, "900", "NIR 12", "umiditate pierdută pe drum, constatată de comisie"));
        assertThat(closed.transfer().nirNumber()).isEqualTo("NIR 12");
        assertThat(closed.transfer().differenceKg()).isEqualByComparingTo("-100");

        WeighingOperationResponse over = operations.finalizeOperation(transfer(scaleA, "1000"));
        assertThatThrownBy(() -> operations.receive(over.id(), receipt(over, scaleB, "1100", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.TRANSFER_DIFFERENCE_NEEDS_NIR));

        // Fără cântar la recepție toleranța e necunoscută: orice diferență se explică, zero trece.
        WeighingOperationResponse blind = operations.finalizeOperation(transfer(scaleA, "1000"));
        assertThatThrownBy(() -> operations.receive(blind.id(), receipt(blind, null, "999", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.TRANSFER_DIFFERENCE_NEEDS_NIR));
        assertThat(operations.receive(blind.id(), receipt(blind, null, "1000", null, null)).transfer().toleranceKg())
                .isNull();
    }

    @Test
    void theTargetIsAnotherAuthorizedDepotAndTheTransferHasNoCounterpartyOrRdCode() {
        assertBusiness(() -> operations.create(head(depotA, depotA)), ErrorMessageEnum.TRANSFER_SAME_DEPOT);
        assertBusiness(() -> operations.create(head(depotA, null)), ErrorMessageEnum.TRANSFER_TARGET_REQUIRED);
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        assertBusiness(() -> operations.create(new WeighingOperationRequest(WeighingOperationType.TRANSFER,
                depotA.getId(), today, partner.getId(), null, null, null, null, null, null, null, null, null, null,
                null, null, depotB.getId())), ErrorMessageEnum.TRANSFER_NO_COUNTERPARTY);

        UUID id = operations.create(head(depotA, depotB)).id();
        assertBusiness(() -> operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(cardboard.getId(), null, null, new BigDecimal("10"), null, null,
                        WasteOperationCode.R3, null)))), ErrorMessageEnum.OPERATION_CODE_NOT_ALLOWED);

        // Fără autorizație: nici a depozitului, nici a firmei. Apoi a depozitului, expirată. Apoi valabilă.
        company.setEnvironmentalAuthNumber(null);
        companyRepository.save(company);
        assertBusiness(() -> operations.create(head(depotA, depotB)), ErrorMessageEnum.TRANSFER_TARGET_NOT_AUTHORIZED);
        depotB.setEnvironmentalAuthNumber("AM-TURDA");
        depotB.setEnvironmentalAuthExpiry(today.minusDays(1));
        workPointRepository.save(depotB);
        assertBusiness(() -> operations.create(head(depotA, depotB)), ErrorMessageEnum.TRANSFER_TARGET_NOT_AUTHORIZED);
        depotB.setEnvironmentalAuthExpiry(today.plusYears(1));
        workPointRepository.save(depotB);
        assertThat(operations.create(head(depotA, depotB)).transfer().targetWorkPointName()).isEqualTo(depotB.getName());
    }

    @Test
    void theReceptionHasItsOwnRules() {
        UUID scaleA = legalScale(depotA);
        UUID id = transfer(scaleA, "500");
        assertBusiness(() -> operations.receive(id, new TransferReceiptRequest(today, null, null, null, List.of(),
                null, null, null)), ErrorMessageEnum.TRANSFER_NOT_IN_TRANSIT);
        WeighingOperationResponse dispatched = operations.finalizeOperation(id);
        UUID line = dispatched.lines().get(0).id();

        assertBusiness(() -> operations.receive(id, new TransferReceiptRequest(today, null, null, null, List.of(),
                null, null, null)), ErrorMessageEnum.TRANSFER_RECEIPT_LINES_MISMATCH);
        assertBusiness(() -> operations.receive(id, new TransferReceiptRequest(today, null, null, null, List.of(
                new TransferReceiptRequest.Line(line, null, null, new BigDecimal("500"), null),
                new TransferReceiptRequest.Line(line, null, null, new BigDecimal("500"), null)),
                null, null, null)), ErrorMessageEnum.TRANSFER_RECEIPT_LINES_MISMATCH);
        // Un rând în plus, cu o linie care nu e a transferului.
        assertBusiness(() -> operations.receive(id, new TransferReceiptRequest(today, null, null, null, List.of(
                new TransferReceiptRequest.Line(line, null, null, new BigDecimal("500"), null),
                new TransferReceiptRequest.Line(UUID.randomUUID(), null, null, new BigDecimal("5"), null)),
                null, null, null)), ErrorMessageEnum.TRANSFER_RECEIPT_LINES_MISMATCH);
        assertBusiness(() -> operations.receive(id, new TransferReceiptRequest(today.minusDays(1), null, null, null,
                List.of(new TransferReceiptRequest.Line(line, null, null, new BigDecimal("500"), null)),
                null, null, null)), ErrorMessageEnum.TRANSFER_RECEIVED_BEFORE_DEPARTURE);
        // Cântarul lui A nu cântărește la B.
        assertBusiness(() -> operations.receive(id, receipt(dispatched, scaleA, "500", null, null)),
                ErrorMessageEnum.SCALE_OTHER_DEPOT);
        // Nu se editează în tranzit.
        assertBusiness(() -> operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(cardboard.getId(), null, null, new BigDecimal("10"), null, null,
                        null, null)))), ErrorMessageEnum.WEIGHING_OPERATION_NOT_EDITABLE);

        // Operatorul din B vede transferul care vine, dar nu-l recepționează: recepția e a celui care aprobă.
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotB.getId())));
        actAs(operator);
        assertThat(operations.list()).extracting(WeighingOperationResponse::id).contains(id);
        assertThatThrownBy(() -> operations.receive(id, receipt(dispatched, null, "500", null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void perDepotTheOpeningMovesStockAndOnTheCompanyATransferCancelsOut() {
        UUID scaleA = legalScale(depotA);
        UUID scaleB = legalScale(depotB);
        WeighingOperationResponse dispatched = operations.finalizeOperation(transfer(scaleA, "1000"));
        operations.receive(dispatched.id(), receipt(dispatched, scaleB, "990", null, null));
        LocalDate next = LocalDate.of(today.getYear() + 1, 1, 1);

        assertThat(movementRepository.art48OpeningBefore(company.getId(), null, next)).isEmpty();
        assertThat(movementRepository.art48OpeningBefore(company.getId(), depotA.getId(), next)).singleElement()
                .satisfies(o -> assertThat(o.getKg()).isEqualByComparingTo("-1000"));
        assertThat(movementRepository.art48OpeningBefore(company.getId(), depotB.getId(), next)).singleElement()
                .satisfies(o -> assertThat(o.getKg()).isEqualByComparingTo("990"));

        // Anulat în tranzit: marfa n-a plecat, deci A nu mai are ieșirea.
        WeighingOperationResponse other = operations.finalizeOperation(transfer(scaleA, "300"));
        operations.cancel(other.id(), "camionul s-a întors");
        assertThat(lines(depotA, MovementDirection.OUT)).hasSize(1);
    }

    @Test
    void theTransferTravelsWithAnAvizWithoutInvoiceAndAnAnexa3WithTheSendersQuantity() throws Exception {
        depotB.setAddress("Str. Depozitului 2, Turda");
        workPointRepository.save(depotB);
        UUID scaleA = legalScale(depotA);
        UUID scaleB = legalScale(depotB);
        WeighingOperationResponse dispatched = operations.finalizeOperation(transfer(scaleA, "1000"));

        String aviz = Golden.flat(Golden.pdfText(documents.renderAviz(dispatched.id())));
        assertThat(aviz).contains(Golden.flat("Fără factură"));
        assertThat(aviz).contains(Golden.flat("Str. Depozitului 2, Turda"));

        operations.receive(dispatched.id(), receipt(dispatched, scaleB, "900", "NIR 7", "pierdere constatată"));
        String anexa3 = Golden.flat(Golden.pdfText(documents.renderAnexa3(dispatched.id())));
        assertThat(anexa3).as("cantitatea expeditorului").contains("1000");
        assertThat(anexa3).as("B își notează greutatea la Observații")
                .contains(Golden.flat("Recepționat la " + depotB.getName())).contains(Golden.flat("900 kg (NIR 7)"));
        assertThat(anexa3).contains(company.getEnvironmentalAuthNumber());
        assertThat(anexa3.split("900", -1)).as("liniile lui B nu intră în tabelul expeditorului, doar la Observații")
                .hasSize(2);
    }

    @Test
    void theArt48RegisterShowsTheTransferPerDepotAndNotOnTheCompany() throws Exception {
        UUID scaleA = legalScale(depotA);
        UUID scaleB = legalScale(depotB);
        WeighingOperationResponse dispatched = operations.finalizeOperation(transfer(scaleA, "1000"));
        operations.receive(dispatched.id(), receipt(dispatched, scaleB, "990", null, null));
        int year = today.getYear();

        var atA = art48.build(year, depotA.getId());
        assertThat(atA.collection()).singleElement().satisfies(c -> {
            assertThat(c.transferredOutKg()).isEqualByComparingTo("1000");
            assertThat(c.collectedKg()).isEqualByComparingTo("0");
            assertThat(c.closingKg()).isEqualByComparingTo("-1000");
        });
        assertThat(atA.entries()).singleElement().satisfies(e -> {
            assertThat(e.operation()).isEqualTo("Transfer trimis la alt depozit");
            assertThat(e.partner()).isEqualTo("Depozitul " + depotB.getName());
        });
        var atB = art48.build(year, depotB.getId());
        assertThat(atB.collection()).singleElement()
                .satisfies(c -> assertThat(c.transferredInKg()).isEqualByComparingTo("990"));
        var onCompany = art48.build(year, null);
        assertThat(onCompany.entries()).isEmpty();
        assertThat(onCompany.collection()).isEmpty();

        String depotSheet = Golden.flat(Golden.pdfText(art48.render(year, depotA.getId(),
                ro.ecoregistru.service.export.ExportFormat.PDF)));
        assertThat(depotSheet).contains(Golden.flat("Trimisă prin transfer intern"));
        String companySheet = Golden.flat(Golden.pdfText(art48.render(year, null, ro.ecoregistru.service.export.ExportFormat.PDF)));
        assertThat(companySheet).doesNotContain(Golden.flat("transfer intern"));

        // Registrul intern al depozitului: plecarea în A și recepția în B, fiecare cu depozitul și celălalt capăt.
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(
                operations.renderRegister(year, today.getMonthValue())))) {
            var sheet = wb.getSheet("Registru");
            List<String> rows = new java.util.ArrayList<>();
            for (int r = 4; r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                rows.add(row.getCell(1).getStringCellValue() + "|" + row.getCell(4).getStringCellValue() + "|"
                        + row.getCell(6).getStringCellValue());
            }
            assertThat(rows).containsExactlyInAnyOrder(
                    "Transfer trimis|" + depotA.getName() + "|" + depotB.getName(),
                    "Transfer primit|" + depotB.getName() + "|" + depotA.getName());
        }
    }

    // ---------------------------------------------------------------------------------------------

    private List<WasteMovementResponse> lines(WorkPoint depot, MovementDirection direction) {
        return movementQuery.list(today.getYear(), null, depot == null ? null : depot.getId(), null, false, false,
                false, null, direction, null, null, 0, 100, null, false).content();
    }

    private UUID transfer(UUID scaleId, String kg) {
        WeighingOperationRequest h = head(depotA, depotB);
        UUID id = operations.create(new WeighingOperationRequest(h.type(), h.workPointId(), h.date(), null, null, null,
                null, null, null, null, null, null, null, null, null, scaleId, h.targetWorkPointId())).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(cardboard.getId(), null, null, new BigDecimal(kg), null, null, null, null))));
        return id;
    }

    private WeighingOperationRequest head(WorkPoint from, WorkPoint to) {
        return new WeighingOperationRequest(WeighingOperationType.TRANSFER, from.getId(), today, null, null, null,
                null, null, null, null, null, null, null, null, null, null, to == null ? null : to.getId());
    }

    private TransferReceiptRequest receipt(WeighingOperationResponse dispatched, UUID scaleId, String kg, String nir,
                                           String reason) {
        return new TransferReceiptRequest(today, scaleId, null, null, List.of(new TransferReceiptRequest.Line(
                dispatched.lines().get(0).id(), null, null, new BigDecimal(kg), null)), nir, reason, null);
    }

    private UUID legalScale(WorkPoint depot) {
        UUID id = scales.create(new ScaleRequest(depot.getId(), "Pod " + suffix(), "SN-" + suffix(), "Pod basculă",
                "III", new BigDecimal("20"), today.minusYears(2), today.minusYears(2), "BRML-" + suffix(), null)).id();
        scales.addEvent(id, new ScaleEventRequest(ScaleEventKind.VERIFICATION, today.minusMonths(1), true, "B-1", null,
                "Laborator", "ing. Pop", null));
        return id;
    }

    private static void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorMessageEnum code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("transfer+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        AppUser fresh = appUserRepository.findById(user.getId()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(fresh, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
