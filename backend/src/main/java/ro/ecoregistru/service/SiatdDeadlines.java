package ro.ecoregistru.service;

import ro.ecoregistru.enums.SiatdModule;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.util.SiatdCalendar;
import ro.ecoregistru.util.SiatdFlow;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * F6a — termenul de confirmare SIATD al unei recepții (Instrucțiunile Ord. 701/2024 art. 18 alin. (6)–(10)). Termenul e al
 * celui care preia, deci doar pe intrări; curge de la recepție (și la o operațiune încă în lucru), sau de la înrolare
 * dacă recepția a fost înainte. O recepție pe mai multe module ia termenul cel mai scurt. Nu se stochează: se calculează
 * la citire, ca o dată de înrolare schimbată să mute corect tot ce nu e confirmat.
 */
public final class SiatdDeadlines {

    public record SiatdDeadline(Set<SiatdModule> modules, LocalDate start, LocalDate reminder, LocalDate due) {

        public enum State { PENDING, CONFIRMED, MISSED }

        /** Confirmată rămâne confirmată oricât de târziu; neconfirmată, după ziua termenului e ratată. */
        public State state(LocalDate today, boolean confirmed) {
            if (confirmed) {
                return State.CONFIRMED;
            }
            return today.isAfter(due) ? State.MISSED : State.PENDING;
        }
    }

    private SiatdDeadlines() {
    }

    /** {@code null} = recepția n-are termen (alt tip, anulată, niciun cod pe un modul bifat). */
    public static SiatdDeadline of(WeighingOperationType type, WeighingOperationStatus status, LocalDate date,
                                   Collection<String> wasteCodes, Map<SiatdModule, LocalDate> enrolment) {
        if (type != WeighingOperationType.IN
                || (status != WeighingOperationStatus.IN_PROGRESS && status != WeighingOperationStatus.FINALIZED)) {
            return null;
        }
        Set<SiatdModule> modules = EnumSet.noneOf(SiatdModule.class);
        for (String code : wasteCodes) {
            SiatdFlow.of(code).filter(enrolment::containsKey).ifPresent(modules::add);
        }
        SiatdDeadline earliest = null;
        for (SiatdModule module : modules) {
            LocalDate enrolled = enrolment.get(module);
            LocalDate start = enrolled.isAfter(date) ? enrolled : date;
            LocalDate due = SiatdCalendar.due(start, module.days());
            if (earliest == null || due.isBefore(earliest.due())) {
                earliest = new SiatdDeadline(java.util.Collections.unmodifiableSet(modules), start, SiatdCalendar.reminder(start, module.days()), due);
            }
        }
        return earliest;
    }
}
