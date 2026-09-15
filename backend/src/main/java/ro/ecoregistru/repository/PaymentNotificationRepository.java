package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.PaymentNotification;

import java.util.UUID;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, UUID> {

    boolean existsByNtpIdAndStatus(String ntpId, int status);
}
