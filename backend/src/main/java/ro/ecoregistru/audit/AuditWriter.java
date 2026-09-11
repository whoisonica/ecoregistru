package ro.ecoregistru.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.AuditAction;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Scrie faptele prinse de {@link AuditInterceptor}, o dată, chiar înainte de commit.
 *
 * <p><b>De ce „chiar înainte de commit" şi nu „după".</b> Scrise după, rândurile de jurnal ar fi
 * într-o tranzacţie proprie, deci o cădere acolo ar lăsa modificarea în date şi nicio urmă despre
 * ea — exact cazul pentru care cineva deschide jurnalul. Scrise aici, intră în <b>aceeaşi</b>
 * tranzacţie: ori modificarea şi urma ei, ori niciuna. La fel şi invers — dacă tranzacţia dă
 * înapoi, lista se goleşte fără să scrie nimic, fiindcă o modificare revenită nu s-a întâmplat.
 *
 * <p>⚠️ <b>Flush-ul de la începutul lui {@link #writePending()} nu e o precauţie, e condiţia ca
 * metoda asta să vadă ceva.</b> Într-o metodă {@code @Transactional} obişnuită, care schimbă o
 * entitate şi se termină, Hibernate face verificarea de „murdărie" abia la commit — adică
 * <em>după</em> sincronizările de dinainte de commit. Fără flush-ul ăsta, lista ar fi goală la
 * fiecare modificare făcută fără un {@code save()} explicit, iar jurnalul ar prinde numai creările.
 *
 * <p><b>Ce nu se scrie niciodată: fapte fără un om.</b> Planificatorul de termene umblă peste toţi
 * tenanţii, fără sesiune şi fără firmă curentă; un jurnal care ar răspunde „nimeni" de o mie de ori
 * pe zi ar îneca rândurile pentru care există.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriter {

    private static final ThreadLocal<Boolean> REGISTERED = new ThreadLocal<>();

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * O faptă care nu e o scriere de rând, spusă pe faţă.
     *
     * <p>Singura de azi e regenerarea evidenţei: o apăsare de buton care rescrie mii de rânduri
     * dintr-un cache. Auditate rând cu rând, ar fi mii de intrări despre un singur gest; aici e una
     * singură, cu anul şi cu câte linii au ieşit.
     */
    public void record(String entityType, UUID entityId, AuditAction action, String label) {
        AuditCapture.add(new PendingAudit(entityType, entityId, action, label, List.of()));
        ensureRegistered();
    }

    /** Cheamă-mă după ce ai pus ceva în listă: se înregistrează o singură dată pe tranzacţie. */
    public void ensureRegistered() {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || Boolean.TRUE.equals(REGISTERED.get())) {
            return;
        }
        REGISTERED.set(true);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                if (!readOnly) {
                    writePending();
                }
            }

            @Override
            public void afterCompletion(int status) {
                REGISTERED.remove();
                AuditCapture.clear();
            }
        });
    }

    void writePending() {
        entityManager.flush();
        List<PendingAudit> pending = AuditCapture.drain();
        if (pending.isEmpty()) {
            return;
        }
        UUID tenantId = TenantContext.get();
        AppUser actor = currentUserOrNull();
        if (tenantId == null || actor == null) {
            return;
        }
        Company company = entityManager.getReference(Company.class, tenantId);
        Instant now = Instant.now();
        List<AuditLog> rows = pending.stream()
                .map(entry -> AuditLog.builder()
                        .company(company)
                        .entityType(entry.entityType())
                        .entityId(entry.entityId())
                        .action(entry.action())
                        .label(entry.label())
                        .changes(AuditChangeCodec.write(entry.changes()))
                        .actorId(actor.getId())
                        .actorEmail(actor.getEmail())
                        .actorRole(actor.getRole())
                        .occurredAt(now)
                        .build())
                .toList();
        auditLogRepository.saveAll(rows);
    }

    /**
     * Utilizatorul curent, sau null.
     *
     * <p>Nu {@code SecurityUtils.currentUser()}, care aruncă: aici lipsa lui e un răspuns normal —
     * o scriere de sistem —, nu o eroare de spus cuiva.
     */
    private AppUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AppUser user ? user : null;
    }
}
