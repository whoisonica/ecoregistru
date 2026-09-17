package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Vehicle;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.VehicleRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * D2.1 — alerta de 30 de zile pentru ITP-ul sau licența de transport a unui vehicul. Aceeași formă ca
 * {@link PartnerAuthorizationAlertScheduler}: job de sistem peste toate firmele, fereastră doar înainte,
 * deduplicare după valoarea datei (o dată reînnoită rearmează alerta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleExpiryAlertScheduler {

    /** Ca la cântar (D2.3): destul pentru o programare la ITP, nu atât de devreme încât să fie ignorată. */
    static final int WARNING_WINDOW_DAYS = 30;

    VehicleRepository vehicleRepository;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Tranzacțional pe metoda programată, altfel data trimiterii nu se salvează (vezi {@link DeadlineAlertScheduler}). */
    @Transactional
    @Scheduled(cron = "${app.alerts.vehicle-expiry-cron:0 20 7 * * *}", zone = "Europe/Bucharest")
    public void runDailyVehicleWarnings() {
        dispatchWarnings(DeadlineService.today());
    }

    @Transactional
    public void dispatchWarnings(LocalDate today) {
        List<Vehicle> candidates = vehicleRepository.findWarningCandidates(today, today.plusDays(WARNING_WINDOW_DAYS));
        int sent = 0;
        for (Vehicle vehicle : candidates) {
            // Decide cea mai apropiată dată care n-a trecut. Una deja trecută nu se mai anunță (mailul n-ar
            // preveni nimic, iar ecranul o arată ca expirată), dar nici nu ascunde licența care expiră după ea.
            LocalDate next = Stream.of(vehicle.getItpExpiry(), vehicle.getTransportLicenseExpiry())
                    .filter(Objects::nonNull)
                    .filter(date -> !date.isBefore(today))
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
            if (next.equals(vehicle.getExpiryWarningSentFor())) {
                continue;
            }
            if (notify(vehicle, ChronoUnit.DAYS.between(today, next))) {
                vehicle.setExpiryWarningSentFor(next);
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Vehicle expiry warnings: sent {} of {} candidate vehicle(s).", sent, candidates.size());
        }
    }

    private boolean notify(Vehicle vehicle, long daysUntil) {
        List<String> recipients = appUserRepository
                .findAllByCompany_IdAndEnabledTrue(vehicle.getCompany().getId())
                .stream().map(AppUser::getEmail).toList();
        if (recipients.isEmpty()) {
            return false;
        }
        try {
            notificationService.sendVehicleExpiryWarning(vehicle, recipients, daysUntil);
            return true;
        } catch (Exception e) {
            log.error("Failed to send expiry warning for vehicle {}", vehicle.getId(), e);
            return false;
        }
    }
}
