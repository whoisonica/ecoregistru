package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.ConsultantDigestScheduler;
import ro.ecoregistru.service.DeadlineAlertScheduler;
import ro.ecoregistru.service.PartnerAuthorizationAlertScheduler;
import ro.ecoregistru.service.notification.NotificationService;
import ro.ecoregistru.service.notification.PushNotifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * G2 — cele trei alerte de dimineață ajung și pe telefon, <b>după</b> mail și numai dacă mailul a plecat.
 * Ordinea contează: fanioanele schedulerelor sunt ale mailului; un push trimis înaintea unui mail căzut
 * s-ar repeta mâine, și poimâine, cât timp SMTP-ul nu merge.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PushAlertsIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Autowired DeadlineAlertScheduler deadlineScheduler;
    @Autowired PartnerAuthorizationAlertScheduler partnerScheduler;
    @Autowired ConsultantDigestScheduler digestScheduler;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired PartnerRepository partnerRepository;

    @MockBean NotificationService notificationService;
    @MockBean PushNotifier pushNotifier;

    @Test
    void aDeadlineReminderReachesThePhonesOfTheCompanyAndOpensDeadlines() {
        Company company = company(null);
        AppUser admin = user(company, null, Role.ADMIN);
        deadline(company, TODAY.plusDays(5));

        deadlineScheduler.dispatchReminders(TODAY);

        verify(pushNotifier).send(argThat(containsOnly(admin)),
                argThat(m -> m.screen().equals("termene") && m.title().equals(company.getName() + ": termen scadent în 5 zile")));
    }

    @Test
    void noMailNoPush() {
        Company company = company(null);
        AppUser admin = user(company, null, Role.ADMIN);
        ReportingDeadline d = deadline(company, TODAY.plusDays(5));
        Mockito.doThrow(new RuntimeException("smtp down")).when(notificationService)
                .sendDeadlineReminder(argThat(x -> x.getId().equals(d.getId())), any(), anyLong());

        deadlineScheduler.dispatchReminders(TODAY);

        verify(pushNotifier, never()).send(argThat(containsOnly(admin)), any());
    }

    @Test
    void anExpiringAuthorizationOpensTheControlScreen() {
        Company company = company(null);
        AppUser admin = user(company, null, Role.ADMIN);
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Colector " + suffix()).type(PartnerType.COLLECTOR)
                .client(true).supplier(false).carrier(false).authorizationNumber("AM 1")
                .authorizationExpiry(TODAY.plusDays(30)).active(true).createdAt(Instant.now()).build());

        partnerScheduler.dispatchWarnings(TODAY);

        verify(pushNotifier).send(argThat(containsOnly(admin)), argThat(m -> m.screen().equals("control")
                && m.title().equals("Autorizația partenerului " + partner.getName() + " expiră în 30 de zile")));
    }

    @Test
    void theConsultantDigestIsOneNotificationForTheConsultants() {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(digitsCui()).createdAt(Instant.now()).build());
        Company company = company(cabinet);
        AppUser consultant = user(null, cabinet, Role.CONSULTANT);
        deadline(company, TODAY.plusDays(1));
        deadline(company, TODAY.plusDays(3));

        digestScheduler.dispatch(TODAY);

        verify(pushNotifier).send(argThat(containsOnly(consultant)),
                argThat(m -> m.title().equals(cabinet.getName() + ": 2 termene în următoarele 7 zile")));
    }

    // ── ajutoare ─────────────────────────────────────────────────────────────

    private static org.mockito.ArgumentMatcher<Collection<AppUser>> containsOnly(AppUser user) {
        return users -> users.size() == 1 && users.iterator().next().getId().equals(user.getId());
    }

    private Company company(Consultancy consultancy) {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix()).cui(digitsCui()).type(CompanyType.GENERATOR).consultancy(consultancy)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
    }

    private AppUser user(Company company, Consultancy consultancy, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("push+" + suffix() + "@proba.ro").password("x").role(role)
                .company(company).consultancy(consultancy).enabled(true).createdAt(Instant.now()).build());
    }

    private ReportingDeadline deadline(Company company, LocalDate due) {
        return deadlineRepository.save(ReportingDeadline.builder()
                .company(company).reportType(ReportType.SIM_ANNUAL).dueDate(due).status(DeadlineStatus.UPCOMING)
                .warned7Days(false).warned1Day(false).createdAt(Instant.now()).build());
    }

    private static String digitsCui() {
        return "RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
