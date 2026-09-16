package ro.ecoregistru.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;

import java.time.LocalDate;

/**
 * Care termen nebifat cu data trecută se arată ca depășit, și care nu.
 *
 * <p>Pe 16.09.2026 (api v105) s-au ascuns toate: rămăseseră din generarea veche a anului întreg,
 * „depășite” din prima zi la orice firmă, și nu cereau nimic. Dar filtrul acela ascundea și un
 * termen ratat cu adevărat: un 15 martie nedepus dispărea a doua zi, fără niciun semn, iar la o
 * aplicație de conformitate asta e exact amenda pe care clientul a plătit ca s-o evite.
 *
 * <p>Regula de acum: cele cu scadența <b>înainte de {@code app.deadlines.missed-shown-from}</b>
 * (implicit 17.09.2026, a doua zi după regula „doar următorul”) rămân ascunse, ca istoria veche;
 * cele de atunci încolo rămân pe ecran ca depășite până se bifează, și primesc un mail a doua zi
 * ({@link DeadlineAlertScheduler}). Data e proprietate doar ca testele să nu depindă de calendar.
 */
@Component
public class MissedDeadlinePolicy {

    private final LocalDate shownFrom;

    public MissedDeadlinePolicy(@Value("${app.deadlines.missed-shown-from:2026-09-17}") String shownFrom) {
        this.shownFrom = LocalDate.parse(shownFrom);
    }

    public LocalDate shownFrom() {
        return shownFrom;
    }

    /** Se arată: bifat (istoric), neajuns la scadență, sau ratat după data de la care se arată. */
    public boolean shown(ReportingDeadline d, LocalDate today) {
        return shown(d, today, shownFrom);
    }

    /** Ratat și arătat: nebifat, cu scadența trecută, de la data regulii încolo. */
    public boolean missed(ReportingDeadline d, LocalDate today) {
        return d.getStatus() != DeadlineStatus.DONE && d.getDueDate().isBefore(today)
                && !d.getDueDate().isBefore(shownFrom);
    }

    static boolean shown(ReportingDeadline d, LocalDate today, LocalDate shownFrom) {
        return d.getStatus() == DeadlineStatus.DONE
                || !d.getDueDate().isBefore(today)
                || !d.getDueDate().isBefore(shownFrom);
    }
}
