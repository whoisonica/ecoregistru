package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.ReportType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * P2.13, felia 2 — un rând din panoul „Toate firmele mele": ce cere atenție la o firmă a cabinetului.
 *
 * <p>Fiecare cifră e una pe care Panoul firmei o arată deja, socotită după aceeași regulă, ca un
 * consultant să nu vadă „2 blocaje" aici și „nimic" după ce comută pe firmă.
 *
 * @param overdueDeadlines         termene nefinalizate cu data trecută, din anul trecut până anul viitor
 * @param nextDeadline             cel mai apropiat termen nefinalizat de azi încolo, sau {@code null}
 * @param deadlinesGenerated       anul curent are măcar un termen; altfel „0 depășite" nu înseamnă nimic
 * @param linesWithoutOperationCode linii de evidență din anul curent cu ieșiri fără cod R/D
 * @param linesAwaitingWeighing    linii de evidență din anul curent cu predări care așteaptă cântărirea
 * @param unprovenMirrorMovements  mișcări din anul curent pe cod-oglindă declarat nepericulos, fără document
 * @param partnersExpiring         parteneri activi cu autorizația sau viza expirată ori expirând în 60 de zile
 */
public record ConsultancyOverviewResponse(
        UUID companyId,
        String name,
        String cui,
        CompanyType type,
        int overdueDeadlines,
        NextDeadline nextDeadline,
        boolean deadlinesGenerated,
        int linesWithoutOperationCode,
        int linesAwaitingWeighing,
        int unprovenMirrorMovements,
        int partnersExpiring) {

    public record NextDeadline(ReportType reportType, LocalDate dueDate) {}

    /** Nimic de făcut pe firma asta — termenele sunt generate și nu cad, nimic nu blochează. */
    public boolean clear() {
        return deadlinesGenerated && overdueDeadlines == 0 && blockers() == 0 && partnersExpiring == 0;
    }

    public int blockers() {
        return linesWithoutOperationCode + linesAwaitingWeighing + unprovenMirrorMovements;
    }
}
