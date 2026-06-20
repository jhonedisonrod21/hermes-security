CREATE TABLE IF NOT EXISTS auth_schema_markers (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT IGNORE INTO auth_schema_markers (id, name)
VALUES ('00000000-0000-0000-0000-000000000200', 'auth-server-initialized');
