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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.ScaleStatus;
import ro.ecoregistru.enums.WeighingOperationStatus;
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
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.ScaleExpiryAlertScheduler;
import ro.ecoregistru.service.ScaleLegality.State;
import ro.ecoregistru.service.ScaleService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.ScaleEventKind.INCIDENT;
import static ro.ecoregistru.enums.ScaleEventKind.REPAIR;
import static ro.ecoregistru.enums.ScaleEventKind.VERIFICATION;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D2.3 — cântarul depozitului: fișa, istoricul de verificări cu „valabil până la” scris doar în jos,
 * starea la zi, cântarul pe operațiune (sigilat/scos din uz = refuz; nelegal = confirmare cu motiv la
 * finalizare), izolarea între firme, alerta de 30 de zile și drepturile pe HTTP.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ScaleIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ScaleService service;
    @Autowired ScaleExpiryAlertScheduler scheduler;
    @Autowired WeighingOperationService operations;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean NotificationService notificationService;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint depot;
    Partner partner;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = company("Cântar");
        admin = user(company, Role.ADMIN);
        depot = depot(company);
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().get(0))
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(company, admin);
    }

    @AfterEach
    void tearDown() {
        clearThread();
    }

    @Test
    void theNameIsRequiredAndUniqueInTheDepotAndThePlateIsChecked() {
        ScaleResponse bridge = service.create(scale("Podul basculă", today.minusYears(2), today.minusYears(2)));
        assertThat(bridge.workPointName()).isEqualTo(depot.getName());
        assertThat(bridge.status()).isEqualTo(ScaleStatus.IN_USE);

        assertThatThrownBy(() -> service.create(scale(" Podul basculă ", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_NAME_TAKEN));
        assertThatThrownBy(() -> service.create(scale("  ", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_NAME_REQUIRED));
        assertThatThrownBy(() -> service.create(new ScaleRequest(depot.getId(), "Platforma", null, null,
                "V", null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_CLASS_UNKNOWN));
        assertThatThrownBy(() -> service.create(new ScaleRequest(depot.getId(), "Platforma", null, null,
                "III", BigDecimal.ZERO, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_DIVISION_NOT_POSITIVE));

        WorkPoint other = depot(company);
        assertThat(service.create(new ScaleRequest(other.getId(), "Podul basculă", null, null, "III",
                new BigDecimal("20"), null, null, null, null)).accuracyClass())
                .as("același nume în alt depozit e alt cântar").isEqualTo("III");
    }

    /** „Valabil până la” = data + 12 luni dacă nu e scris; mai scurt se poate, mai lung nu (L.O.-2022). */
    @Test
    void theValidityIsAYearAtMostAndOnlyAnAdmittedBulletinGivesIt() {
        ScaleResponse s = service.create(scale("Pod", today.minusYears(3), today.minusYears(3)));
        LocalDate verified = today.minusMonths(2);

        ScaleResponse after = service.addEvent(s.id(), verification(verified, true, null));
        assertThat(after.state()).isEqualTo(State.VALID);
        assertThat(after.validUntil()).isEqualTo(verified.plusMonths(12));

        assertThatThrownBy(() -> service.addEvent(s.id(), verification(verified, true, verified.plusMonths(12).plusDays(1))))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_VALID_UNTIL_TOO_LATE));
        assertThatThrownBy(() -> service.addEvent(s.id(), verification(verified, true, verified.minusDays(1))))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_VALID_UNTIL_BEFORE_DATE));
        assertThatThrownBy(() -> service.addEvent(s.id(), new ScaleEventRequest(VERIFICATION, verified, true,
                " ", null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_BULLETIN_REQUIRED));
        assertThatThrownBy(() -> service.addEvent(s.id(), new ScaleEventRequest(VERIFICATION, verified, null,
                "B-1", null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_RESULT_REQUIRED));

        // Mai scurt decât anul: se ia cum scrie pe buletin.
        ScaleResponse shorter = service.addEvent(s.id(), verification(today.minusMonths(1), true, today.plusDays(10)));
        assertThat(shorter.validUntil()).isEqualTo(today.plusDays(10));

        // RESPINS: valabilitatea nu se păstrează, orice ar scrie pe cerere.
        ScaleResponse rejected = service.addEvent(s.id(), verification(today, false, today.plusMonths(6)));
        assertThat(rejected.state()).isEqualTo(State.REJECTED);
        assertThat(rejected.events()).filteredOn(e -> Boolean.FALSE.equals(e.admitted()))
                .singleElement().satisfies(e -> assertThat(e.validUntil()).isNull());
    }

    /** Plasa din bază (V68), pentru un rând scris pe lângă serviciu. */
    @Test
    void theDatabaseRefusesAValidityOverAYearAndAReasonWithoutAnIllegalState() {
        ScaleResponse s = legalScale("Plasă");
        UUID event = s.events().get(0).id();
        assertThatThrownBy(() -> jdbc.update(
                "update scale_events set valid_until = event_date + interval '13 months' where id = ?", event))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "update scale_events set valid_until = null where id = ?", event))
                .as("ADMIS fără valabilitate").isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        UUID id = weighed(s.id());
        assertThatThrownBy(() -> jdbc.update(
                "update weighing_operations set scale_state = 'VALID', scale_override_reason = 'x' where id = ?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.update(
                "update weighing_operations set scale_state = 'EXPIRED', scale_override_reason = 'x' where id = ?", id))
                .as("control pozitiv").isEqualTo(1);
    }

    @Test
    void aRepairAfterTheBulletinCancelsTheValidityUntilTheNextVerification() {
        ScaleResponse s = service.create(scale("Pod", today.minusYears(3), today.minusYears(3)));
        service.addEvent(s.id(), verification(today.minusMonths(3), true, null));
        ScaleResponse repaired = service.addEvent(s.id(),
                new ScaleEventRequest(REPAIR, today.minusDays(1), null, null, null, null, null, "celula 3 schimbată"));
        assertThat(repaired.state()).isEqualTo(State.REPAIRED);
        assertThat(repaired.events()).extracting(ScaleResponse.Event::kind)
                .as("istoricul, cel mai nou primul").containsExactly(REPAIR, VERIFICATION);

        ScaleResponse reverified = service.addEvent(s.id(), verification(today, true, null));
        assertThat(reverified.state()).isEqualTo(State.VALID);
        assertThat(reverified.validUntil()).isEqualTo(today.plusMonths(12));

        // Un rând greșit se șterge; starea se recalculează din ce rămâne.
        UUID last = reverified.events().get(0).id();
        assertThat(service.deleteEvent(s.id(), last).state()).isEqualTo(State.REPAIRED);
    }

    @Test
    void anotherFirmsScaleAndDepotAreNotFound() {
        ScaleResponse mine = service.create(scale("Pod", null, null));

        Company other = company("Alta");
        AppUser otherAdmin = user(other, Role.ADMIN);
        actAs(other, otherAdmin);
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.addEvent(mine.id(), verification(today, true, null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.create(scale("Pod", null, null)))
                .as("depozitul altei firme").isInstanceOf(NotFoundException.class);
    }

    @Test
    void aSealedOrRetiredScaleCannotWeighAndAScaleOfAnotherDepotNeither() {
        ScaleResponse sealed = service.create(new ScaleRequest(depot.getId(), "Sigilat", null, null, null, null,
                today.minusYears(2), today.minusYears(2), null, ScaleStatus.SEALED));
        assertThatThrownBy(() -> operations.create(head(sealed.id())))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_NOT_USABLE));

        ScaleResponse usable = legalScale("Bun");
        UUID id = weighed(usable.id());
        service.update(usable.id(), new ScaleRequest(depot.getId(), "Bun", null, null, null, null,
                today.minusYears(2), today.minusYears(2), null, ScaleStatus.OUT_OF_USE));
        assertThatThrownBy(() -> operations.finalizeOperation(id, "am cântărit oricum"))
                .as("scos din uz între cântărire și finalizare: nici motivul nu-l salvează")
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_NOT_USABLE));

        WorkPoint otherDepot = depot(company);
        ScaleResponse elsewhere = service.create(new ScaleRequest(otherDepot.getId(), "Pod", null, null, null, null,
                today.minusMonths(1), today.minusMonths(1), null, null));
        assertThatThrownBy(() -> operations.create(head(elsewhere.id())))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_OTHER_DEPOT));
    }

    /** Decizia proprietarului, 26.09.2026: cântar expirat sau nedeclarat → confirmare explicită cu motiv. */
    @Test
    void anIllegalScaleIsFinalizedOnlyWithAReasonKeptOnTheOperation() {
        ScaleResponse expired = service.create(scale("Expirat", today.minusYears(3), today.minusYears(3)));
        service.addEvent(expired.id(), verification(today.minusMonths(13), true, null));
        UUID id = weighed(expired.id());
        assertThat(operations.get(id).scaleState()).as("operatorul vede starea înainte de finalizare")
                .isEqualTo(State.EXPIRED);

        assertThatThrownBy(() -> operations.finalizeOperation(id, "  "))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_REASON_REQUIRED));
        assertThat(operations.get(id).status()).isEqualTo(WeighingOperationStatus.IN_PROGRESS);

        WeighingOperationResponse done = operations.finalizeOperation(id, "verificarea programată pe 30.09");
        assertThat(done.status()).isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(done.scaleState()).isEqualTo(State.EXPIRED);
        assertThat(done.scaleOverrideReason()).isEqualTo("verificarea programată pe 30.09");

        // Starea rămâne cea de la finalizare, chiar dacă istoricul se completează după.
        service.addEvent(expired.id(), verification(today.minusMonths(1), true, null));
        assertThat(operations.get(id).scaleState()).isEqualTo(State.EXPIRED);

        ScaleResponse undeclared = service.create(scale("Nedeclarat", today.minusMonths(2), null));
        UUID second = weighed(undeclared.id());
        assertThatThrownBy(() -> operations.finalizeOperation(second, null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_REASON_REQUIRED));
    }

    @Test
    void aLegalScaleOrNoScaleFinalizesWithoutAReasonAndAReasonIsNotKeptThen() {
        UUID withScale = weighed(legalScale("Legal").id());
        WeighingOperationResponse done = operations.finalizeOperation(withScale, "nu trebuia");
        assertThat(done.scaleState()).isEqualTo(State.VALID);
        assertThat(done.scaleOverrideReason()).isNull();

        UUID without = weighed(null);
        WeighingOperationResponse plain = operations.finalizeOperation(without, null);
        assertThat(plain.status()).isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(plain.scaleState()).isNull();
    }

    @Test
    void aScaleWithWeighingsIsRetiredNotDeleted() {
        ScaleResponse used = legalScale("Folosit");
        weighed(used.id());
        assertThatThrownBy(() -> service.delete(used.id()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_HAS_WEIGHINGS));

        ScaleResponse unused = service.create(scale("Nefolosit", null, null));
        service.addEvent(unused.id(), verification(today, true, null));
        service.delete(unused.id());
        assertThat(service.list()).extracting(ScaleResponse::name).containsExactly("Folosit");
    }

    @Test
    void theExpiryWarningGoesOutOncePerDateWithinThirtyDays() {
        ScaleResponse soon = service.create(scale("Curând", today.minusYears(2), today.minusYears(2)));
        service.addEvent(soon.id(), verification(today.minusMonths(6), true, today.plusDays(20)));
        ScaleResponse later = service.create(scale("Târziu", today.minusYears(2), today.minusYears(2)));
        service.addEvent(later.id(), verification(today.minusMonths(6), true, null));
        ScaleResponse retired = service.create(new ScaleRequest(depot.getId(), "Scos", null, null, null, null,
                today.minusYears(2), today.minusYears(2), null, ScaleStatus.OUT_OF_USE));
        service.addEvent(retired.id(), verification(today.minusMonths(6), true, today.plusDays(5)));
        clearThread();

        scheduler.dispatchWarnings(today);
        verify(notificationService).sendScaleExpiryWarning(argThat(s -> s.getId().equals(soon.id())),
                anyList(), eq(20L));
        verify(notificationService, never()).sendScaleExpiryWarning(argThat(s -> !s.getId().equals(soon.id())),
                anyList(), anyLong());

        clearInvocations(notificationService);
        scheduler.dispatchWarnings(today.plusDays(1));
        verify(notificationService, never()).sendScaleExpiryWarning(any(), anyList(), anyLong());

        // O verificare nouă mută scadența; alerta se rearmează pentru noua dată.
        actAs(company, admin);
        service.addEvent(soon.id(), verification(today.minusMonths(5), true, today.plusDays(25)));
        clearThread();
        scheduler.dispatchWarnings(today);
        verify(notificationService).sendScaleExpiryWarning(argThat(s -> s.getId().equals(soon.id())),
                anyList(), eq(25L));
    }

    @Test
    void theViewerReadsTheScalesButOnlyWritersAddToThemOverHttp() throws Exception {
        ScaleResponse s = service.create(scale("Pod", null, null));
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        clearThread();

        mockMvc.perform(get("/api/v1/scales").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scales/" + s.id() + "/events").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"INCIDENT\",\"date\":\"" + today + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/scales/" + s.id() + "/events").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"INCIDENT\",\"date\":\"" + today + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/scales").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workPointId\":\"" + depot.getId() + "\",\"name\":\"Nou\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------

    private ScaleResponse legalScale(String name) {
        ScaleResponse s = service.create(scale(name, today.minusYears(2), today.minusYears(2)));
        return service.addEvent(s.id(), verification(today.minusMonths(1), true, null));
    }

    private UUID weighed(UUID scaleId) {
        UUID id = operations.create(head(scaleId)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(cardboard.getId(), null, null, new BigDecimal("120"), null, null,
                        null, null))));
        return id;
    }

    private WeighingOperationRequest head(UUID scaleId) {
        return new WeighingOperationRequest(IN, depot.getId(), today, partner.getId(),
                null, null, null, null, null, null, null, null, null, null, null, scaleId);
    }

    private ScaleRequest scale(String name, LocalDate commissioned, LocalDate declared) {
        return new ScaleRequest(depot.getId(), name, "SN-" + suffix(), "Pod basculă 60 t", "III",
                new BigDecimal("20"), commissioned, declared, declared == null ? null : "BRML-" + suffix(), null);
    }

    private static ScaleEventRequest verification(LocalDate date, boolean admitted, LocalDate validUntil) {
        return new ScaleEventRequest(VERIFICATION, date, admitted, "B-" + date, validUntil,
                "Laborator Metrologie Cluj", "ing. Pop", null);
    }

    private static void clearThread() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private Company company(String name) {
        return companyRepository.save(Company.builder()
                .name(name + " " + suffix() + " SRL").cui("ROS" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private WorkPoint depot(Company owner) {
        return workPointRepository.save(WorkPoint.builder()
                .company(owner).name("Depozit " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company owner, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("cantar+" + suffix() + "@demo.ro").password("x")
                .role(role).company(owner).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(Company owner, AppUser user) {
        TenantContext.set(owner.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
