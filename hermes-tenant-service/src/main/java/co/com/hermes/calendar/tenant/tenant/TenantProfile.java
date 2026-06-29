package co.com.hermes.calendar.tenant.tenant;

/**
 * Datos de negocio de un establecimiento que se fijan en bloque al darlo de alta o actualizarlo.
 * Agrupa los campos de contacto/ubicación para que las operaciones de {@link Tenant} no arrastren
 * listas largas de parámetros. Cualquier campo puede ser {@code null} (todos son opcionales salvo
 * los que valide la capa de aplicación).
 */
public record TenantProfile(
        String taxId,
        String country,
        String city,
        String address,
        String description,
        String timeZone,
        GeoLocation location
) {
}
