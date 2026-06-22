-- Tokens de restablecimiento de contraseña. Se guarda solo el HASH (SHA-256) del token, nunca el valor
-- en claro: aunque se filtre la tabla, los tokens no son utilizables. Un solo uso (used_at) y con caducidad.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_prt_token_hash (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES identity_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);
