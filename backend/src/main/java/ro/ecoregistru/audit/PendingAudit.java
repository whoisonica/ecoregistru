package ro.ecoregistru.audit;

import ro.ecoregistru.enums.AuditAction;

import java.util.List;
import java.util.UUID;

/**
 * O faptă prinsă în timpul unui flush, înainte de a fi scrisă ca rând de jurnal.
 *
 * <p>Intermediarul există fiindcă cele două momente nu se pot suprapune: interceptorul vede
 * schimbarea în mijlocul flush-ului, iar scrierea rândului cere un flush al ei — deci prinderea
 * stă într-o listă până înainte de commit. Vezi {@link AuditCapture}.
 *
 * @param field un câmp schimbat; {@code from}/{@code to} sunt deja text, iar câmpurile care trimit
 *              la alt rând poartă identificatorul — numele se rezolvă la citire
 */
public record PendingAudit(String entityType, UUID entityId, AuditAction action, String label,
                           List<PendingAudit.FieldChange> changes) {

    public record FieldChange(String field, String from, String to) {
    }
}
