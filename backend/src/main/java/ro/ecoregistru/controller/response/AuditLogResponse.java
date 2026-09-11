package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.AuditAction;
import ro.ecoregistru.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Un rând de jurnal, gata de citit: cine, ce, când — şi ce anume s-a schimbat.
 *
 * @param label    numele rândului în clipa faptei, aşa cum a fost scris atunci
 * @param changes  câmpurile schimbate, cu valorile <b>rezolvate</b>: acolo unde jurnalul a păstrat
 *                 un identificator (partenerul, codul de deşeu), aici e numele
 * @param actorEmail adresa autorului de atunci, nu de acum — coloana e un instantaneu
 */
public record AuditLogResponse(UUID id,
                               String entityType,
                               UUID entityId,
                               AuditAction action,
                               String label,
                               List<Change> changes,
                               UUID actorId,
                               String actorEmail,
                               Role actorRole,
                               Instant occurredAt) {

    /** O rubrică schimbată. {@code from} lipseşte la câmpurile care erau goale. */
    public record Change(String field, String from, String to) {
    }
}
