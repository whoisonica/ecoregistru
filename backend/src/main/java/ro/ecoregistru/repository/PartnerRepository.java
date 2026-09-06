package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Partner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findAllByCompany_Id(UUID companyId);

    Optional<Partner> findByIdAndCompany_Id(UUID id, UUID companyId);

    /** Partners whose authorization expires on/before the cutoff (for expiry alerts). */
    List<Partner> findAllByActiveTrueAndAuthorizationExpiryNotNullAndAuthorizationExpiryLessThanEqual(LocalDate cutoff);

    /**
     * Candidates for the 60-day authorization warning: active partners whose authorization expires
     * inside the window ahead. Across all tenants, because the scheduler is a system job with no
     * {@code TenantContext} — the same shape as the deadline reminders.
     *
     * <p>The window starts at {@code from} = today, so an authorization that lapsed in the past is
     * deliberately <em>not</em> a candidate: a warning about it prevents nothing, and it is already
     * visible in two other places (the partner-list badge and the handover warning of decision 36).
     * See V30 for the full reasoning.
     */
    List<Partner> findAllByActiveTrueAndAuthorizationExpiryBetween(LocalDate from, LocalDate to);
}
