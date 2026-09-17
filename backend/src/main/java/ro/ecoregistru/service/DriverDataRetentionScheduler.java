package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.repository.WasteMovementRepository;

import java.time.LocalDate;

/**
 * AO, 14.09.2026 — datele delegatului nu se ţin mai mult decât evidenţa pe care o servesc.
 *
 * <p>Numele şi actul de identitate al şoferului sunt singurele date personale ale cuiva din afara
 * firmei, şi se tipăresc pe Anexa 3. Se ţin cât se păstrează evidenţa: OUG 92/2021 art. 48 alin. (5),
 * „cel puţin 3 ani". Specialista a lăsat decizia la noi, „cu atenţie la GDPR"; decizia e să nu le
 * ţinem nici o zi peste termen.
 *
 * <p><b>De ce anul întreg, nu ziua.</b> Evidenţa se ţine şi se depune pe an (15 martie anul următor),
 * deci termenul curge de la încheierea anului: mişcările din 2022 pleacă abia pe 1 ianuarie 2026,
 * după trei ani calendaristici întregi (2023, 2024, 2025). Termenul din act e minim, iar la îndoială
 * ţinem mai mult, nu mai puţin.
 *
 * <p>Transportatorii au 12 luni în acelaşi alineat, dar clientul nostru îşi ţine evidenţa lui, nu pe
 * a transportatorului, deci se aplică termenul lui.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverDataRetentionScheduler {

    /** OUG 92/2021 art. 48 alin. (5): evidenţa se păstrează cel puţin 3 ani. */
    static final int RETENTION_YEARS = 3;

    WasteMovementRepository movementRepository;

    /**
     * Zilnic la 03:30; lucrează efectiv o singură dată pe an, pe 1 ianuarie, restul zilelor atinge 0.
     *
     * <p>{@code @Transactional} stă și aici, nu doar pe {@link #purge}: apelul de mai jos e din aceeași
     * clasă și ocolește proxy-ul, deci tranzacția lui {@code purge} nu se deschide. Pe producție
     * UPDATE-ul a căzut așa în fiecare noapte („Executing an update/delete query”), până la 15.09.2026.
     */
    @Transactional
    @Scheduled(cron = "${app.retention.driver-data-cron:0 30 3 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        purge(DeadlineService.today());
    }

    /** Separată de programare, ca proba să aleagă ziua. Întoarce câte mişcări a atins. */
    @Transactional
    public int purge(LocalDate today) {
        LocalDate cutoff = LocalDate.of(today.getYear() - RETENTION_YEARS, 1, 1);
        int cleared = movementRepository.clearDriverDataBefore(cutoff);
        if (cleared > 0) {
            log.info("Driver data retention: cleared driver name and ID on {} movement(s) dated before {}.",
                    cleared, cutoff);
        }
        return cleared;
    }
}
