package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.CardPayment;
import ro.ecoregistru.enums.CardPaymentKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardPaymentRepository extends JpaRepository<CardPayment, UUID> {

    Optional<CardPayment> findByOrderId(String orderId);

    List<CardPayment> findAllByInvoice_IdOrderByCreatedAtDesc(UUID invoiceId);

    long countByInvoice_IdAndKind(UUID invoiceId, CardPaymentKind kind);
}
