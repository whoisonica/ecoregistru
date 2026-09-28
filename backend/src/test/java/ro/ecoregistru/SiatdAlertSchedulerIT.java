package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.SiatdAlertScheduler;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * F6a — mementoul SIATD de dimineață: un singur mail pe firmă, cu recepțiile al căror memento sau termen e azi, doar către
 * cei care pot confirma. Ce e deja ratat nu se mai trimite (e în tab, la „Ratate”); o firmă care cade nu le oprește pe
 * celelalte.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SiatdAlertSchedulerIT {

    /** Vineri 04.09.2026, 5 zile la ambalaje: memento miercuri 09.09, termen joi 10.09. */
    private static final LocalDate RECEPTION = LocalDate.of(2026, 9, 4);
    private static final LocalDate REMINDER = LocalDate.of(2026, 9, 9);
    private static final LocalDate DUE = LocalDate.of(2026, 9, 10);

    @Autowired SiatdAlertScheduler scheduler;
    @Autowired WeighingOperationService operations;
    @MockitoSpyBean CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired ConsultancyRepository consultancyRepository;

    @MockitoBean NotificationService notificationService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsOnReminderDayAndOnDueDay() {
        Fixture f = company(true);
        UUID id = f.reception(RECEPTION);

        scheduler.dispatch(REMINDER);
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())),
                argThat(rows -> rows.stream().map(SiatdReceptionRow::operationId).toList().equals(List.of(id))),
                anyList());
        clearInvocations(notificationService);

        scheduler.dispatch(DUE);
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())), anyList(),
                anyList());
        clearInvocations(notificationService);

        scheduler.dispatch(DUE.plusDays(1));   // ratată: nu mai pleacă
        verify(notificationService, never()).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())),
                anyList(), anyList());
        scheduler.dispatch(REMINDER.minusDays(1));   // încă nimic de spus
        verify(notificationService, never()).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())),
                anyList(), anyList());
    }

    @Test
    void nothingWithoutModules() {
        Fixture f = company(false);
        f.reception(RECEPTION);
        scheduler.dispatch(REMINDER);
        verify(notificationService, never()).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())),
                anyList(), anyList());
    }

    @Test
    void onlyApproversReceive() {
        Fixture f = company(true);
        f.reception(RECEPTION);
        scheduler.dispatch(REMINDER);
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())), anyList(),
                argThat(to -> to.contains(f.admin.getEmail()) && !to.contains(f.operator.getEmail())));
    }

    /**
     * Consultantul n-are firmă (V40: {@code company_id} gol), ci cabinet: la un client fără administrator activ, fără el
     * mailul n-ar pleca la nimeni. Recenzia finală F6a, 28.09.2026.
     */
    @Test
    void consultantsOfTheCabinetReceive() {
        Fixture f = company(true);
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet SIATD").cui("RO" + TestCui.random()).createdAt(Instant.now()).build());
        f.company.setConsultancy(cabinet);
        companyRepository.save(f.company);
        AppUser consultant = appUserRepository.save(AppUser.builder()
                .email("consultant+" + UUID.randomUUID().toString().substring(0, 8) + "@demo.ro").password("x")
                .role(Role.CONSULTANT).consultancy(cabinet).enabled(true).createdAt(Instant.now()).build());
        f.admin.setEnabled(false);
        appUserRepository.save(f.admin);
        f.reception(RECEPTION);

        scheduler.dispatch(REMINDER);
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(f.company.getId())), anyList(),
                argThat(to -> to.contains(consultant.getEmail()) && !to.contains(f.admin.getEmail())));
    }

    @Test
    void oneCompanyFailingDoesNotStopTheOther() {
        Fixture failing = company(true);
        Fixture fine = company(true);
        failing.reception(RECEPTION);
        fine.reception(RECEPTION);
        doThrow(new RuntimeException("SMTP căzut")).when(notificationService)
                .sendSiatdReminder(argThat(c -> c != null && c.getId().equals(failing.company.getId())), any(), any());

        scheduler.dispatch(REMINDER);
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(fine.company.getId())), anyList(),
                anyList());
    }

    /**
     * O firmă a cărei citire cade (o interogare expirată, o dată stricată) nu strică tranzacția celorlalte: fără tranzacție
     * comună în scheduler, fiecare firmă se citește în a ei. Recenzia finală F6a, 28.09.2026.
     */
    @Test
    void aFailingReadDoesNotPoisonTheOthers() {
        Fixture broken = company(true);
        Fixture fine = company(true);
        broken.reception(RECEPTION);
        fine.reception(RECEPTION);
        doThrow(new IllegalStateException("citire căzută")).when(companyRepository)
                .findById(broken.company.getId());

        assertThatCode(() -> scheduler.dispatch(REMINDER)).doesNotThrowAnyException();
        verify(notificationService).sendSiatdReminder(argThat(c -> c.getId().equals(fine.company.getId())), anyList(),
                anyList());
    }

    // --- ajutoare ---

    private Fixture company(boolean enrolled) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Memento " + suffix + " SRL").cui("ROM" + suffix).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now())
                .siatdPackagingFrom(enrolled ? LocalDate.of(2024, 1, 1) : null).build());
        AppUser admin = user(company, Role.ADMIN, suffix);
        AppUser operator = user(company, Role.OPERATOR, suffix);
        WorkPoint depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Baciu " + suffix).active(true).createdAt(Instant.now()).build());
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix + " SRL").cui("RO" + suffix)
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        WasteArticle cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode("15 01 01").orElseThrow()).name("Carton")
                .active(true).createdAt(Instant.now()).build());
        return new Fixture(company, admin, operator, depot, partner, cardboard);
    }

    private AppUser user(Company company, Role role, String suffix) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private final class Fixture {
        final Company company;
        final AppUser admin;
        final AppUser operator;
        final WorkPoint depot;
        final Partner partner;
        final WasteArticle cardboard;

        Fixture(Company company, AppUser admin, AppUser operator, WorkPoint depot, Partner partner,
                WasteArticle cardboard) {
            this.company = company;
            this.admin = admin;
            this.operator = operator;
            this.depot = depot;
            this.partner = partner;
            this.cardboard = cardboard;
        }

        UUID reception(LocalDate date) {
            return receptionOf(this, date);
        }
    }

    private UUID receptionOf(Fixture f, LocalDate date) {
        TenantContext.set(f.company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(f.admin, null, List.of()));
        UUID id = operations.create(new WeighingOperationRequest(IN, f.depot.getId(), date, f.partner.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new Line(f.cardboard.getId(), null, null, new BigDecimal("80"), null, new BigDecimal("0.4"), null, null))));
        operations.finalizeOperation(id);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        return id;
    }
}
