CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    client_secret VARCHAR(200),
    client_secret_expires_at DATETIME(6),
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

CREATE UNIQUE INDEX ux_oauth2_registered_client_client_id
    ON oauth2_registered_client (client_id);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id VARCHAR(100) PRIMARY KEY,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000),
    attributes MEDIUMTEXT,
    state VARCHAR(500),
    authorization_code_value MEDIUMTEXT,
    authorization_code_issued_at DATETIME(6),
    authorization_code_expires_at DATETIME(6),
    authorization_code_metadata MEDIUMTEXT,
    access_token_value MEDIUMTEXT,
    access_token_issued_at DATETIME(6),
    access_token_expires_at DATETIME(6),
    access_token_metadata MEDIUMTEXT,
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(1000),
    oidc_id_token_value MEDIUMTEXT,
    oidc_id_token_issued_at DATETIME(6),
    oidc_id_token_expires_at DATETIME(6),
    oidc_id_token_metadata MEDIUMTEXT,
    refresh_token_value MEDIUMTEXT,
    refresh_token_issued_at DATETIME(6),
    refresh_token_expires_at DATETIME(6),
    refresh_token_metadata MEDIUMTEXT,
    user_code_value MEDIUMTEXT,
    user_code_issued_at DATETIME(6),
    user_code_expires_at DATETIME(6),
    user_code_metadata MEDIUMTEXT,
    device_code_value MEDIUMTEXT,
    device_code_issued_at DATETIME(6),
    device_code_expires_at DATETIME(6),
    device_code_metadata MEDIUMTEXT
);

CREATE INDEX idx_oauth2_authorization_registered_client_id
    ON oauth2_authorization (registered_client_id);

CREATE INDEX idx_oauth2_authorization_principal_name
    ON oauth2_authorization (principal_name);

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
