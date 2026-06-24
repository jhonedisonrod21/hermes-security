package co.com.hermes.calendar.identity.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityAuditRepository extends JpaRepository<SecurityAuditEvent, UUID> {
}
