package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.AppUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Enabled users of a company — recipients for that tenant's notifications. */
    List<AppUser> findAllByCompany_IdAndEnabledTrue(UUID companyId);
}
