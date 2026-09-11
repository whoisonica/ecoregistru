package ro.ecoregistru.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.audit.AuditChangeCodec;
import ro.ecoregistru.audit.PendingAudit;
import ro.ecoregistru.controller.response.AuditLogResponse;
import ro.ecoregistru.controller.response.PageResponse;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Citirea jurnalului de audit — P1.11.
 *
 * <p>Scrierea nu trece pe aici: ea se face singură, din interceptorul de Hibernate
 * ({@code ro.ecoregistru.audit}). Serviciul ăsta e numai partea de citit, şi are o singură sarcină
 * care nu e evidentă: <b>să pună nume peste identificatori</b>.
 *
 * <p>Jurnalul scrie „partener: {@code 3f2a…} → {@code 91bc…}", fiindcă în mijlocul unui flush
 * numele nu se poate citi fără să iniţializezi un proxy. Aici, unde avem o pagină întreagă în faţă
 * şi nicio tranzacţie de scriere în desfăşurare, cei douăzeci şi cinci de identificatori se ridică
 * dintr-o interogare per tabelă şi rândul ajunge să scrie „partener: Hamburger → Eco Valorificare".
 * Asta e diferenţa dintre un jurnal pe care îl citeşte cineva şi unul pe care nu-l citeşte nimeni.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 25;

    AuditLogRepository auditLogRepository;
    WasteCodeRepository wasteCodeRepository;
    PartnerRepository partnerRepository;
    WorkPointRepository workPointRepository;
    InternalGeneratorRepository internalGeneratorRepository;
    PartnerWorkPointRepository partnerWorkPointRepository;
    CompanyRepository companyRepository;

    /**
     * O pagină de jurnal, cea mai nouă întâi.
     *
     * @param entityType „WasteMovement", „Partner"… — filtrul din capul ecranului
     * @param entityId   drumul invers: tot ce s-a întâmplat cu <b>un</b> rând, adică exact
     *                   „cine a modificat cantitatea asta şi când"
     * @param search     acelaşi fel de căutare ca pe restul tabelelor (vezi {@code FoldedSearch}):
     *                   fără diacritice, expresia înaintea cuvintelor
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(String entityType, UUID entityId, String search,
                                               int page, int size) {
        UUID tenantId = TenantContext.require();
        Specification<AuditLog> spec = ordered(withSearch(filter(tenantId, entityType, entityId), search));
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));

        var rows = auditLogRepository.findAll(spec, pageable);
        Map<UUID, String> names = resolveNames(rows.getContent());
        return PageResponse.of(rows, row -> toResponse(row, names));
    }

    private Specification<AuditLog> filter(UUID tenantId, String entityType, UUID entityId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), tenantId));
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Aceleaşi reguli de căutare ca pe Mişcări, din acelaşi motiv: o casetă care caută altfel pe
     * fiecare ecran e o casetă în care omul nu are încredere.
     */
    private Specification<AuditLog> withSearch(Specification<AuditLog> filter, String search) {
        String folded = FoldedSearch.fold(search == null ? "" : search.trim());
        if (folded.isEmpty()) {
            return filter;
        }
        Specification<AuditLog> phrase = filter.and((root, query, cb) ->
                FoldedSearch.phrase(cb, haystack(root, cb), folded));
        if (auditLogRepository.count(phrase) > 0) {
            return phrase;
        }
        List<String> tokens = FoldedSearch.tokenize(folded);
        return tokens.isEmpty() ? phrase
                : filter.and((root, query, cb) -> FoldedSearch.words(cb, haystack(root, cb), tokens));
    }

    /**
     * Textul în care se caută: eticheta rândului, autorul, tipul şi fapta.
     *
     * <p>{@code changes} intră şi el, fiindcă „cine a atins cantitatea" e o întrebare pe care o
     * pune cineva cu cuvântul „cantitate" în minte, nu cu numele mişcării.
     */
    private Expression<String> haystack(Root<AuditLog> root, CriteriaBuilder cb) {
        return FoldedSearch.haystack(cb,
                root.get("label"),
                root.get("actorEmail"),
                root.get("entityType"),
                root.get("action").as(String.class),
                root.get("changes"));
    }

    /**
     * Cele mai noi întâi, iar {@code id} închide ordinea.
     *
     * <p>Acelaşi motiv ca la mişcări, dar aici e mai ascuţit: toate faptele unei cereri poartă
     * <b>aceeaşi</b> clipă, până la milisecundă, fiindcă se scriu împreună înainte de commit. Fără
     * o a doua coloană, o pagină de jurnal ar putea arăta de două ori acelaşi rând şi niciodată pe
     * altul.
     */
    private Specification<AuditLog> ordered(Specification<AuditLog> spec) {
        return (root, query, cb) -> {
            Predicate where = spec.toPredicate(root, query, cb);
            query.orderBy(cb.desc(root.get("occurredAt")), cb.desc(root.get("id")));
            return where;
        };
    }

    private int clampSize(int size) {
        return size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    private AuditLogResponse toResponse(AuditLog row, Map<UUID, String> names) {
        List<AuditLogResponse.Change> changes = AuditChangeCodec.read(row.getChanges()).stream()
                .map(change -> new AuditLogResponse.Change(change.field(),
                        display(change.field(), change.from(), names),
                        display(change.field(), change.to(), names)))
                .toList();
        return new AuditLogResponse(row.getId(), row.getEntityType(), row.getEntityId(), row.getAction(),
                row.getLabel(), changes, row.getActorId(), row.getActorEmail(), row.getActorRole(),
                row.getOccurredAt());
    }

    /**
     * Câmpurile care poartă un identificator, şi de unde se ia numele fiecăruia.
     *
     * <p>Numele câmpurilor sunt cele ale entităţilor, fiindcă aşa le-a scris interceptorul. Ce nu e
     * aici rămâne aşa cum a fost scris — o cantitate, o dată, un cod R/D nu au ce rezolva.
     */
    private static final Map<String, String> ID_FIELDS = Map.of(
            "wasteCode", "wasteCode",
            "partner", "partner",
            "transportPartner", "partner",
            "workPoint", "workPoint",
            "internalGenerator", "internalGenerator",
            "partnerWorkPoint", "partnerWorkPoint",
            "company", "company");

    private Map<UUID, String> resolveNames(List<AuditLog> rows) {
        Map<String, Set<UUID>> idsByKind = new HashMap<>();
        for (AuditLog row : rows) {
            for (PendingAudit.FieldChange change : AuditChangeCodec.read(row.getChanges())) {
                String kind = ID_FIELDS.get(change.field());
                if (kind == null) {
                    continue;
                }
                addUuid(idsByKind, kind, change.from());
                addUuid(idsByKind, kind, change.to());
            }
        }
        Map<UUID, String> names = new HashMap<>();
        lookup(names, idsByKind.get("wasteCode"), wasteCodeRepository::findAllById,
                c -> c.getId(), c -> c.getCode());
        lookup(names, idsByKind.get("partner"), partnerRepository::findAllById,
                p -> p.getId(), p -> p.getName());
        lookup(names, idsByKind.get("workPoint"), workPointRepository::findAllById,
                w -> w.getId(), w -> w.getName());
        lookup(names, idsByKind.get("internalGenerator"), internalGeneratorRepository::findAllById,
                g -> g.getId(), g -> g.getName());
        lookup(names, idsByKind.get("partnerWorkPoint"), partnerWorkPointRepository::findAllById,
                w -> w.getId(), w -> w.getName());
        lookup(names, idsByKind.get("company"), companyRepository::findAllById,
                c -> c.getId(), c -> c.getName());
        return names;
    }

    private void addUuid(Map<String, Set<UUID>> target, String kind, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            target.computeIfAbsent(kind, k -> new HashSet<>()).add(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            // Nu e un identificator — un câmp care s-a mutat de la o asociere la un text, cândva.
            // Se afişează aşa cum a fost scris; alternativa ar fi să nu se afişeze deloc.
        }
    }

    /** O interogare per tabelă, nu una per rând: o pagină de jurnal costă cel mult şase. */
    private <T> void lookup(Map<UUID, String> names, Set<UUID> ids,
                            Function<Iterable<UUID>, List<T>> finder,
                            Function<T, UUID> idOf, Function<T, String> nameOf) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (T entity : finder.apply(ids)) {
            names.put(idOf.apply(entity), nameOf.apply(entity));
        }
    }

    /**
     * Valoarea aşa cum o vede omul. Un identificator care nu se mai găseşte — rândul a fost şters
     * între timp — rămâne identificator: „nu mai există" e un răspuns mai bun decât un gol.
     */
    private String display(String field, String raw, Map<UUID, String> names) {
        if (raw == null || !ID_FIELDS.containsKey(field)) {
            return raw;
        }
        try {
            return names.getOrDefault(UUID.fromString(raw), raw);
        } catch (IllegalArgumentException e) {
            return raw;
        }
    }
}
