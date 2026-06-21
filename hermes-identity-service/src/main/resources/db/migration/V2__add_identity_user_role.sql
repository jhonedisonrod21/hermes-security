-- Rol del usuario invitado autorregistrado. No está anclado a plataforma (scope TENANT): su
-- alcance efectivo lo decide la membresía: PLATFORM/invitado mientras no tenga organización,
-- TENANT en cuanto el administrador del sistema lo dé de alta en un tenant.
INSERT IGNORE INTO identity_roles (id, name, description, scope)
VALUES ('00000000-0000-0000-0000-000000000002', 'GUEST_USER', 'Usuario invitado sin organizacion', 'TENANT');
