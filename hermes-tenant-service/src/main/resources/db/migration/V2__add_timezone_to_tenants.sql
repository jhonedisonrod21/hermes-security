-- Zona horaria IANA del establecimiento (p. ej. America/Bogota). Necesaria para calcular la
-- disponibilidad y programar los recordatorios en la hora local del tenant.
ALTER TABLE tenants ADD COLUMN time_zone VARCHAR(60) AFTER city;
