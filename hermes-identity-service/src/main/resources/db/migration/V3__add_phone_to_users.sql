-- Teléfono de contacto del usuario (para notificaciones por SMS). Opcional.
ALTER TABLE identity_users ADD COLUMN phone VARCHAR(40) NULL AFTER email;
