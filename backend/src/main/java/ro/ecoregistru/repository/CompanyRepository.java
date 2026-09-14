package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByCui(String cui);

    boolean existsByCui(String cui);

    List<Company> findAllByActiveTrue();

    /** P2.13 — may this consultancy's consultants select this tenant? Asked on every request. */
    boolean existsByIdAndConsultancy_Id(UUID id, UUID consultancyId);

    /** P2.13 — the portfolio: the tenant switcher and the clients screen of a consultant. */
    List<Company> findAllByConsultancy_Id(UUID consultancyId);

    /** P2.13 — one company of the portfolio; another consultancy's id comes back empty (404). */
    Optional<Company> findByIdAndConsultancy_Id(UUID id, UUID consultancyId);

    /** P2.13 — managed companies; the figure the contract bills on. */
    long countByConsultancy_Id(UUID consultancyId);
}
