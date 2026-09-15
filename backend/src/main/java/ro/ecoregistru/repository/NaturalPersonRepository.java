package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.NaturalPerson;

import java.util.Optional;
import java.util.UUID;

public interface NaturalPersonRepository extends JpaRepository<NaturalPerson, UUID> {

    Optional<NaturalPerson> findByIdAndCompany_Id(UUID id, UUID companyId);
}
