package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.util.List;

/**
 * P2.13, felia 2 — rezumatul zilnic al termenelor, pentru consultanți.
 *
 * <p><b>Decizia proprietarului (15.09.2026): un singur mail pe zi, cumulat.</b> {@link DeadlineAlertScheduler}
 * scrie utilizatorilor firmei câte un mail pe termen; la un cabinet cu 20 de firme, același tipar ar fi
 * însemnat zeci de mailuri pe săptămână. Aici fiecare consultant activ primește un mail care numește
 * toate termenele nefinalizate din următoarele {@value #WINDOW_DAYS} zile ale firmelor active din
 * cabinet, și nimic în zilele în care nu e nimic. Utilizatorii firmei își păstrează mailurile lor.
 *
 * <p><b>Fără fanioane de „trimis"</b>, spre deosebire de alertele firmei: rezumatul nu e un eveniment,
 * e starea zilei. Un mail căzut azi nu se retrimite, fiindcă termenele lui sunt oricum în mailul de
 * mâine cât timp rămân deschise. Termenele depășite nu intră: ele sunt pe panou, iar un mail care le
 * repetă zilnic ar fi zgomotul pe care rezumatul l-a înlocuit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultantDigestScheduler {

    static final int WINDOW_DAYS = 7;

    ConsultancyRepository consultancyRepository;
    ReportingDeadlineRepository deadlineRepository;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Zilnic la 07:30, după alertele firmelor (07:00) și ale partenerilor (07:15). Tranzacția stă și aici: apelul intern ocolește proxy-ul. */
    @Transactional(readOnly = true)
    @Scheduled(cron = "${app.alerts.consultant-digest-cron:0 30 7 * * *}", zone = "Europe/Bucharest")
    public void runDailyDigest() {
        dispatch(LocalDate.now());
    }

    /** Separat de programare, ca testele să aleagă ziua. Întoarce câte cabinete au primit rezumatul. */
    @Transactional(readOnly = true)
    public int dispatch(LocalDate today) {
        int sent = 0;
        for (Consultancy consultancy : consultancyRepository.findAll()) {
            List<ReportingDeadline> due = deadlineRepository.findOpenForConsultancy(
                    consultancy.getId(), today, today.plusDays(WINDOW_DAYS));
            if (due.isEmpty()) {
                continue;
            }
            List<String> recipients = appUserRepository.findAllByConsultancy_IdAndEnabledTrue(consultancy.getId())
                    .stream().map(AppUser::getEmail).toList();
            if (recipients.isEmpty()) {
                continue;
            }
            try {
                notificationService.sendConsultantDigest(consultancy.getName(), due, recipients, today);
                sent++;
            } catch (Exception e) {
                // Un cabinet căzut nu îi oprește pe ceilalți.
                log.error("Failed to send the consultant digest for consultancy {}", consultancy.getId(), e);
            }
        }
        if (sent > 0) {
            log.info("Consultant digest: sent to {} consultanc(ies).", sent);
        }
        return sent;
    }
}
