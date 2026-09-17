package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.ConsultantDigestScheduler;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * P2.13, felia 2 — rezumatul zilnic al consultanților (decizia proprietarului, 15.09.2026: un mail pe zi,
 * cumulat). Ziua e fixă și mailul e dublat, deci testul se uită la <em>ce</em> ar pleca și <em>cui</em>.
 *
 * <p>Baza e comună cu celelalte clase, iar alte teste lasă cabinete în urmă, deci fiecare verificare
 * se face pe numele cabinetului ei, nu pe numărul total de mailuri.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ConsultantDigestIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Autowired ConsultantDigestScheduler scheduler;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;

    @MockitoBean NotificationService notificationService;

    @SuppressWarnings("unchecked")
    @Test
    void oneMailPerCabinetWithTheOpenDeadlinesOfTheNextSevenDaysToItsActiveConsultants() {
        Consultancy cabinet = consultancy();
        Company a = company(cabinet, true);
        Company b = company(cabinet, true);
        Company inactive = company(cabinet, false);
        Company foreign = company(consultancy(), true);

        AppUser ana = consultant(cabinet, true, null);
        consultant(cabinet, false, null);              // invitație nefolosită
        consultant(cabinet, false, Instant.now());     // dezactivat

        ReportingDeadline dueToday = deadline(a, TODAY, DeadlineStatus.UPCOMING);
        ReportingDeadline dueInSeven = deadline(b, TODAY.plusDays(7), DeadlineStatus.UPCOMING);
        deadline(a, TODAY.plusDays(8), DeadlineStatus.UPCOMING);      // în afara ferestrei
        deadline(a, TODAY.minusDays(1), DeadlineStatus.UPCOMING);     // depășit — e pe panou, nu în mail
        deadline(b, TODAY.plusDays(3), DeadlineStatus.DONE);          // finalizat
        deadline(inactive, TODAY.plusDays(2), DeadlineStatus.UPCOMING);
        deadline(foreign, TODAY.plusDays(2), DeadlineStatus.UPCOMING);

        scheduler.dispatch(TODAY);

        ArgumentCaptor<List<ReportingDeadline>> deadlines = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> recipients = ArgumentCaptor.forClass(List.class);
        verify(notificationService).sendConsultantDigest(
                eq(cabinet.getName()), deadlines.capture(), recipients.capture(), eq(TODAY));

        assertThat(deadlines.getValue()).extracting(ReportingDeadline::getId)
                .containsExactly(dueToday.getId(), dueInSeven.getId());
        assertThat(recipients.getValue()).containsExactly(ana.getEmail());
    }

    @Test
    void noMailWhenNothingIsDueOrNobodyCanReadIt() {
        Consultancy quiet = consultancy();
        deadline(company(quiet, true), TODAY.plusDays(20), DeadlineStatus.UPCOMING);
        consultant(quiet, true, null);

        Consultancy unread = consultancy();
        deadline(company(unread, true), TODAY.plusDays(1), DeadlineStatus.UPCOMING);
        consultant(unread, false, null);

        scheduler.dispatch(TODAY);

        verify(notificationService, never()).sendConsultantDigest(eq(quiet.getName()), any(), any(), any());
        verify(notificationService, never()).sendConsultantDigest(eq(unread.getName()), any(), any(), any());
    }

    @Test
    void aCabinetWhoseMailFailsDoesNotStopTheNextOne() {
        Consultancy failing = consultancy();
        deadline(company(failing, true), TODAY.plusDays(1), DeadlineStatus.UPCOMING);
        consultant(failing, true, null);
        Consultancy fine = consultancy();
        deadline(company(fine, true), TODAY.plusDays(1), DeadlineStatus.UPCOMING);
        consultant(fine, true, null);

        Mockito.doThrow(new RuntimeException("smtp down")).when(notificationService)
                .sendConsultantDigest(eq(failing.getName()), any(), any(), any());

        scheduler.dispatch(TODAY);

        verify(notificationService).sendConsultantDigest(eq(fine.getName()), any(), any(), eq(TODAY));
    }

    private Consultancy consultancy() {
        return consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(digitsCui()).createdAt(Instant.now()).build());
    }

    private Company company(Consultancy consultancy, boolean active) {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix()).cui(digitsCui()).type(CompanyType.GENERATOR)
                .consultancy(consultancy).afmObligation(false).active(active).createdAt(Instant.now()).build());
    }

    private AppUser consultant(Consultancy consultancy, boolean enabled, Instant deactivatedAt) {
        return appUserRepository.save(AppUser.builder()
                .email("digest+" + suffix() + "@cabinet.ro").password("x")
                .role(Role.CONSULTANT).consultancy(consultancy)
                .enabled(enabled).deactivatedAt(deactivatedAt).createdAt(Instant.now()).build());
    }

    private ReportingDeadline deadline(Company company, LocalDate dueDate, DeadlineStatus status) {
        return deadlineRepository.save(ReportingDeadline.builder()
                .company(company).reportType(ReportType.SIM_ANNUAL).dueDate(dueDate).status(status)
                .warned7Days(false).warned1Day(false).createdAt(Instant.now()).build());
    }

    private static String digitsCui() {
        return "RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
