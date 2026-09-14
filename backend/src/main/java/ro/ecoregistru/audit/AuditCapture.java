package ro.ecoregistru.audit;

import ro.ecoregistru.enums.AuditAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lista faptelor prinse pe firul cererii curente, între flush şi commit.
 *
 * <p>E un {@link ThreadLocal} din acelaşi motiv ca {@code TenantContext}: interceptorul de
 * Hibernate e un singleton pe care îl vede orice sesiune, iar ce prinde el aparţine cererii, nu
 * aplicaţiei.
 *
 * <p>⚠️ <b>Se goleşte la încheierea tranzacţiei, nu la scriere</b> — inclusiv când tranzacţia dă
 * înapoi. O modificare revenită nu s-a întâmplat, iar un jurnal care ar scrie-o ar minţi în
 * direcţia cea mai proastă: ar arăta o faptă care nu e în date.
 */
public final class AuditCapture {

    private static final ThreadLocal<List<PendingAudit>> PENDING = new ThreadLocal<>();

    private AuditCapture() {
    }

    /**
     * <b>BUG-013.</b> Aceeaşi modificare se putea prinde de două ori: o interogare din mijlocul
     * tranzacţiei porneşte un auto-flush, Hibernate cheamă {@code onFlushDirty} încă din verificarea
     * de „murdărie", iar dacă tabelele interogate nu privesc entitatea, flush-ul nu se mai execută —
     * starea încărcată rămâne cea veche. Flush-ul de dinainte de commit o vede apoi a doua oară, cu
     * aceleaşi valori. Un {@code PUT} pe mişcare lăsa aşa două rânduri {@code UPDATE} identice.
     *
     * <p>Deci o faptă nouă despre un rând care are deja una în listă se <b>contopeşte</b> cu ea: pe
     * fiecare câmp rămâne prima valoare veche şi ultima nouă, iar un câmp întors la valoarea de
     * plecare dispare. Un rând de jurnal pe rând de date, pe tranzacţie — ce a fost înainte şi ce a
     * rămas. Crearea stă separat: „a fost creat" şi „a fost apoi schimbat" sunt două fapte.
     */
    static void add(PendingAudit entry) {
        List<PendingAudit> list = PENDING.get();
        if (list == null) {
            list = new ArrayList<>();
            PENDING.set(list);
        }
        if (entry.entityId() != null && entry.action() != AuditAction.CREATE) {
            for (int i = 0; i < list.size(); i++) {
                PendingAudit earlier = list.get(i);
                if (earlier.action() != AuditAction.CREATE
                        && entry.entityId().equals(earlier.entityId())
                        && entry.entityType().equals(earlier.entityType())) {
                    list.set(i, merge(earlier, entry));
                    return;
                }
            }
        }
        list.add(entry);
    }

    private static PendingAudit merge(PendingAudit earlier, PendingAudit later) {
        Map<String, PendingAudit.FieldChange> byField = new LinkedHashMap<>();
        for (PendingAudit.FieldChange c : earlier.changes()) {
            byField.put(c.field(), c);
        }
        for (PendingAudit.FieldChange c : later.changes()) {
            PendingAudit.FieldChange first = byField.get(c.field());
            byField.put(c.field(), first == null ? c : new PendingAudit.FieldChange(c.field(), first.from(), c.to()));
        }
        byField.values().removeIf(c -> Objects.equals(c.from(), c.to()));
        // Verbul mai precis câştigă: o ştergere sau o dezactivare nu redevine „modificare".
        AuditAction action = later.action() != AuditAction.UPDATE ? later.action() : earlier.action();
        return new PendingAudit(later.entityType(), later.entityId(), action, later.label(),
                List.copyOf(byField.values()));
    }

    /** Ce s-a prins până acum, golind lista. Nu întoarce niciodată null. */
    public static List<PendingAudit> drain() {
        List<PendingAudit> list = PENDING.get();
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        PENDING.set(new ArrayList<>());
        return list;
    }

    public static boolean isEmpty() {
        List<PendingAudit> list = PENDING.get();
        return list == null || list.isEmpty();
    }

    public static void clear() {
        PENDING.remove();
    }
}
