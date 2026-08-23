package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.InternalGenerator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InternalGeneratorRepository extends JpaRepository<InternalGenerator, UUID> {

    List<InternalGenerator> findAllByCompany_IdOrderByNameAsc(UUID companyId);

    List<InternalGenerator> findAllByCompany_IdAndWorkPoint_IdOrderByNameAsc(UUID companyId, UUID workPointId);

    Optional<InternalGenerator> findByIdAndCompany_Id(UUID id, UUID companyId);

    boolean existsByWorkPoint_IdAndNameIgnoreCase(UUID workPointId, String name);
}
