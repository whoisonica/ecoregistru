package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByCui(String cui);

    boolean existsByCui(String cui);

    /**
     * Aceeași firmă, scrisă cu sau fără „RO” (scanarea din 17.09.2026: „RO51779887” și „51779887” intrau amândouă,
     * fiindcă unicitatea din V1 e pe text). {@code digits} = {@code Cui.digits(...)}.
     */
    @Query("select count(c) > 0 from Company c where upper(c.cui) = :digits or upper(c.cui) = concat('RO', :digits)")
    boolean existsByCuiDigits(@Param("digits") String digits);

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
