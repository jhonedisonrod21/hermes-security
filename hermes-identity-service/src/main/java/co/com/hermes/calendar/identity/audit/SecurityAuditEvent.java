package co.com.hermes.calendar.identity.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Evento de auditoría de seguridad de Identity (cambio de contraseña, etc.). Inmutable. */
@Entity
@Table(name = "identity_security_audit")
public class SecurityAuditEvent {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(length = 255)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected SecurityAuditEvent() {
    }

    public static SecurityAuditEvent of(UUID userId, String eventType, String outcome, String detail) {
        SecurityAuditEvent event = new SecurityAuditEvent();
        event.id = UUID.randomUUID();
        event.userId = userId;
        event.eventType = eventType;
        event.outcome = outcome;
        event.detail = detail;
        event.occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        return event;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
