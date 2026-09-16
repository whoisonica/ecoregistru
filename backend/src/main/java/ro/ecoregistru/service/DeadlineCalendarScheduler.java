package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.repository.CompanyRepository;

import java.time.LocalDate;

/**
 * Ține calendarul de termene la zi fără buton (16.09.2026): în fiecare dimineață, pe ora României,
 * fiecare firmă activă primește următorul termen al fiecărui fel pe care îl datorează — cel de după
 * unul care tocmai a trecut, sau 30 aprilie când a apărut prima mișcare cu ulei uzat.
 *
 * <p>Rulează înaintea mementourilor ({@link DeadlineAlertScheduler}), ca un termen apărut azi să
 * poată primi mementoul chiar azi. Fiecare firmă are tranzacția ei ({@code ensureUpcoming} trece prin
 * proxy), iar una căzută nu le oprește pe celelalte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeadlineCalendarScheduler {

    CompanyRepository companyRepository;
    DeadlineService deadlineService;

    @Scheduled(cron = "${app.alerts.deadline-calendar-cron:0 45 6 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        run(DeadlineService.today());
    }

    /** Separat de programare, ca testele să aleagă ziua. Întoarce câte termene noi au apărut. */
    public int run(LocalDate today) {
        int created = 0;
        for (Company company : companyRepository.findAllByActiveTrue()) {
            try {
                created += deadlineService.ensureUpcoming(company.getId(), today);
            } catch (Exception e) {
                log.error("Failed to complete the deadline calendar for company {}", company.getId(), e);
            }
        }
        if (created > 0) {
            log.info("Deadline calendar: {} new deadlines", created);
        }
        return created;
    }
}
