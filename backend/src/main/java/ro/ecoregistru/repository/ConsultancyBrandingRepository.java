package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.ecoregistru.entity.ConsultancyBranding;
import ro.ecoregistru.service.export.ReportBranding;

import java.util.Optional;
import java.util.UUID;

public interface ConsultancyBrandingRepository extends JpaRepository<ConsultancyBranding, UUID> {

    /**
     * The branding a company's reports carry: its consultancy's, or none for a direct client. The
     * company decides, not whoever downloads — the company's own admin gets the same page as the consultant.
     */
    @Query("select new ro.ecoregistru.service.export.ReportBranding(cons.name, b.headerLine, b.logo) "
            + "from Company c join c.consultancy cons, ConsultancyBranding b "
            + "where c.id = :companyId and b.consultancyId = cons.id")
    Optional<ReportBranding> findForCompany(UUID companyId);
}
