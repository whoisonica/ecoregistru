package ro.ecoregistru.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WasteCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Global nomenclator — not tenant-scoped. */
public interface WasteCodeRepository extends JpaRepository<WasteCode, UUID> {

    Optional<WasteCode> findByCode(String code);

    List<WasteCode> findAllByOrderByCodeAsc(Limit limit);

    /**
     * @param q the search text already folded with {@link ro.ecoregistru.util.Diacritics#fold},
     *          because {@code searchText} holds the folded form of code and name (V17). Passing
     *          raw user input here would silently stop matching the moment someone types a
     *          diacritic.
     */
    @Query("""
            select w from WasteCode w
            where w.searchText like concat('%', :q, '%')
            order by w.code
            """)
    List<WasteCode> search(@Param("q") String q, Limit limit);
}
