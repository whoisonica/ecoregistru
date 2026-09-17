package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /** P2.13 — the consultants of a consultancy, whatever their state. */
    List<AppUser> findAllByConsultancy_Id(UUID consultancyId);

    /** P2.13 — one colleague; a user of another consultancy comes back empty, hence 404. */
    Optional<AppUser> findByIdAndConsultancy_Id(UUID id, UUID consultancyId);

    long countByConsultancy_Id(UUID consultancyId);

    /**
     * F-B — utilizatorii fiecărei firme, pentru tabelul Clienți: {@code [companyId, count]}. Invitațiile încă
     * nefolosite se numără (omul e pe drum), cei dezactivați nu.
     */
    @Query("select u.company.id, count(u) from AppUser u where u.company is not null and u.deactivatedAt is null"
            + " group by u.company.id")
    List<Object[]> countMembersByCompany();

    /** P2.13, felia 2 — consultanții care pot citi rezumatul zilnic; invitațiile nefolosite nu. */
    List<AppUser> findAllByConsultancy_IdAndEnabledTrue(UUID consultancyId);

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
