package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.ReceivedFormRequest;
import ro.ecoregistru.controller.request.TransferReceiptRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.ReceivedFormResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.ReceivedFormService;
import ro.ecoregistru.service.WeighingDocumentService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D2.6 — registrul formularelor primite: pe depozit, numerotat fără goluri, append-only (baza refuză UPDATE), corectat
 * doar cu un rând nou; la recepția unui transfer își trece singur Anexa 3 a expeditorului; PDF cu seria și „Pagina X
 * din Y”; depozitul altcuiva e „negăsit”.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ReceivedFormIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ReceivedFormService service;
    @Autowired WeighingOperationService operations;
    @Autowired WeighingDocumentService documents;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired JdbcTemplate jdbc;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint baciu;
    WorkPoint turda;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Registru " + suffix() + " SRL").cui("ROR" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        baciu = depot("Baciu");
        turda = depot("Turda");
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void eachDepotNumbersItsOwnRegisterWithoutGaps() {
        assertThat(service.record(form(baciu, "101")).entryNo()).isEqualTo(1);
        assertThat(service.record(form(baciu, "102")).entryNo()).isEqualTo(2);
        assertThat(service.record(form(turda, "7")).entryNo()).isEqualTo(1);
        assertThat(service.list(baciu.getId(), today.getYear())).extracting(ReceivedFormResponse::formNumber)
                .containsExactly("101", "102");
    }

    @Test
    void theEntryNeedsDateNumberAndSender() {
        assertBusiness(() -> service.record(new ReceivedFormRequest(baciu.getId(), null, null, null, "1", null, "X",
                null, null, null, null, null)), ErrorMessageEnum.RECEIVED_FORM_DATE_REQUIRED);
        assertBusiness(() -> service.record(new ReceivedFormRequest(baciu.getId(), today, null, null, " ", null, "X",
                null, null, null, null, null)), ErrorMessageEnum.RECEIVED_FORM_NUMBER_REQUIRED);
        assertBusiness(() -> service.record(new ReceivedFormRequest(baciu.getId(), today, null, null, "1", null, " ",
                null, null, null, null, null)), ErrorMessageEnum.RECEIVED_FORM_SENDER_REQUIRED);
        assertBusiness(() -> service.record(new ReceivedFormRequest(baciu.getId(), today, "ANEXA_9", null, "1", null,
                "X", null, null, null, null, null)), ErrorMessageEnum.RECEIVED_FORM_KIND_UNKNOWN);
        assertBusiness(() -> service.record(new ReceivedFormRequest(baciu.getId(), today, null, null, "1", null, "X",
                null, null, BigDecimal.ZERO, null, null)), ErrorMessageEnum.RECEIVED_FORM_QUANTITY_NOT_POSITIVE);
        assertBusiness(() -> service.record(new ReceivedFormRequest(null, today, null, null, "1", null, "X",
                null, null, null, null, null)), ErrorMessageEnum.RECEIVED_FORM_DEPOT_REQUIRED);
    }

    @Test
    void aMistakeIsCorrectedWithANewRowAndTheDatabaseRefusesToRewriteOne() {
        ReceivedFormResponse wrong = service.record(form(baciu, "101"));
        assertBusiness(() -> service.correct(wrong.id(), form(baciu, "110")), ErrorMessageEnum.RECEIVED_FORM_REASON_REQUIRED);
        ReceivedFormResponse fixed = service.correct(wrong.id(), withReason(form(turda, "110"), "cifre inversate"));
        assertThat(fixed.entryNo()).isEqualTo(2);
        assertThat(fixed.workPointId()).as("corectura stă în registrul rândului îndreptat").isEqualTo(baciu.getId());
        assertThat(fixed.correctsEntryNo()).isEqualTo(1);
        assertThat(service.list(baciu.getId(), today.getYear())).first()
                .satisfies(r -> {
                    assertThat(r.formNumber()).as("rândul greșit rămâne").isEqualTo("101");
                    assertThat(r.correctedBy()).isEqualTo(2);
                });
        assertBusiness(() -> service.correct(wrong.id(), withReason(form(baciu, "111"), "iar")),
                ErrorMessageEnum.RECEIVED_FORM_ALREADY_CORRECTED);

        assertThatThrownBy(() -> jdbc.update("update received_forms set form_number = '999' where id = ?", wrong.id()))
                .hasMessageContaining("append-only");
    }

    @Test
    void aTransferReceptionWritesTheSendersAnexa3IntoTheTargetsRegister() throws Exception {
        WasteArticle cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().stream().filter(c -> !c.isHazardous())
                        .findFirst().orElseThrow())
                .name("Carton").active(true).createdAt(Instant.now()).build());
        UUID printed = dispatched(cardboard);
        documents.renderAnexa3(printed);
        receive(printed, "990");
        UUID unprinted = dispatched(cardboard);
        receive(unprinted, "500");

        assertThat(service.list(turda.getId(), today.getYear())).singleElement().satisfies(r -> {
            assertThat(r.weighingOperationId()).isEqualTo(printed);
            assertThat(r.formNumber()).isNotBlank();
            assertThat(r.senderName()).contains(company.getName()).contains(baciu.getName());
            assertThat(r.quantityKg()).as("cantitatea de pe formular, a expeditorului").isEqualByComparingTo("500");
        });
        assertThat(service.list(baciu.getId(), today.getYear())).isEmpty();
    }

    @Test
    void theRegisterPrintsItsSeriesAndPageNumbers() throws Exception {
        baciu.setReceivedFormsSeries("RFP-BAC");
        workPointRepository.save(baciu);
        service.record(form(baciu, "101"));
        String text = Golden.flat(Golden.pdfText(service.render(baciu.getId(), today.getYear())));
        assertThat(text).contains("RFP-BAC").contains(Golden.flat("Pagina 1 din 1")).contains("101");
    }

    @Test
    void anotherDepotsRegisterIsNotFoundAndAViewerDoesNotWrite() throws Exception {
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(baciu.getId())));
        ReceivedFormResponse inTurda = service.record(form(turda, "7"));
        actAs(operator);
        assertThatThrownBy(() -> service.list(turda.getId(), today.getYear()))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessageEnum.WORK_POINT_NOT_FOUND));
        assertThatThrownBy(() -> service.correct(inTurda.id(), withReason(form(turda, "8"), "x")))
                .isInstanceOf(NotFoundException.class);
        assertThat(service.record(form(baciu, "1")).entryNo()).isEqualTo(1);

        TenantContext.clear();
        SecurityContextHolder.clearContext();
        AppUser viewer = user(Role.CLIENT_VIEWER);
        mockMvc.perform(post("/api/v1/received-forms").header("Authorization", "Bearer " + jwtService.generateToken(viewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workPointId\":\"" + baciu.getId() + "\",\"receivedOn\":\"" + today
                                + "\",\"formNumber\":\"1\",\"senderName\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------

    private UUID dispatched(WasteArticle article) {
        UUID id = operations.create(new WeighingOperationRequest(WeighingOperationType.TRANSFER, baciu.getId(), today,
                null, null, null, null, null, null, null, null, null, null, null, null, null, turda.getId())).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                article.getId(), null, null, new BigDecimal("500"), null, null, null, null))));
        return operations.finalizeOperation(id).id();
    }

    private void receive(UUID id, String kg) {
        WeighingOperationResponse op = operations.get(id);
        operations.receive(id, new TransferReceiptRequest(today, null, null, null, List.of(new TransferReceiptRequest.Line(
                op.lines().get(0).id(), null, null, new BigDecimal(kg), null)), "NIR 1", "diferență de probă", null));
    }

    private ReceivedFormRequest form(WorkPoint depot, String number) {
        return new ReceivedFormRequest(depot.getId(), today, "ANEXA_3", "CJ", number, today.minusDays(1),
                "Magazin Alfa SRL", "RO123", "15 01 01 ambalaje de hârtie și carton", new BigDecimal("1200"), null, null);
    }

    private static ReceivedFormRequest withReason(ReceivedFormRequest r, String reason) {
        return new ReceivedFormRequest(r.workPointId(), r.receivedOn(), r.formKind(), r.formSeries(), r.formNumber(),
                r.formDate(), r.senderName(), r.senderCui(), r.wasteDescription(), r.quantityKg(),
                r.weighingOperationId(), reason);
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
                .email("registru+" + suffix() + "@demo.ro").password("x")
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
