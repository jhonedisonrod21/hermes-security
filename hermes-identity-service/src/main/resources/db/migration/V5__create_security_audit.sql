-- Auditoría de eventos de seguridad de Identity (p. ej. cambio de contraseña). Registro inmutable de
-- "quién hizo qué y cuándo" con su resultado, para trazabilidad. No guarda secretos (ni contraseñas).
CREATE TABLE IF NOT EXISTS identity_security_audit (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36),
    event_type VARCHAR(60) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    detail VARCHAR(255),
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_audit_user ON identity_security_audit (user_id);
CREATE INDEX idx_audit_event ON identity_security_audit (event_type);
