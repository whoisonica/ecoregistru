package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.util.List;

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

    CompanyRepository companyRepository;
    AppUserRepository appUserRepository;
    SiatdService siatdService;
    NotificationService notificationService;

    /**
     * <b>Fără tranzacție comună, dinadins:</b> fiecare firmă se citește în tranzacția lui {@code SiatdService.pendingFor}.
     * Una comună ar fi marcată rollback-only de prima firmă care cade, iar o eroare SQL ar opri toate firmele de după ea
     * (recenzia finală F6a, 28.09.2026). Clasa n-are nicio metodă {@code @Transactional}, deci garda
     * {@code ScheduledTransactionBoundaryTest} nu se aplică.
     */
    @Scheduled(cron = "${app.alerts.siatd-cron:0 5 7 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        dispatch(DeadlineService.today());
    }

    /** @return câte firme au primit mailul */
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
                List<String> to = recipients(company);
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

    /**
     * Cine poate confirma: administratorii activi ai firmei și consultanții activi ai cabinetului ei. Consultantul n-are
     * firmă (V40), ci cabinet, deci nu iese din utilizatorii firmei.
     */
    private List<String> recipients(Company company) {
        List<AppUser> users = new java.util.ArrayList<>(appUserRepository.findAllByCompany_IdAndEnabledTrue(company.getId())
                .stream().filter(u -> u.getRole() == Role.ADMIN).toList());
        if (company.getConsultancy() != null) {
            users.addAll(appUserRepository.findAllByConsultancy_IdAndEnabledTrue(company.getConsultancy().getId()));
        }
        return users.stream().map(AppUser::getEmail).distinct().toList();
    }
}
