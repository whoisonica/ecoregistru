package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.controller.response.SiatdSummary;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SiatdModule;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.SiatdDeadlines.SiatdDeadline.State;
import ro.ecoregistru.service.SiatdService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.util.SiatdCalendar;

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
import static ro.ecoregistru.enums.WeighingOperationType.IN;
import static ro.ecoregistru.enums.WeighingOperationType.OUT;

/**
 * F6a — recepțiile cu termen SIATD: lista din tab, cifrele benzii și confirmarea. Apare doar ce e pe un modul bifat, pe
 * depozitele pe care le vede omul; confirmă doar cine aprobă, și mai multe deodată (un colector de la PF are zeci de
 * recepții pe zi), dar codul SIATD doar pe una, fiindcă e unic pe tranzacție.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SiatdServiceIT {

    private static final LocalDate ENROLLED = LocalDate.of(2024, 1, 1);

    @Autowired SiatdService siatd;
    @Autowired WeighingOperationService operations;
    @Autowired CompanyUserService users;
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WeighingOperationRepository operationRepository;

    Company company;
    AppUser admin;
    AppUser operator;
    WorkPoint depotA;
    WorkPoint depotB;
    Partner partner;
    WasteArticle cardboard;   // 15 01 01 — ambalaje, 5 zile
    WasteArticle paper;       // 20 01 01 — municipale, 3 zile
    WasteArticle scrap;       // 17 04 05 — fără modul
    LocalDate today;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("SIATD " + suffix() + " SRL").cui("ROS" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).siatdPackagingFrom(ENROLLED).build());
        admin = user(Role.ADMIN);
        operator = user(Role.OPERATOR);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = article("Carton", "15 01 01");
        paper = article("Hârtie", "20 01 01");
        scrap = article("Fier", "17 04 05");
        today = DeadlineService.today();
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void pendingListsOnlyCheckedModules() {
        UUID packaging = reception(depotA, today, cardboard);
        reception(depotA, today, scrap);   // fără modul
        reception(depotA, today, paper);   // municipale, modul nebifat

        List<SiatdReceptionRow> rows = siatd.receptions(State.PENDING);
        assertThat(rows).extracting(SiatdReceptionRow::operationId).containsExactly(packaging);
        SiatdReceptionRow row = rows.get(0);
        assertThat(row.modules()).containsExactly(SiatdModule.PACKAGING);
        assertThat(row.due()).isEqualTo(SiatdCalendar.due(today, 5));
        assertThat(row.partnerName()).isEqualTo("Magazin Alfa SRL");
        assertThat(row.netKg()).isEqualByComparingTo("120");
        assertThat(siatd.summary().pending()).isEqualTo(1);
    }

    @Test
    void summaryCounts() {
        LocalDate dueToday = startDueOn(today);
        LocalDate dueTomorrow = startDueOn(today.plusDays(1));
        if (dueToday != null) {
            reception(depotA, dueToday, cardboard);
        }
        if (dueTomorrow != null) {
            reception(depotA, dueTomorrow, cardboard);
        }
        reception(depotA, today.minusDays(20), cardboard);   // ratată
        reception(depotA, today, cardboard);                  // de confirmat, nu expiră azi/mâine

        SiatdSummary summary = siatd.summary();
        assertThat(summary.anyModule()).isTrue();
        assertThat(summary.dueToday()).isEqualTo(dueToday == null ? 0 : 1);
        assertThat(summary.dueTomorrow()).isEqualTo(dueTomorrow == null ? 0 : 1);
        assertThat(summary.missed()).isEqualTo(1);
        assertThat(summary.pending()).isEqualTo(1 + summary.dueToday() + summary.dueTomorrow());
    }

    @Test
    void confirmThreeAtOnce() {
        List<UUID> ids = List.of(reception(depotA, today, cardboard), reception(depotA, today, cardboard),
                reception(depotB, today.minusDays(20), cardboard));

        List<SiatdReceptionRow> confirmed = siatd.confirm(ids, null);
        assertThat(confirmed).hasSize(3).allSatisfy(r -> {
            assertThat(r.confirmedAt()).isNotNull();
            assertThat(r.code()).isNull();
        });
        assertThat(operationRepository.findAllById(ids))
                .allSatisfy(o -> assertThat(o.getSiatdConfirmedBy()).isEqualTo(admin.getId()));
        assertThat(siatd.receptions(State.PENDING)).isEmpty();
        assertThat(siatd.receptions(State.MISSED)).isEmpty();
        assertThat(siatd.receptions(State.CONFIRMED)).extracting(SiatdReceptionRow::operationId)
                .containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    void codeOnlyForOne() {
        List<UUID> ids = List.of(reception(depotA, today, cardboard), reception(depotA, today, cardboard));
        assertThatThrownBy(() -> siatd.confirm(ids, "ABC-1")).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.SIATD_CODE_SINGLE);
        assertThatThrownBy(() -> siatd.confirm(List.of(ids.get(0)), "X".repeat(61)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.SIATD_CODE_TOO_LONG);

        List<SiatdReceptionRow> one = siatd.confirm(List.of(ids.get(0)), "  ABC-1 ");
        assertThat(one.get(0).code()).isEqualTo("ABC-1");
        assertThat(one.get(0).confirmedByName()).isNotBlank();
        // a doua confirmare nu suprascrie codul
        siatd.confirm(List.of(ids.get(0)), "ALT-2");
        assertThat(operationRepository.findById(ids.get(0)).orElseThrow().getSiatdCode()).isEqualTo("ABC-1");
    }

    @Test
    void operationWithoutDeadlineIsRefused() {
        UUID out = operations.create(new WeighingOperationRequest(OUT, depotA.getId(), today, partner.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null, null)).id();
        assertThatThrownBy(() -> siatd.confirm(List.of(out), null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.SIATD_NO_DEADLINE);
        UUID noModule = reception(depotA, today, scrap);
        assertThatThrownBy(() -> siatd.confirm(List.of(noModule), null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.SIATD_NO_DEADLINE);
        assertThatThrownBy(() -> siatd.confirm(List.of(), null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.SIATD_CONFIRM_SELECTION);
    }

    @Test
    void operatorCannotConfirm() throws Exception {
        UUID id = reception(depotA, today, cardboard);
        actAs(operator);
        assertThatThrownBy(() -> siatd.confirm(List.of(id), null)).isInstanceOf(AccessDeniedException.class);

        // Autentificarea de pe thread ar opri filtrul JWT, iar 403-ul n-ar dovedi nimic (capcana din D1.5).
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        mockMvc.perform(post("/api/v1/depot-siatd/confirmations")
                        .header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationIds\":[\"" + id + "\"]}"))
                .andExpect(status().isForbidden());
        actAs(admin);
        assertThat(operationRepository.findById(id).orElseThrow().getSiatdConfirmedAt()).isNull();
    }

    /** Un operator restrâns la Baciu nu vede recepțiile din Turda, nici în listă, nici pe bandă. */
    @Test
    void otherDepotIsHidden() {
        reception(depotA, today, cardboard);
        reception(depotB, today, cardboard);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotA.getId())));

        actAs(operator);
        assertThat(siatd.receptions(State.PENDING)).extracting(SiatdReceptionRow::workPointId)
                .containsExactly(depotA.getId());
        assertThat(siatd.summary().pending()).isEqualTo(1);
    }

    /** Confirmarea nu e dată de registru: trece și după finalizare, și după ce Anexa 3 are număr. */
    @Test
    void confirmAfterLockWorks() {
        UUID id = reception(depotA, today, cardboard);
        var op = operationRepository.findById(id).orElseThrow();
        op.setAnexa3Series("WH");
        op.setAnexa3Number(7);
        operationRepository.save(op);

        assertThat(siatd.confirm(List.of(id), null)).hasSize(1);
    }

    @Test
    void unconfirmBringsItBack() {
        UUID fresh = reception(depotA, today, cardboard);
        UUID late = reception(depotA, today.minusDays(20), cardboard);
        siatd.confirm(List.of(fresh), "C-1");
        siatd.confirm(List.of(late), null);

        siatd.unconfirm(fresh);
        siatd.unconfirm(late);
        assertThat(siatd.receptions(State.PENDING)).extracting(SiatdReceptionRow::operationId).containsExactly(fresh);
        assertThat(siatd.receptions(State.MISSED)).extracting(SiatdReceptionRow::operationId).containsExactly(late);
        assertThat(operationRepository.findById(fresh).orElseThrow().getSiatdCode()).isNull();

        actAs(operator);
        assertThatThrownBy(() -> siatd.unconfirm(fresh)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void operationResponseCarriesSiatd() {
        UUID id = reception(depotA, today, cardboard);
        var view = operations.get(id).siatd();
        assertThat(view).isNotNull();
        assertThat(view.due()).isEqualTo(SiatdCalendar.due(today, 5));
        assertThat(view.state()).isEqualTo(State.PENDING);
        assertThat(operations.get(reception(depotA, today, scrap)).siatd()).isNull();
    }

    @Test
    void missedStaysUntilConfirmed() {
        UUID id = reception(depotA, today.minusDays(20), cardboard);
        assertThat(siatd.receptions(State.MISSED)).extracting(SiatdReceptionRow::operationId).containsExactly(id);
        siatd.confirm(List.of(id), null);
        assertThat(operations.get(id).siatd().state()).isEqualTo(State.CONFIRMED);
    }

    /** Scheduler-ul citește pe firmă, fără utilizator: vede toate depozitele. */
    @Test
    void pendingForSeesTheWholeCompany() {
        reception(depotA, today, cardboard);
        reception(depotB, today, cardboard);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        assertThat(siatd.pendingFor(company.getId(), today)).hasSize(2);
    }

    /** Kg de pe rând sunt ale liniilor pe modulele bifate: fierul de pe aceeași recepție nu intră în SIATD (recenzia F6a). */
    @Test
    void kgCountOnlyTheSiatdLines() {
        UUID id = operations.create(new WeighingOperationRequest(IN, depotA.getId(), today, partner.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new Line(cardboard.getId(), null, null, new BigDecimal("200"), null, new BigDecimal("0.5"), null, null),
                new Line(scrap.getId(), null, null, new BigDecimal("3000"), null, new BigDecimal("0.5"), null, null))));
        operations.finalizeOperation(id);

        SiatdReceptionRow row = siatd.receptions(State.PENDING).stream()
                .filter(r -> r.operationId().equals(id)).findFirst().orElseThrow();
        assertThat(row.netKg()).isEqualByComparingTo("200");
    }

    /**
     * Interogarea aduce doar codurile modulelor bifate: o firmă doar pe ambalaje nu-și încarcă la fiecare deschidere toate
     * recepțiile de hârtie din capitolul 20 (recenzia F6a — memoria dyno-ului, ca la BUG-017).
     */
    @Test
    void theQueryLoadsOnlyCheckedModules() {
        UUID packaging = reception(depotA, today, cardboard);
        UUID municipal = reception(depotA, today, paper);
        List<UUID> loaded = operationRepository.findSiatdCandidates(company.getId(),
                        false, true, false, false, false).stream()
                .map(ro.ecoregistru.entity.WeighingOperation::getId).toList();
        assertThat(loaded).contains(packaging).doesNotContain(municipal);
    }

    // --- ajutoare ---

    /** O zi de recepție al cărei termen de 5 zile cade exact în {@code due}, sau null dacă nu există (weekend). */
    private static LocalDate startDueOn(LocalDate due) {
        for (int back = 6; back <= 20; back++) {
            LocalDate start = due.minusDays(back);
            if (SiatdCalendar.due(start, 5).equals(due)) {
                return start;
            }
        }
        return null;
    }

    private UUID reception(WorkPoint depot, LocalDate date, WasteArticle article) {
        UUID id = operations.create(new WeighingOperationRequest(IN, depot.getId(), date, partner.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new Line(article.getId(), null, null, new BigDecimal("120"), null, new BigDecimal("0.5"), null, null))));
        operations.finalizeOperation(id);
        return id;
    }

    private WasteArticle article(String name, String code) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode(code).orElseThrow()).name(name)
                .active(true).createdAt(Instant.now()).build());
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x").firstName("Ana")
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
