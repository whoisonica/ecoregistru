package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.AuthorizedLimit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizedLimitRepository extends JpaRepository<AuthorizedLimit, UUID> {

    List<AuthorizedLimit> findAllByCompanyIdAndWorkPoint_IdOrderByCreatedAtAsc(UUID companyId, UUID workPointId);

    Optional<AuthorizedLimit> findByIdAndCompanyId(UUID id, UUID companyId);
}
