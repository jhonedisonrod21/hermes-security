package co.com.hermes.calendar.tenant.tenant;

/**
 * Estado operativo de un establecimiento (tenant).
 *
 * <ul>
 *   <li>{@link #ACTIVE}: operativo; sus membresías otorgan contexto de tenant.</li>
 *   <li>{@link #INACTIVE}: deshabilitado por el administrador del sistema; sus usuarios pierden el
 *       contexto de tenant en la siguiente emisión de token.</li>
 * </ul>
 */
public enum TenantStatus {
    ACTIVE,
    INACTIVE
}
