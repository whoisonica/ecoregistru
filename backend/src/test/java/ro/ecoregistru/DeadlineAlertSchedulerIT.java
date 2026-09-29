package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.service.DeadlineAlertScheduler;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * FAZA TERMENE / T2 (scheduler): reminder dispatch logic, driven deterministically via a fixed
 * "today". NotificationService is mocked so no SMTP is touched — the test asserts on which
 * deadlines get their warned flags flipped (i.e. which reminders were sent).
 */
@SpringBootTest(properties = "app.deadlines.missed-shown-from=2026-01-01")
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DeadlineAlertSchedulerIT {

    @Autowired DeadlineAlertScheduler scheduler;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;

    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ro.ecoregistru.repository.AuditLogRepository auditLogRepository;

    @MockitoBean NotificationService notificationService;

    private Company companyWithUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Alert " + suffix).cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        appUserRepository.save(AppUser.builder()
                .email("alert+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return company;
    }

    private ReportingDeadline deadline(Company c, LocalDate dueDate, DeadlineStatus status,
                                       boolean warned7, boolean warned1) {
        return deadlineRepository.save(ReportingDeadline.builder()
                .company(c).reportType(ReportType.SIM_ANNUAL).dueDate(dueDate).status(status)
                .warned7Days(warned7).warned1Day(warned1).createdAt(Instant.now()).build());
    }

    private ReportingDeadline reload(UUID id) {
        return deadlineRepository.findById(id).orElseThrow();
    }

    @Test
    void sendsEarlyReminderWhenSeveralDaysRemain() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(5), DeadlineStatus.UPCOMING, false, false);

        scheduler.dispatchReminders(today);

        assertThat(reload(d.getId()).isWarned7Days()).isTrue();
        assertThat(reload(d.getId()).isWarned1Day()).isFalse();
    }

    @Test
    void sendsFinalReminderWhenDueTomorrow() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(1), DeadlineStatus.UPCOMING, false, false);

        scheduler.dispatchReminders(today);

        assertThat(reload(d.getId()).isWarned1Day()).isTrue();
    }

    @Test
    void sendsFinalReminderWhenDueToday() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today, DeadlineStatus.UPCOMING, false, false);

        scheduler.dispatchReminders(today);

        assertThat(reload(d.getId()).isWarned1Day()).isTrue();
    }

    @Test
    void doesNotResendAnEarlyReminderAlreadySent() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        // 4 days out, early reminder already sent -> stays as-is, no final reminder yet.
        ReportingDeadline d = deadline(c, today.plusDays(4), DeadlineStatus.UPCOMING, true, false);

        scheduler.dispatchReminders(today);

        assertThat(reload(d.getId()).isWarned1Day()).isFalse();
    }

    @Test
    void ignoresDeadlinesOutsideTheWindowAndDoneOnes() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline far = deadline(c, today.plusDays(20), DeadlineStatus.UPCOMING, false, false);
        ReportingDeadline done = deadline(c, today.plusDays(2), DeadlineStatus.DONE, false, false);

        scheduler.dispatchReminders(today);

        assertThat(reload(far.getId()).isWarned7Days()).isFalse();
        assertThat(reload(done.getId()).isWarned7Days()).isFalse();
        assertThat(reload(done.getId()).isWarned1Day()).isFalse();
    }

    /**
     * 15.09.2026 — prin intrarea programată, nu prin metoda de test: acolo apelul intern ocolea
     * tranzacția și fanionul nu se salva, deci mementoul pleca din nou în fiecare zi.
     */
    @Test
    void theScheduledEntryPointSavesTheFlag() {
        Company c = companyWithUser();
        ReportingDeadline d = deadline(c, LocalDate.now().plusDays(5), DeadlineStatus.UPCOMING, false, false);

        scheduler.runDailyDeadlineReminders();

        assertThat(reload(d.getId()).isWarned7Days()).isTrue();
    }

    @Test
    void doesNotMarkWhenDeliveryFails() {
        Mockito.doThrow(new RuntimeException("smtp down"))
                .when(notificationService).sendDeadlineReminder(any(), any(), anyLong());

        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(5), DeadlineStatus.UPCOMING, false, false);

        scheduler.dispatchReminders(today);

        // Delivery failed -> flag stays false so the reminder is retried tomorrow.
        assertThat(reload(d.getId()).isWarned7Days()).isFalse();
    }

    // ─── Termenul ratat (V61): o singură notificare, a doua zi ───

    @Test
    void aMissedDeadlineIsToldOnceTheDayAfter() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 10);
        ReportingDeadline d = deadline(c, today.minusDays(1), DeadlineStatus.UPCOMING, true, true);

        scheduler.dispatchReminders(today);
        assertThat(reload(d.getId()).isWarnedMissed()).isTrue();

        Mockito.clearInvocations(notificationService);
        scheduler.dispatchReminders(today.plusDays(1));
        Mockito.verify(notificationService, Mockito.never())
                .sendDeadlineReminder(Mockito.argThat(x -> x.getId().equals(d.getId())), any(), anyLong());
    }

    @Test
    void aMissedDeadlineGetsANegativeDayCountSoTheMailSaysItPassed() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 20);
        ReportingDeadline d = deadline(c, today.minusDays(2), DeadlineStatus.UPCOMING, true, true);

        scheduler.dispatchReminders(today);

        Mockito.verify(notificationService)
                .sendDeadlineReminder(Mockito.argThat(x -> x.getId().equals(d.getId())), any(), Mockito.eq(-2L));
    }

    @Test
    void noMissedNoticeForOldNewsTickedOnesOrThoseBeforeTheRule() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 7, 1);
        ReportingDeadline stale = deadline(c, today.minusDays(20), DeadlineStatus.UPCOMING, true, true);
        ReportingDeadline done = deadline(c, today.minusDays(1), DeadlineStatus.DONE, true, true);
        ReportingDeadline beforeRule = deadline(c, LocalDate.of(2025, 12, 31), DeadlineStatus.UPCOMING, true, true);

        scheduler.dispatchReminders(today);
        scheduler.dispatchReminders(LocalDate.of(2026, 1, 2));

        assertThat(reload(stale.getId()).isWarnedMissed()).isFalse();
        assertThat(reload(done.getId()).isWarnedMissed()).isFalse();
        assertThat(reload(beforeRule.getId()).isWarnedMissed()).isFalse();
    }

    /** 28.09.2026 — operatorul de cântar nu lucrează cu termenele generatorului: nu primește mementoul. */
    @Test
    void theScaleOperatorIsNotRemindedOfDeadlines() {
        Company c = companyWithUser();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser scaleOperator = appUserRepository.save(AppUser.builder()
                .email("cantar+" + suffix + "@demo.ro").password("x")
                .role(Role.SCALE_OPERATOR).company(c).enabled(true).createdAt(Instant.now()).build());
        AppUser operator = appUserRepository.save(AppUser.builder()
                .email("operator+" + suffix + "@demo.ro").password("x")
                .role(Role.OPERATOR).company(c).enabled(true).createdAt(Instant.now()).build());
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(5), DeadlineStatus.UPCOMING, false, false);

        scheduler.dispatchReminders(today);

        Mockito.verify(notificationService).sendDeadlineReminder(Mockito.argThat(x -> x.getId().equals(d.getId())),
                Mockito.argThat(to -> to.contains(operator.getEmail()) && !to.contains(scaleOperator.getEmail())),
                anyLong());
    }

    /**
     * Bifa „Depus” pusă cât timp mementoul trimite mailuri nu se pierde (29.09.2026).
     *
     * <p>Mementoul ţine termenul încărcat pe toată trecerea şi scrie steagul abia după mail. Mailul
     * e simulat aici de mock: în clipa trimiterii, o tranzacţie separată — omul din ecran — marchează
     * termenul depus şi face commit. Cu UPDATE pe tot rândul, flush-ul mementoului rescria apoi
     * `status = UPCOMING` peste bifă, iar termenul reapărea ca neîndeplinit.
     */
    @Test
    void aDeadlineTickedWhileTheMailGoesOutStaysDone() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(5), DeadlineStatus.UPCOMING, false, false);
        TransactionTemplate user = new TransactionTemplate(transactionManager);
        user.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Mockito.doAnswer(inv -> {
            user.executeWithoutResult(s -> {
                ReportingDeadline mine = deadlineRepository.findById(d.getId()).orElseThrow();
                mine.setStatus(DeadlineStatus.DONE);
                mine.setCompletedAt(Instant.now());
                mine.setCompletionNote("depus pe SIM");
            });
            return null;
        }).when(notificationService).sendDeadlineReminder(
                Mockito.argThat(x -> x != null && x.getId().equals(d.getId())), any(), anyLong());

        scheduler.dispatchReminders(today);

        ReportingDeadline after = reload(d.getId());
        assertThat(after.isWarned7Days()).as("garda: mementoul chiar a scris steagul").isTrue();
        assertThat(after.getStatus()).isEqualTo(DeadlineStatus.DONE);
        assertThat(after.getCompletionNote()).isEqualTo("depus pe SIM");
    }

    /**
     * Steagurile mementoului nu ajung în „Istoric” (29.09.2026), nici când pe fir sunt un om şi o firmă — bifa
     * „Depus” se scrie pe faţă, din DeadlineService, iar termenul nu e pe lista albă a interceptorului.
     */
    @Test
    void theReminderFlagsLeaveNoJournalRow() {
        Company c = companyWithUser();
        LocalDate today = LocalDate.of(2026, 6, 1);
        ReportingDeadline d = deadline(c, today.plusDays(5), DeadlineStatus.UPCOMING, false, false);
        AppUser someone = appUserRepository.findAllByCompany_IdAndEnabledTrue(c.getId()).get(0);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        someone, null, java.util.List.of()));
        // Şi cu firma aleasă: cazul cel mai rău, în care jurnalul ar avea şi om, şi firmă de scris.
        ro.ecoregistru.security.TenantContext.set(c.getId());
        try {
            scheduler.dispatchReminders(today);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            ro.ecoregistru.security.TenantContext.clear();
        }

        assertThat(reload(d.getId()).isWarned7Days()).as("garda: mementoul chiar a scris steagul").isTrue();
        assertThat(auditLogRepository.findAll()).noneMatch(r -> d.getId().equals(r.getEntityId()));
    }
}
