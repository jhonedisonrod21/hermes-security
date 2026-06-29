-- Nombre visible del usuario (capturado en el registro). Es independiente del `username` (handle de
-- login derivado del correo). Nullable: las cuentas anteriores quedan sin nombre y la UI usa un fallback.
ALTER TABLE identity_users ADD COLUMN name VARCHAR(120) NULL AFTER email;
