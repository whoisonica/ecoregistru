package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.repository.WasteCodeRepository;

import java.time.LocalDate;

/**
 * Lista deșeurilor se schimbă la o dată din act, nu la un deploy. Decizia delegată (UE) 2025/934 se
 * aplică de la 9.11.2026: codurile noi și cele scoase au deja intervalul lor ({@code valid_from} /
 * {@code valid_to}, V80), dar codurile care <em>rămân</em> și își schimbă numele sau pericolul
 * (16 06 04 devine periculos) nu pot sta pe două rânduri — de aici ziua lor „în așteptare”, pe care o
 * scrie aici în {@code name} / {@code hazardous}.
 *
 * <p>Rulează zilnic la 00:05 și la fiecare pornire (un dyno repornit după 9.11 nu așteaptă noaptea).
 * Operația e idempotentă, deci două instanțe care o rulează odată nu strică nimic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteCodeListScheduler {

    WasteCodeRepository wasteCodeRepository;

    /** {@code @Transactional} și aici: apelul către {@link #apply} e din aceeași clasă și ocolește proxy-ul. */
    @Transactional
    @Scheduled(cron = "${app.waste-codes.cron:0 5 0 * * *}", zone = "Europe/Bucharest")
    @EventListener(ApplicationReadyEvent.class)
    public void runDaily() {
        apply(DeadlineService.today());
    }

    /** Separată de programare, ca proba să aleagă ziua. Întoarce câte coduri s-au schimbat. */
    @Transactional
    public int apply(LocalDate today) {
        int changed = wasteCodeRepository.applyPendingChanges(today);
        if (changed > 0) {
            wasteCodeRepository.recomputeMirrors();
            log.info("Waste list: applied {} pending code change(s) due by {}.", changed, today);
        }
        return changed;
    }
}
