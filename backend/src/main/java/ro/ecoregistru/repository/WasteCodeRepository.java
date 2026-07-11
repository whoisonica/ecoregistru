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

    @Query("""
            select w from WasteCode w
            where lower(w.code) like lower(concat('%', :q, '%'))
               or lower(w.name) like lower(concat('%', :q, '%'))
            order by w.code
            """)
    List<WasteCode> search(@Param("q") String q, Limit limit);
}
