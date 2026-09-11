package ro.ecoregistru.audit;

import java.util.ArrayList;
import java.util.List;

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

    static void add(PendingAudit entry) {
        List<PendingAudit> list = PENDING.get();
        if (list == null) {
            list = new ArrayList<>();
            PENDING.set(list);
        }
        list.add(entry);
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
