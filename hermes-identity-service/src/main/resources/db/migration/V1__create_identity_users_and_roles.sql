CREATE TABLE IF NOT EXISTS identity_roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    scope VARCHAR(30) NOT NULL DEFAULT 'TENANT'
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

-- Permisos efectivos por rol global (los roles PLATFORM entregan permisos de plataforma).
CREATE TABLE IF NOT EXISTS identity_role_permissions (
    role_id CHAR(36) NOT NULL,
    permission VARCHAR(120) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_identity_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES identity_roles(id) ON DELETE CASCADE
);

-- Rol de administrador del sistema: alcance de plataforma, no pertenece a ningun tenant.
INSERT IGNORE INTO identity_roles (id, name, description, scope)
VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN', 'Administrador del sistema (plataforma)', 'PLATFORM');

INSERT IGNORE INTO identity_role_permissions (role_id, permission)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'platform:admin'),
    ('00000000-0000-0000-0000-000000000001', 'platform:tenants:read'),
    ('00000000-0000-0000-0000-000000000001', 'platform:tenants:manage'),
    ('00000000-0000-0000-0000-000000000001', 'platform:users:read'),
    ('00000000-0000-0000-0000-000000000001', 'platform:users:manage'),
    ('00000000-0000-0000-0000-000000000001', 'platform:roles:manage');

-- NOTA: las migraciones NO siembran credenciales. El usuario administrador del sistema se crea:
--   * en LOCAL, por LocalAdminSeeder (@Profile("local")) con una credencial de desarrollo;
--   * en dev/prod, mediante un proceso de alta controlado fuera de banda (password con BCrypt).
-- Así una migración versionada nunca introduce una cuenta con password conocido en producción.
