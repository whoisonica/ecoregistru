package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.AuditAction;
import ro.ecoregistru.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Un rând de jurnal: cine, ce, când — P1.11.
 *
 * <p>Scris o singură dată şi niciodată modificat. Nu are nici {@code updatedAt}, nici vreun drum
 * de editare: un jurnal care se poate rescrie nu răspunde la întrebarea pentru care există.
 *
 * <p><b>Autorul e copiat, nu referit.</b> {@link #actorEmail} şi {@link #actorRole} sunt
 * instantanee, fiindcă un cont se dezactivează, se redenumeşte şi îşi schimbă rolul — iar la un
 * control contează cine a fost atunci, nu cine e acum. {@link #actorId} rămâne alături pentru
 * cazul în care contul chiar există şi vrei să ajungi la el.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_log")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    /** Numele simplu al clasei — „WasteMovement" —, nu numele tabelei: jurnalul se citeşte lângă cod. */
    @Column(name = "entity_type", nullable = false, length = 60)
    String entityType;

    /** Rândul atins. Null doar pentru faptele care nu sunt despre un rând anume (regenerarea). */
    @Column(name = "entity_id")
    UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AuditAction action;

    /**
     * Cum se numea rândul în clipa faptei. Scris atunci, nu rezolvat la citire — un rând şters nu
     * mai poate fi întrebat cum îl chema, iar ştergerea e fapta despre care se întreabă cel mai des.
     */
    @Column(columnDefinition = "text")
    String label;

    /** JSON: {@code [{"field":…,"from":…,"to":…}]}. Gol la creare şi la ştergere. */
    @Column(columnDefinition = "text")
    String changes;

    @Column(name = "actor_id")
    UUID actorId;

    @Column(name = "actor_email", length = 200)
    String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 20)
    Role actorRole;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;
}
