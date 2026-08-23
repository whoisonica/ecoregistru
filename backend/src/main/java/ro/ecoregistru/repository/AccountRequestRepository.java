package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.AccountRequest;
import ro.ecoregistru.enums.AccountRequestStatus;

import java.util.List;
import java.util.UUID;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {

    List<AccountRequest> findAllByOrderByCreatedAtDesc();

    List<AccountRequest> findAllByStatusOrderByCreatedAtDesc(AccountRequestStatus status);

    long countByStatus(AccountRequestStatus status);
}
