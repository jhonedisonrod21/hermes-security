package co.com.hermes.calendar.identity.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Registra eventos de auditoría de seguridad (persistencia + log). */
@Service
public class SecurityAuditService {

    /** Tipos de evento auditados. */
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    public enum Outcome { SUCCESS, FAILURE }

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final SecurityAuditRepository repository;

    public SecurityAuditService(SecurityAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Persiste un evento de auditoría en su propia transacción ({@code REQUIRES_NEW}) para que el
     * registro sobreviva aunque la operación auditada termine con error y haga rollback (p. ej. un
     * intento de cambio de contraseña con la actual incorrecta debe quedar igualmente auditado).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String eventType, Outcome outcome, String detail) {
        repository.save(SecurityAuditEvent.of(userId, eventType, outcome.name(), detail));
        log.info("event={} user={} outcome={} detail={}", eventType, userId, outcome, detail == null ? "" : detail);
    }
}
