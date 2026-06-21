CREATE TABLE IF NOT EXISTS tenants (
    id CHAR(36) PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    tax_id VARCHAR(40) UNIQUE,
    country VARCHAR(80),
    city VARCHAR(120),
    address VARCHAR(200),
    description VARCHAR(500),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS tenant_roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_role_permissions (
    role_id CHAR(36) NOT NULL,
    permission VARCHAR(120) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_tenant_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tenant_memberships (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (tenant_id, user_id),
    CONSTRAINT fk_tenant_memberships_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tenant_membership_roles (
    membership_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (membership_id, role_id),
    CONSTRAINT fk_tenant_membership_roles_membership
        FOREIGN KEY (membership_id) REFERENCES tenant_memberships(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_membership_roles_role
        FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_memberships_user_status
    ON tenant_memberships (user_id, status);

INSERT IGNORE INTO tenants (id, slug, name, status)
VALUES ('00000000-0000-0000-0000-000000000010', 'hermes-local', 'Hermes Local Company', 'ACTIVE');

INSERT IGNORE INTO tenant_roles (id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000012', 'TENANT_ADMIN', 'Administrador de la organizacion'),
    ('00000000-0000-0000-0000-000000000013', 'TENANT_PARTNER', 'Colaborador de la organizacion');

INSERT IGNORE INTO tenant_role_permissions (role_id, permission)
VALUES
    ('00000000-0000-0000-0000-000000000012', 'users:manage'),
    ('00000000-0000-0000-0000-000000000012', 'calendar:read'),
    ('00000000-0000-0000-0000-000000000012', 'calendar:write'),
    ('00000000-0000-0000-0000-000000000013', 'calendar:read'),
    ('00000000-0000-0000-0000-000000000013', 'calendar:write');

-- El administrador del sistema (identidad 0..0100) es una cuenta de PLATAFORMA y no
-- pertenece a ningun tenant, por lo que no se siembra ninguna membresia por defecto.
-- El tenant 'hermes-local' y sus roles quedan disponibles para los actores de tenant.
