package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Consultancy;

import java.util.UUID;

public interface ConsultancyRepository extends JpaRepository<Consultancy, UUID> {
    boolean existsByCui(String cui);
}
