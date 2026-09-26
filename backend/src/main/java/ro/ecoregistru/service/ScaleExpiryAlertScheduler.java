package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Scale;
import ro.ecoregistru.enums.ScaleStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.ScaleRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * D2.3 — alerta de 30 de zile înainte să expire verificarea metrologică a unui cântar în uz. Legea nu
 * cere preaviz; cele 30 de zile sunt alegerea noastră, ca la vehicule ({@link VehicleExpiryAlertScheduler}):
 * destul pentru o programare la laborator. Deduplicare după valoarea datei: o verificare nouă o rearmează.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleExpiryAlertScheduler {

    static final int WARNING_WINDOW_DAYS = 30;

    ScaleRepository scaleRepository;
    ScaleService scaleService;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Tranzacțional pe metoda programată, altfel data trimiterii nu se salvează (vezi {@link DeadlineAlertScheduler}). */
    @Transactional
    @Scheduled(cron = "${app.alerts.scale-expiry-cron:0 25 7 * * *}", zone = "Europe/Bucharest")
    public void runDailyScaleWarnings() {
        dispatchWarnings(DeadlineService.today());
    }

    @Transactional
    public void dispatchWarnings(LocalDate today) {
        int sent = 0;
        List<Scale> scales = scaleRepository.findAllByStatusWithCompany(ScaleStatus.IN_USE);
        for (Scale scale : scales) {
            ScaleLegality.Verdict verdict = scaleService.verdictAt(scale, today);
            LocalDate until = verdict.validUntil();
            // Doar o valabilitate care ține încă (VALID sau nedeclarat): una căzută — reparație, respins,
            // expirată — n-are dată în viitor, iar ecranul și cântarul o arată deja.
            if (until == null || until.isBefore(today) || until.isAfter(today.plusDays(WARNING_WINDOW_DAYS))
                    || until.equals(scale.getExpiryWarningSentFor())) {
                continue;
            }
            if (notify(scale, ChronoUnit.DAYS.between(today, until))) {
                scale.setExpiryWarningSentFor(until);
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Scale expiry warnings: sent {} of {} scale(s) in use.", sent, scales.size());
        }
    }

    private boolean notify(Scale scale, long daysUntil) {
        List<String> recipients = appUserRepository
                .findAllByCompany_IdAndEnabledTrue(scale.getCompany().getId())
                .stream().map(AppUser::getEmail).toList();
        if (recipients.isEmpty()) {
            return false;
        }
        try {
            notificationService.sendScaleExpiryWarning(scale, recipients, daysUntil);
            return true;
        } catch (Exception e) {
            log.error("Failed to send expiry warning for scale {}", scale.getId(), e);
            return false;
        }
    }
}
