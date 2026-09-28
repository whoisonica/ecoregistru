package ro.ecoregistru.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WasteCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Global nomenclator — not tenant-scoped. */
public interface WasteCodeRepository extends JpaRepository<WasteCode, UUID> {

    Optional<WasteCode> findByCode(String code);

    /** The codes of the List of Waste in force on {@code on} (V80: validity dates), by code. */
    @Query("""
            select w from WasteCode w
            where (w.validFrom is null or w.validFrom <= :on)
              and (w.validTo is null or w.validTo >= :on)
            order by w.code
            """)
    List<WasteCode> findValidOn(@Param("on") LocalDate on, Limit limit);

    @Query("""
            select w from WasteCode w
            where w.searchText like concat('%', :q, '%')
              and (w.validFrom is null or w.validFrom <= :on)
              and (w.validTo is null or w.validTo >= :on)
            order by w.code
            """)
    List<WasteCode> search(@Param("q") String q, @Param("on") LocalDate on, Limit limit);

    /**
     * Writes the renames and hazard changes whose day has come (V80: 16 06 04 becomes hazardous on
     * 9.11.2026) into {@code name} / {@code hazardous}, so every reader — the aggregate queries too —
     * sees the new list. Idempotent: an applied row has no pending day left.
     */
    @Modifying
    @Query(value = """
            update waste_codes
            set name = coalesce(pending_name, name),
                hazardous = coalesce(pending_hazardous, hazardous),
                pending_from = null, pending_name = null, pending_hazardous = null
            where pending_from <= :today
            """, nativeQuery = true)
    int applyPendingChanges(@Param("today") LocalDate today);

    /**
     * Recomputes every mirror pair with the V37 rule: a non-hazardous code whose official name cites
     * a hazardous code (OUG 92/2021 art. 8 alin. (2)). Run after {@link #applyPendingChanges}, since a
     * code that turns hazardous stops being a mirror.
     */
    @Modifying
    @Query(value = """
            update waste_codes c
            set mirror_of = case when c.hazardous then null else (
                    select string_agg(h.code, ', ' order by h.code)
                    from waste_codes h
                    where h.hazardous and h.code <> c.code and c.name like '%' || h.code || '%')
                end
            """, nativeQuery = true)
    int recomputeMirrors();
}
