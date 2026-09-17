package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.repository.NaturalPersonRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * D1.7 — datele persoanelor fizice de la care depozitul cumpără nu se țin peste termenul borderoului.
 *
 * <p>Borderoul e document financiar-contabil (OUG 31/2011 art. 1 alin. (1^3)) și se păstrează
 * <b>10 ani</b> de la încheierea exercițiului în care s-a întocmit (Legea 82/1991 art. 25). Ca la
 * șoferi, termenul curge pe ani întregi: o operațiune din 2026 ține datele persoanei până la
 * 31.12.2036, iar pe 1 ianuarie 2037 ele pleacă, dacă persoana n-a mai vândut nimic între timp.
 *
 * <p>Persoana nu se șterge, se anonimizează: operațiunile vechi o numesc prin cheie străină, iar
 * numărul lor și cantitățile rămân în registrul art. 48, care nu cere numele vânzătorului.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NaturalPersonRetentionScheduler {

    /** Legea 82/1991 art. 25: documentele justificative se păstrează 10 ani. */
    static final int RETENTION_YEARS = 10;

    NaturalPersonRepository personRepository;

    /**
     * Zilnic la 03:45; lucrează efectiv pe 1 ianuarie. {@code @Transactional} stă și aici, fiindcă apelul
     * către {@link #purge} e din aceeași clasă și ocolește proxy-ul (vezi {@code ScheduledTransactionBoundaryTest}).
     */
    @Transactional
    @Scheduled(cron = "${app.retention.natural-person-cron:0 45 3 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        purge(DeadlineService.today());
    }

    /** Separată de programare, ca proba să aleagă ziua. Întoarce câte persoane a anonimizat. */
    @Transactional
    public int purge(LocalDate today) {
        LocalDate cutoff = LocalDate.of(today.getYear() - RETENTION_YEARS, 1, 1);
        int erased = personRepository.anonymizeUnusedSince(cutoff, cutoff.atStartOfDay().toInstant(ZoneOffset.UTC));
        if (erased > 0) {
            log.info("Natural person retention: anonymized {} person(s) with no operation since {}.", erased, cutoff);
        }
        return erased;
    }
}
