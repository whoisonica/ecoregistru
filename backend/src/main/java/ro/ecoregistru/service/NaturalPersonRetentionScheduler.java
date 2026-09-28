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
 * <b>5 ani calculați de la 1 iulie a anului următor</b> exercițiului în care s-a întocmit (Legea 82/1991
 * art. 25, modificat prin Legea 36/2023; `surse-oficiale.md` §15). Decizia proprietarului, 28.09.2026:
 * datele pleacă la termenul legal, nu la cel vechi de 10 ani. O operațiune din 2026 ține datele persoanei
 * până pe 30.06.2032, iar pe 1 iulie 2032 ele pleacă, dacă persoana n-a mai vândut nimic între timp.
 *
 * <p>Prescripția fiscală (Codul de procedură fiscală art. 110–111) se poate întrerupe printr-un control;
 * jobul nu știe de asta. Ce se păstrează peste termen pentru un control în curs e o decizie a firmei.
 *
 * <p>Persoana nu se șterge, se anonimizează: operațiunile vechi o numesc prin cheie străină, iar
 * numărul lor și cantitățile rămân în registrul art. 48, care nu cere numele vânzătorului.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NaturalPersonRetentionScheduler {

    /** Legea 82/1991 art. 25: documentele justificative se păstrează 5 ani, socotiți de la 1 iulie. */
    static final int RETENTION_YEARS = 5;

    NaturalPersonRepository personRepository;

    /**
     * Zilnic la 03:45; lucrează efectiv pe 1 iulie. {@code @Transactional} stă și aici, fiindcă apelul
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
        // Termenul exercițiului Y se încheie pe 1 iulie Y + 1 + 5: de la 1 iulie încolo iese exercițiul de acum
        // șase ani, până atunci cel de acum șapte. Pleacă cine n-are nicio operațiune după ultimul exercițiu ieșit.
        int lastExpiredYear = today.getYear() - RETENTION_YEARS - (today.getMonthValue() >= 7 ? 1 : 2);
        LocalDate cutoff = LocalDate.of(lastExpiredYear + 1, 1, 1);
        int erased = personRepository.anonymizeUnusedSince(cutoff, cutoff.atStartOfDay().toInstant(ZoneOffset.UTC));
        if (erased > 0) {
            log.info("Natural person retention: anonymized {} person(s) with no operation since {}.", erased, cutoff);
        }
        return erased;
    }
}
