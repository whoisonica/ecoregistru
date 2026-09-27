package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * F6a — mementoul SIATD de dimineață: pe fiecare firmă cu un modul bifat, recepțiile neconfirmate al căror memento
 * (o zi înainte) sau termen e azi, într-un singur mail către cei care pot confirma. Fără stare „trimis”: e un rezumat
 * zilnic, iar o recepție poate apărea două zile la rând. Ce e ratat nu mai pleacă — stă în tab, la „Ratate”.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiatdAlertScheduler {

    private static final Set<Role> RECIPIENTS = EnumSet.of(Role.ADMIN, Role.CONSULTANT);

    CompanyRepository companyRepository;
    AppUserRepository appUserRepository;
    SiatdService siatdService;
    NotificationService notificationService;

    /** Tranzacțional pe metoda programată: un apel intern ar ocoli proxy-ul (garda {@code ScheduledTransactionBoundaryTest}). */
    @Transactional(readOnly = true)
    @Scheduled(cron = "${app.alerts.siatd-cron:0 5 7 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        dispatch(DeadlineService.today());
    }

    /** @return câte firme au primit mailul */
    @Transactional(readOnly = true)
    public int dispatch(LocalDate today) {
        int sent = 0;
        for (Company company : companyRepository.findAllWithSiatdModule()) {
            try {
                List<SiatdReceptionRow> rows = siatdService.pendingFor(company.getId(), today).stream()
                        .filter(r -> r.reminder().equals(today) || r.due().equals(today))
                        .toList();
                if (rows.isEmpty()) {
                    continue;
                }
                List<String> to = appUserRepository.findAllByCompany_IdAndEnabledTrue(company.getId()).stream()
                        .filter(u -> RECIPIENTS.contains(u.getRole()))
                        .map(AppUser::getEmail)
                        .toList();
                if (to.isEmpty()) {
                    continue;
                }
                notificationService.sendSiatdReminder(company, rows, to);
                sent++;
            } catch (Exception e) {
                log.error("SIATD reminder failed for company {}", company.getId(), e);
            }
        }
        if (sent > 0) {
            log.info("SIATD reminders: sent to {} company(ies).", sent);
        }
        return sent;
    }
}
