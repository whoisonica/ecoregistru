package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Warns a tenant, 60 days ahead, that a partner's environmental authorization is about to lapse
 * (FAZA TERMENE, last slice; migration V30). Runs daily as a system job across all tenants, with no
 * {@code TenantContext} — the same shape as {@link DeadlineAlertScheduler}.
 *
 * <p><b>Why the tenant cares.</b> OUG 92/2021 art. 23 alin. (1) allows a handover only towards an
 * <em>authorized</em> operator, and alin. (2) says handing the waste over does not discharge
 * responsibility. An expired partner authorization is therefore the tenant's exposure, not the
 * partner's.
 *
 * <p><b>Why this is not decision 36 again.</b> Decision 36 compares the expiry with the <em>date of
 * the movement</em> and paints a yellow badge on screen — it signals <em>after</em>, once the
 * handover has happened and nothing can be undone. This one writes <em>before</em>, while the
 * partner can still renew or the client can still pick someone else. One is a finding, the other is
 * a chance.
 *
 * <p><b>Why a separate class rather than a method on the deadline scheduler.</b> A reporting
 * deadline and a partner authorization share only the fact that they have a date. They have
 * different windows, different deduplication, different recipients-of-consequence and different
 * failure modes, and the deadline scheduler's name would have stopped being true. They are two jobs
 * that happen to run on the same morning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerAuthorizationAlertScheduler {

    /**
     * How far ahead the warning goes out. Deliberately the same 60 days as
     * {@code PartnerService.EXPIRY_WARNING_DAYS}, which paints the badge in the partner list: the
     * mail and the badge should not disagree about what "expiring soon" means. The two constants
     * stay separate because they are read by different layers — a shared one would tie the alert
     * scheduler to the CRUD service for a number, not for a behaviour.
     */
    static final int WARNING_WINDOW_DAYS = 60;

    PartnerRepository partnerRepository;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Daily at 07:15 (server time), just after the deadline reminders. Cron is overridable. */
    @Scheduled(cron = "${app.alerts.partner-authorization-cron:0 15 7 * * *}")
    public void runDailyAuthorizationWarnings() {
        dispatchWarnings(LocalDate.now());
    }

    /**
     * Core logic, separated from scheduling so it can be driven deterministically in tests.
     *
     * <p>The window is {@code [today, today + 60]} — forward only. An authorization that lapsed in
     * the past is not a candidate: warning about it prevents nothing, it is already visible in the
     * partner list badge and at handover time, and including the past would have meant one mail per
     * stale partner on the very first run. V30 carries the full reasoning.
     */
    @Transactional
    public void dispatchWarnings(LocalDate today) {
        List<Partner> candidates = partnerRepository.findAllByActiveTrueAndAuthorizationExpiryBetween(
                today, today.plusDays(WARNING_WINDOW_DAYS));

        int sent = 0;
        for (Partner partner : candidates) {
            // Deduplication is by value, not by event: we warn once per expiry date, so renewing
            // the authorization re-arms the alert on its own. See V30.
            if (partner.getAuthorizationExpiry().equals(partner.getAuthorizationWarningSentFor())) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(today, partner.getAuthorizationExpiry());
            if (notify(partner, daysUntil)) {
                partner.setAuthorizationWarningSentFor(partner.getAuthorizationExpiry());
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Partner authorization warnings: sent {} of {} candidate partner(s).",
                    sent, candidates.size());
        }
    }

    /** Returns true only if the warning was delivered, so the caller may mark the partner. */
    private boolean notify(Partner partner, long daysUntil) {
        List<String> recipients = appUserRepository
                .findAllByCompany_IdAndEnabledTrue(partner.getCompany().getId())
                .stream().map(AppUser::getEmail).toList();
        if (recipients.isEmpty()) {
            return false; // no one to tell yet — leave unmarked so it retries when users exist
        }
        try {
            notificationService.sendPartnerAuthorizationWarning(partner, recipients, daysUntil);
            return true;
        } catch (Exception e) {
            log.error("Failed to send authorization warning for partner {} ({})",
                    partner.getId(), partner.getName(), e);
            return false;
        }
    }
}
