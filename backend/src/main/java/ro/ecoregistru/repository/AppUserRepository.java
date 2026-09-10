package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Enabled users of a company — recipients for that tenant's notifications. */
    List<AppUser> findAllByCompany_IdAndEnabledTrue(UUID companyId);

    /** P1.12 — every member of a tenant, whatever their state. Drives the users screen. */
    List<AppUser> findAllByCompany_Id(UUID companyId);

    /**
     * P1.12 — one member of a tenant. Scoped by company on purpose: an id from another tenant
     * comes back empty and the service turns that into 404, so the endpoint never confirms that
     * a user id exists somewhere else.
     */
    Optional<AppUser> findByIdAndCompany_Id(UUID id, UUID companyId);

    /**
     * P1.12 — how many admins the tenant still has who can actually sign in. Guards the two ways
     * a firm can lock itself out of its own account: switching off the last admin, or demoting
     * them. Counts only enabled ones — an invited admin who never set a password cannot rescue
     * anybody.
     */
    long countByCompany_IdAndRoleAndEnabledTrue(UUID companyId, Role role);
}
