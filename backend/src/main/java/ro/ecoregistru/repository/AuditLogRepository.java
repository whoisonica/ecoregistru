package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ro.ecoregistru.entity.AuditLog;

import java.util.UUID;

/**
 * Jurnalul de audit. Numai scriere şi citire — niciun drum de modificare sau ştergere, fiindcă un
 * jurnal care se poate rescrie nu răspunde la întrebarea pentru care există.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>,
        JpaSpecificationExecutor<AuditLog> {
}
