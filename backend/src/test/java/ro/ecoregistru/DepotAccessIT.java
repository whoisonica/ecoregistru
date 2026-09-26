package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.controller.response.WorkPointResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.MovementQueryService;
import ro.ecoregistru.service.ScaleService;
import ro.ecoregistru.service.WasteMovementService;
import ro.ecoregistru.service.WeighingDocumentService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.WorkPointService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D2.4 — accesul pe depozit: un operator restrâns la Baciu nu vede și nu scrie nimic din Turda (operațiuni,
 * documentele lor, liniile ajunse în registru, cântare, lista de depozite). Implicit — și pentru toți
 * utilizatorii de dinainte — fiecare vede toate depozitele (decizia proprietarului, 16.09.2026); adminul
 * vede mereu tot. Restricția o pune doar cine administrează utilizatorii.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DepotAccessIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired WeighingOperationService operations;
    @Autowired WeighingDocumentService documents;
    @Autowired ScaleService scales;
    @Autowired WorkPointService workPoints;
    @Autowired MovementQueryService movementQuery;
    @Autowired WasteMovementService movements;
    @Autowired ro.ecoregistru.service.MovementDocumentService movementDocuments;
    @Autowired ro.ecoregistru.service.MovementAttachmentService attachments;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    LocalDate today;
    Company company;
    AppUser admin;
    AppUser operator;
    WorkPoint baciu;
    WorkPoint turda;
    Partner partner;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Acces " + suffix() + " SRL").cui("ROA" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        operator = user(Role.OPERATOR);
        baciu = depot("Baciu");
        turda = depot("Turda");
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().get(0))
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void everyoneSeesEveryDepotUntilTheAdminRestrictsThem() {
        UUID inBaciu = weighed(baciu);
        UUID inTurda = weighed(turda);

        actAs(operator);
        assertThat(operations.list()).extracting(WeighingOperationResponse::id).contains(inBaciu, inTurda);
        assertThat(workPoints.list()).extracting(WorkPointResponse::id).contains(baciu.getId(), turda.getId());

        actAs(admin);
        CompanyUserResponse row = users.changeWorkPoints(operator.getId(),
                new UserWorkPointsRequest(false, List.of(baciu.getId())));
        assertThat(row.allWorkPoints()).isFalse();
        assertThat(row.workPointIds()).containsExactly(baciu.getId());
        assertThat(users.list()).filteredOn(u -> u.id().equals(operator.getId()))
                .singleElement().satisfies(u -> assertThat(u.workPointIds()).containsExactly(baciu.getId()));

        actAs(operator);
        assertThat(operations.list()).extracting(WeighingOperationResponse::id).containsExactly(inBaciu);
        assertThat(workPoints.list()).extracting(WorkPointResponse::id).containsExactly(baciu.getId());

        // Și înapoi: „Toate depozitele” le include și pe cele deschise după.
        actAs(admin);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(true, List.of()));
        WorkPoint cluj = depot("Cluj");
        actAs(operator);
        assertThat(workPoints.list()).extracting(WorkPointResponse::id)
                .contains(baciu.getId(), turda.getId(), cluj.getId());
    }

    @Test
    void aRestrictedOperatorCannotReadOrWriteAnOperationOfAnotherDepot() {
        UUID inBaciu = weighed(baciu);
        UUID inTurda = weighed(turda);
        restrictOperatorToBaciu();

        actAs(operator);
        assertThat(operations.get(inBaciu).workPointId()).isEqualTo(baciu.getId());
        assertNotFound(() -> operations.get(inTurda), ErrorMessageEnum.WEIGHING_OPERATION_NOT_FOUND);
        assertNotFound(() -> operations.update(inTurda, head(turda)), ErrorMessageEnum.WEIGHING_OPERATION_NOT_FOUND);
        assertNotFound(() -> operations.replaceLines(inTurda, lines()), ErrorMessageEnum.WEIGHING_OPERATION_NOT_FOUND);
        assertNotFound(() -> documents.cashCheck(inTurda), ErrorMessageEnum.WEIGHING_OPERATION_NOT_FOUND);
        // Nici nu pornește una acolo, nici nu mută una de-a lui acolo.
        assertNotFound(() -> operations.create(head(turda)), ErrorMessageEnum.WORK_POINT_NOT_FOUND);
        assertNotFound(() -> operations.update(inBaciu, head(turda)), ErrorMessageEnum.WORK_POINT_NOT_FOUND);
        assertThat(operations.create(head(baciu)).workPointId()).isEqualTo(baciu.getId());
    }

    @Test
    void theRegisterLinesOfAnotherDepotAreHiddenToo() {
        UUID inBaciu = weighed(baciu);
        UUID inTurda = weighed(turda);
        operations.finalizeOperation(inBaciu);
        operations.finalizeOperation(inTurda);
        UUID turdaLine = operations.get(inTurda).lines().get(0).id();
        UUID baciuLine = operations.get(inBaciu).lines().get(0).id();
        restrictOperatorToBaciu();

        actAs(operator);
        List<UUID> listed = movementQuery.list(today.getYear(), null, null, null, false, false, false,
                        null, null, null, null, 0, 100, null, false)
                .content().stream().map(WasteMovementResponse::id).toList();
        assertThat(listed).contains(baciuLine).doesNotContain(turdaLine);
        // Filtrul cerut pe Turda nu ocolește restricția.
        assertThat(movementQuery.list(today.getYear(), null, turda.getId(), null, false, false, false,
                null, null, null, null, 0, 100, null, false).content()).isEmpty();
        assertThat(movementQuery.totals(today.getYear(), null, turda.getId(), null, false, false, false,
                null, null, null).rows()).isZero();
        assertThat(movements.get(baciuLine).id()).isEqualTo(baciuLine);
        assertNotFound(() -> movements.get(turdaLine), ErrorMessageEnum.MOVEMENT_NOT_FOUND);
        // Documentele și fișierele liniei: altfel ar răspunde cu regula lor (linia e a unei operațiuni).
        assertNotFound(() -> movementDocuments.renderAviz(turdaLine), ErrorMessageEnum.MOVEMENT_NOT_FOUND);
        assertNotFound(() -> attachments.attachmentContent(turdaLine, UUID.randomUUID()),
                ErrorMessageEnum.MOVEMENT_NOT_FOUND);
    }

    @Test
    void scalesOfAnotherDepotAreNeitherListedNorWritable() {
        ScaleResponse baciuScale = scales.create(scale(baciu));
        ScaleResponse turdaScale = scales.create(scale(turda));
        restrictOperatorToBaciu();

        actAs(operator);
        assertThat(scales.list()).extracting(ScaleResponse::id).containsExactly(baciuScale.id());
        assertNotFound(() -> scales.addEvent(turdaScale.id(), verification()), ErrorMessageEnum.SCALE_NOT_FOUND);
        assertNotFound(() -> scales.create(scale(turda)), ErrorMessageEnum.WORK_POINT_NOT_FOUND);
        assertNotFound(() -> scales.update(baciuScale.id(), scale(turda)), ErrorMessageEnum.WORK_POINT_NOT_FOUND);
    }

    @Test
    void theAdminAlwaysSeesEverythingAndOnlyOperatorsAndViewersAreRestricted() {
        UUID inTurda = weighed(turda);
        AppUser secondAdmin = user(Role.ADMIN);

        assertThatThrownBy(() -> users.changeWorkPoints(secondAdmin.getId(),
                new UserWorkPointsRequest(false, List.of(baciu.getId()))))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.USER_WORK_POINTS_ROLE));
        assertThatThrownBy(() -> users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of())))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.USER_WORK_POINTS_REQUIRED));

        // Un depozit al altei firme nu se poate da.
        Company other = companyRepository.save(Company.builder()
                .name("Alta " + suffix() + " SRL").cui("ROB" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        WorkPoint foreign = workPointRepository.save(WorkPoint.builder()
                .company(other).name("Străin").active(true).createdAt(Instant.now()).build());
        assertNotFound(() -> users.changeWorkPoints(operator.getId(),
                new UserWorkPointsRequest(false, List.of(foreign.getId()))), ErrorMessageEnum.WORK_POINT_NOT_FOUND);

        // O restricție rămasă pe cineva ajuns admin nu-l mai restrânge.
        restrictOperatorToBaciu();
        AppUser promoted = appUserRepository.findById(operator.getId()).orElseThrow();
        assertThat(promoted.isAllWorkPoints()).isFalse();
        promoted.setRole(Role.ADMIN);
        appUserRepository.save(promoted);
        actAs(operator);
        assertThat(operations.get(inTurda).id()).isEqualTo(inTurda);
    }

    @Test
    void onlyWhoManagesUsersSetsTheDepotsOverHttp() throws Exception {
        // Capcana MockMvc: o autentificare lăsată pe thread de actAs face filtrul JWT să sară, iar 403-ul iese fals.
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        String body = "{\"allWorkPoints\":false,\"workPointIds\":[\"" + baciu.getId() + "\"]}";
        mockMvc.perform(put("/api/v1/users/" + admin.getId() + "/work-points")
                        .header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/users/" + operator.getId() + "/work-points")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------------------------------------

    private void restrictOperatorToBaciu() {
        actAs(admin);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(baciu.getId())));
        actAs(admin);
    }

    private UUID weighed(WorkPoint depot) {
        UUID id = operations.create(head(depot)).id();
        operations.replaceLines(id, lines());
        return id;
    }

    private WeighingLinesRequest lines() {
        return new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(cardboard.getId(), null, null, new BigDecimal("120"), null, null,
                        null, null)));
    }

    private WeighingOperationRequest head(WorkPoint depot) {
        return new WeighingOperationRequest(IN, depot.getId(), today, partner.getId(),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private ScaleRequest scale(WorkPoint depot) {
        return new ScaleRequest(depot.getId(), "Pod " + suffix(), "SN-" + suffix(), "Pod basculă 60 t", "III",
                new BigDecimal("20"), today.minusYears(2), today.minusYears(2), "BRML-" + suffix(), null);
    }

    private ScaleEventRequest verification() {
        return new ScaleEventRequest(ScaleEventKind.VERIFICATION, today.minusMonths(1), true, "B-1", null,
                "Laborator", "ing. Pop", null);
    }

    private static void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                                       ErrorMessageEnum code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getError()).isEqualTo(code));
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("acces+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    /** Utilizatorul se recitește: restricția e în bază, nu pe obiectul din test. */
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
