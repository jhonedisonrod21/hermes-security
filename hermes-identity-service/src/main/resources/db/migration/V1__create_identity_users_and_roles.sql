CREATE TABLE IF NOT EXISTS identity_roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS identity_users (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36),
    username VARCHAR(160) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS identity_user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_identity_user_roles_user
        FOREIGN KEY (user_id) REFERENCES identity_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_user_roles_role
        FOREIGN KEY (role_id) REFERENCES identity_roles(id) ON DELETE CASCADE
);

INSERT IGNORE INTO identity_roles (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Hermes platform administrator');

INSERT IGNORE INTO identity_users (id, tenant_id, username, email, password_hash, enabled, locked)
VALUES (
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000010',
    'admin@hermes.local',
    'admin@hermes.local',
    '{noop}admin123',
    TRUE,
    FALSE
);

INSERT IGNORE INTO identity_user_roles (user_id, role_id)
VALUES ('00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000001');
