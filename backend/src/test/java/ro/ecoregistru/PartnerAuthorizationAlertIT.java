package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.service.PartnerAuthorizationAlertScheduler;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * FAZA TERMENE, last slice (V30): the 60-day warning that a partner's environmental authorization
 * is about to lapse. Driven deterministically via a fixed "today"; NotificationService is mocked so
 * no SMTP is touched, and the assertions are on {@code authorizationWarningSentFor} — which is both
 * the deduplication key and the record that a mail went out.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PartnerAuthorizationAlertIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Autowired PartnerAuthorizationAlertScheduler scheduler;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired PartnerRepository partnerRepository;

    @MockBean NotificationService notificationService;

    private Company companyWithUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Autorizatie " + suffix).cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        appUserRepository.save(AppUser.builder()
                .email("partner+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return company;
    }

    private Company companyWithoutUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return companyRepository.save(Company.builder()
                .name("Fara useri " + suffix).cui("ROQ" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
    }

    private Partner partner(Company c, LocalDate expiry, LocalDate warnedFor, boolean active) {
        return partnerRepository.save(Partner.builder()
                .company(c).name("Colector " + UUID.randomUUID().toString().substring(0, 6))
                .type(PartnerType.COLLECTOR).client(true).supplier(false).carrier(false)
                .authorizationNumber("AM 123")
                .authorizationExpiry(expiry).authorizationWarningSentFor(warnedFor)
                .active(active).createdAt(Instant.now()).build());
    }

    private LocalDate warnedFor(UUID id) {
        return partnerRepository.findById(id).orElseThrow().getAuthorizationWarningSentFor();
    }

    @Test
    void warnsWhenTheAuthorizationExpiresInsideTheWindow() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(30), null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void warnsOnTheLastDayOfTheWindow() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(60), null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isEqualTo(TODAY.plusDays(60));
    }

    @Test
    void staysQuietBeyondTheWindow() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(61), null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isNull();
    }

    /**
     * The window is forward-only. An authorization that lapsed before today is shown by the partner
     * list badge and by the handover warning of decision 36; a mail about it would prevent nothing
     * and would have flooded the first run with every stale partner at once. See V30.
     */
    @Test
    void staysQuietAboutAnAuthorizationThatHasAlreadyLapsed() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.minusDays(1), null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isNull();
    }

    @Test
    void doesNotWarnTwiceForTheSameExpiry() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(30), TODAY.plusDays(30), true);

        scheduler.dispatchWarnings(TODAY);

        Mockito.verify(notificationService, Mockito.never())
                .sendPartnerAuthorizationWarning(any(), any(), anyLong());
    }

    /**
     * The reason the column holds a date instead of a boolean: renewing the authorization moves the
     * expiry, which no longer equals what we last warned for, so the alert re-arms without anybody
     * clearing a flag. A boolean would have let the second term pass in silence.
     */
    @Test
    void renewingTheAuthorizationReArmsTheAlert() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(30), TODAY.minusYears(1), true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void ignoresInactivePartners() {
        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(30), null, false);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isNull();
    }

    @Test
    void ignoresPartnersWithoutAnExpiryOnFile() {
        Company c = companyWithUser();
        Partner p = partner(c, null, null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isNull();
    }

    /** Nobody to tell yet: leave it unmarked so the warning goes out once a user exists. */
    @Test
    void doesNotMarkWhenTheTenantHasNoUsers() {
        Partner p = partner(companyWithoutUsers(), TODAY.plusDays(30), null, true);

        scheduler.dispatchWarnings(TODAY);

        assertThat(warnedFor(p.getId())).isNull();
    }

    @Test
    void doesNotMarkWhenDeliveryFails() {
        Mockito.doThrow(new RuntimeException("smtp down"))
                .when(notificationService).sendPartnerAuthorizationWarning(any(), any(), anyLong());

        Company c = companyWithUser();
        Partner p = partner(c, TODAY.plusDays(30), null, true);

        scheduler.dispatchWarnings(TODAY);

        // Delivery failed -> stays unmarked so the warning is retried tomorrow.
        assertThat(warnedFor(p.getId())).isNull();
    }
}
