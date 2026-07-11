package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.VerificationRecord;
import ro.ecoregistru.enums.VerificationRecordType;

import java.util.Optional;
import java.util.UUID;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, UUID> {

    Optional<VerificationRecord> findByCodeAndVerificationRecordType(String code, VerificationRecordType type);

    void deleteByUserAndVerificationRecordTypeAndConfirmedFalse(AppUser user, VerificationRecordType type);
}
