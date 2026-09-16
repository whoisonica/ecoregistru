package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * D2.2 — alerta de 30 de zile pentru atestatul unui șofer al firmei. Aceeași formă ca
 * {@link VehicleExpiryAlertScheduler}: job de sistem peste toate firmele, deduplicare după valoarea datei.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverAttestationAlertScheduler {

    static final int WARNING_WINDOW_DAYS = VehicleExpiryAlertScheduler.WARNING_WINDOW_DAYS;

    DriverRepository driverRepository;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Tranzacțional pe metoda programată, altfel data trimiterii nu se salvează (vezi {@link DeadlineAlertScheduler}). */
    @Transactional
    @Scheduled(cron = "${app.alerts.driver-attestation-cron:0 25 7 * * *}")
    public void runDailyAttestationWarnings() {
        dispatchWarnings(LocalDate.now());
    }

    @Transactional
    public void dispatchWarnings(LocalDate today) {
        List<Driver> candidates = driverRepository.findAttestationWarningCandidates(today, today.plusDays(WARNING_WINDOW_DAYS));
        int sent = 0;
        for (Driver driver : candidates) {
            LocalDate expiry = driver.getAttestationExpiry();
            if (expiry.equals(driver.getAttestationWarningSentFor())) {
                continue;
            }
            if (notify(driver, ChronoUnit.DAYS.between(today, expiry))) {
                driver.setAttestationWarningSentFor(expiry);
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Driver attestation warnings: sent {} of {} candidate driver(s).", sent, candidates.size());
        }
    }

    private boolean notify(Driver driver, long daysUntil) {
        List<String> recipients = appUserRepository
                .findAllByCompany_IdAndEnabledTrue(driver.getCompany().getId())
                .stream().map(AppUser::getEmail).toList();
        if (recipients.isEmpty()) {
            return false;
        }
        try {
            notificationService.sendDriverAttestationWarning(driver, recipients, daysUntil);
            return true;
        } catch (Exception e) {
            log.error("Failed to send attestation warning for driver {}", driver.getId(), e);
            return false;
        }
    }
}
