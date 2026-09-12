package ro.ecoregistru.audit;

import org.hibernate.Interceptor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.AuditAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prinde scrierile auditate acolo unde nu pot fi uitate: în flush-ul lui Hibernate.
 *
 * <p><b>De ce aici şi nu în servicii.</b> Un apel scris de mână în fiecare loc care salvează ceva
 * e corect până în ziua în care cineva adaugă al şaselea drum de editare şi nu ştie că trebuie
 * să-l scrie. Proiectul ăsta a plătit de câteva ori exact greşeala asta — o regulă ţinută prin
 * disciplină, nu prin construcţie. Interceptorul vede <b>orice</b> scriere, inclusiv pe cele care
 * vin prin cascadă, iar ce nu vrem se exclude pe faţă, într-o listă care se citeşte dintr-o privire.
 *
 * <p><b>Şi de ce o listă albă, nu una neagră.</b> Auditul e o promisiune făcută în DPA, nu un log
 * de depanare: ce intră în el trebuie să fie o alegere, nu un rest. {@link #AUDITED} e alegerea —
 * ce ajunge pe hârtie sau priveşte o persoană. Evidenţa lunară lipseşte dinadins: e un cache
 * recalculabil, iar o regenerare de an ar scrie mii de rânduri despre o apăsare de buton (fapta
 * aceea se scrie o dată, explicit, din {@code EvidenceCalculator}).
 *
 * <p><b>Nu citeşte nimic din entitate.</b> Tot ce scrie — eticheta şi valorile — vine din tablourile
 * de stare pe care i le dă Hibernate. Un {@code getWasteCode().getCode()} în mijlocul unui flush ar
 * iniţializa un proxy exact în momentul în care sesiunea e cel mai puţin dispusă s-o facă; de aceea
 * câmpurile care trimit la alt rând ies de aici cu identificatorul, iar numele se pune la citire.
 */
@Component
public class AuditInterceptor implements Interceptor {

    /**
     * Cine scrie faptele prinse, cerut abia la prima prindere.
     *
     * <p>⚠️ Nu injectat direct, şi nu din gust: interceptorul se naşte <b>înaintea</b> fabricii de
     * sesiuni (o configurează), iar {@code AuditWriter} are nevoie de un repository, adică de
     * fabrica aceea. Injectat pe faţă, lanţul s-ar închide în cerc şi aplicaţia n-ar mai porni.
     * Un {@link ObjectProvider} e doar un mâner: se deschide la prima scriere auditată, când tot
     * ce trebuie există de mult.
     */
    private final ObjectProvider<ro.ecoregistru.audit.AuditWriter> writer;

    public AuditInterceptor(ObjectProvider<ro.ecoregistru.audit.AuditWriter> writer) {
        this.writer = writer;
    }

    /** Ce se auditează. Vezi javadocul clasei pentru de ce e listă albă. */
    private static final Set<Class<?>> AUDITED = Set.of(
            WasteMovement.class,
            Company.class,
            Partner.class,
            PartnerWorkPoint.class,
            WorkPoint.class,
            InternalGenerator.class,
            Driver.class,
            AnalysisBulletin.class,
            Attachment.class,
            AppUser.class);

    /**
     * Câmpurile din care se compune eticheta, pe tip, în ordinea în care se citesc.
     *
     * <p>Toate sunt câmpuri simple — niciunul nu trimite la alt rând —, fiindcă eticheta se
     * calculează în mijlocul flush-ului. „Mişcarea din 11.09.2026" e o etichetă mai slabă decât
     * „11.09.2026 · 20 01 01", şi e etichetă de câte ori codul e o asociere.
     */
    private static final Map<Class<?>, List<String>> LABEL_FIELDS = Map.of(
            WasteMovement.class, List.of("date", "documentReference"),
            Company.class, List.of("name"),
            Partner.class, List.of("name"),
            PartnerWorkPoint.class, List.of("name"),
            WorkPoint.class, List.of("name"),
            InternalGenerator.class, List.of("name"),
            Driver.class, List.of("name"),
            AnalysisBulletin.class, List.of("issueDate", "laboratory"),
            Attachment.class, List.of("fileName"),
            AppUser.class, List.of("email"));

    /**
     * Ce nu se scrie niciodată ca modificare.
     *
     * <p>{@code password} e evident. Celelalte sunt zgomot de infrastructură: {@code updatedAt} se
     * schimbă la fiecare scriere, deci ar apărea pe fiecare rând de jurnal fără să spună nimic, iar
     * {@code tokenVersion} creşte la o rotire de sesiune, nu la o faptă a cuiva asupra datelor.
     */
    private static final Set<String> IGNORED_FIELDS =
            Set.of("password", "updatedAt", "createdAt", "createdBy", "tokenVersion",
                    // Contorul de blocare optimistă. Creşte la fiecare scriere şi nu spune nimic
                    // despre ce s-a schimbat — prins de probă, care cerea „exact un câmp".
                    "version",
                    // Spun a doua oară ce spun deja fapta şi autorul rândului de jurnal.
                    "deletedAt", "deletedBy", "deactivatedAt");

    /**
     * Ce se scrie ca faptă, dar fără valori. <b>BUG-002.</b>
     *
     * <p>Rubrica de şofer a Anexei 3 la HG 1061/2008 e una singură — „serie CI sau CNP" — deci
     * câmpul e liber şi ţine, în practică, un CNP. Auditarea lui scria perechea întreagă
     * {@code vechi → nou} într-o tabelă din care {@link ro.ecoregistru.repository.AuditLogRepository}
     * nu are, dinadins, drum de ştergere — şi pe care căutarea liberă o parcurge, deci jurnalul
     * răspundea la o căutare după CNP. Trei lucruri se adunau: valoarea <b>veche</b> supravieţuia
     * corecturii (iar un CNP tastat greşit e al altcuiva), nu se putea şterge la o cerere art. 17
     * GDPR, şi devenea index de persoane.
     *
     * <p><b>De ce redactare şi nu {@link #IGNORED_FIELDS}:</b> acolo s-ar pierde fapta. „Cine a
     * schimbat datele de identificare ale şoferului şi când" e o întrebare de control, iar
     * răspunsul la ea nu are nevoie de valoare. Un test cere explicit ca fapta să rămână scrisă,
     * ca reparaţia asta să nu fie refăcută prin scoaterea şoferului de pe lista auditată.
     *
     * <p>Un {@code null} rămâne {@code null}: „câmpul era gol" nu e o dată personală, şi e
     * diferenţa dintre o completare şi o corectare.
     */
    private static final Set<String> REDACTED_FIELDS = Set.of("identification", "driverIdentification");

    /** Ce se scrie în locul valorii unui câmp redactat. */
    private static final String REDACTED = "•••";

    /**
     * Nu prinde nimic — înarmează.
     *
     * <p>⚠️ <b>Şi e singurul loc în care se poate face, lucru care a costat o probă.</b> Scrierea
     * rândurilor de jurnal se face dintr-o sincronizare de dinainte de commit, iar sincronizarea
     * aceea trebuie înregistrată <em>cât timp tranzacţia încă rulează</em>. Dacă o înregistrăm abia
     * la prima prindere, merge pentru creări ({@code onSave} se cheamă la {@code persist}, deci
     * devreme) şi pentru orice modificare urmată de o interogare (care forţează un flush) — şi
     * <b>nu merge deloc</b> pentru o tranzacţie care doar schimbă un rând deja încărcat şi se
     * termină: acolo verificarea de „murdărie" se face la commit, adică după ce sincronizările au
     * trecut. Ştergerea moale a unei mişcări e exact aşa, şi nu lăsa nicio urmă.
     *
     * <p>Încărcarea, în schimb, se întâmplă mereu la început şi mereu înăuntru: orice tranzacţie
     * care are ce modifica a citit mai întâi rândul.
     */
    @Override
    public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (isAudited(entity)) {
            writer.getObject().ensureRegistered();
        }
        return false;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (isAudited(entity)) {
            capture(new PendingAudit(typeOf(entity), asUuid(id), AuditAction.CREATE,
                    label(entity, state, propertyNames), List.of()));
        }
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) {
        if (!isAudited(entity) || previousState == null) {
            return false;
        }
        List<PendingAudit.FieldChange> changes = new ArrayList<>();
        for (int i = 0; i < propertyNames.length; i++) {
            if (IGNORED_FIELDS.contains(propertyNames[i]) || types[i] instanceof CollectionType) {
                continue;
            }
            String before = format(previousState[i]);
            String after = format(currentState[i]);
            if (!java.util.Objects.equals(before, after)) {
                boolean redact = REDACTED_FIELDS.contains(propertyNames[i]);
                changes.add(new PendingAudit.FieldChange(propertyNames[i],
                        redact ? redacted(before) : before,
                        redact ? redacted(after) : after));
            }
        }
        if (changes.isEmpty()) {
            return false;
        }
        capture(new PendingAudit(typeOf(entity), asUuid(id), actionOf(changes),
                label(entity, currentState, propertyNames), changes));
        return false;
    }

    @Override
    public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (isAudited(entity)) {
            capture(new PendingAudit(typeOf(entity), asUuid(id), AuditAction.DELETE,
                    label(entity, state, propertyNames), List.of()));
        }
    }

    /** Valoarea unui câmp redactat: gol rămâne gol, orice altceva devine {@value #REDACTED}. */
    private static String redacted(String value) {
        return value == null ? null : REDACTED;
    }

    /** Pune fapta în listă şi se asigură că cineva o va scrie înainte de commit. */
    private void capture(PendingAudit entry) {
        AuditCapture.add(entry);
        writer.getObject().ensureRegistered();
    }

    /**
     * Verbul din spatele modificării.
     *
     * <p>Ştergerea e moale peste tot în aplicaţie şi dezactivarea la fel — amândouă ies din baza de
     * date ca un UPDATE pe un boolean. Scrise aşa, ar fi fost adevărate şi de negăsit: nimeni nu
     * caută în jurnal „deleted: false → true", toată lumea caută „cine a şters".
     */
    private AuditAction actionOf(List<PendingAudit.FieldChange> changes) {
        for (PendingAudit.FieldChange change : changes) {
            if (change.field().equals("deleted") && "true".equals(change.to())) {
                return AuditAction.DELETE;
            }
            if (change.field().equals("active")) {
                return "true".equals(change.to()) ? AuditAction.REACTIVATE : AuditAction.DEACTIVATE;
            }
            if (change.field().equals("enabled") && change.from() != null) {
                return "true".equals(change.to()) ? AuditAction.REACTIVATE : AuditAction.DEACTIVATE;
            }
        }
        return AuditAction.UPDATE;
    }

    private boolean isAudited(Object entity) {
        return entity != null && AUDITED.contains(entity.getClass());
    }

    private String typeOf(Object entity) {
        return entity.getClass().getSimpleName();
    }

    private UUID asUuid(Object id) {
        return id instanceof UUID uuid ? uuid : null;
    }

    private String label(Object entity, Object[] state, String[] propertyNames) {
        List<String> fields = LABEL_FIELDS.get(entity.getClass());
        if (fields == null || state == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String field : fields) {
            for (int i = 0; i < propertyNames.length; i++) {
                if (propertyNames[i].equals(field) && state[i] != null) {
                    String value = String.valueOf(state[i]);
                    if (!value.isBlank()) {
                        parts.add(value);
                    }
                }
            }
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /**
     * Valoarea unui câmp, ca text.
     *
     * <p>Pentru un câmp care trimite la alt rând se scrie <b>identificatorul</b>, luat din proxy
     * fără să-l iniţializeze ({@link HibernateProxy#getHibernateLazyInitializer()}) când e proxy şi
     * din {@code getId()} când e obiectul întreg. Un {@code toString()} pe o entitate ar fi mers
     * uneori şi ar fi aruncat {@code LazyInitializationException} în rest — adică ar fi picat
     * tocmai pe rândurile vechi, la care nimeni nu se uită până la un control.
     */
    private String format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HibernateProxy proxy) {
            return String.valueOf(proxy.getHibernateLazyInitializer().getIdentifier());
        }
        if (value.getClass().isAnnotationPresent(jakarta.persistence.Entity.class)) {
            try {
                return String.valueOf(value.getClass().getMethod("getId").invoke(value));
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return String.valueOf(value);
    }
}
