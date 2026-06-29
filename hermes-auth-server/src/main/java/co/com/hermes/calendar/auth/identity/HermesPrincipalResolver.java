package co.com.hermes.calendar.auth.identity;

import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.contract.TenantContextResponse;
import co.com.hermes.calendar.shared.security.AccountScope;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve el {@link HermesUserPrincipal} a partir del perfil verificado en Identity.
 * Concentra la decisión de alcance que comparten el {@link IdentityAuthenticationProvider}
 * (login con password) y el customizer de claims.
 *
 * <p>El alcance se decide por <b>membresía</b>:</p>
 * <ul>
 *   <li>Cuenta anclada a plataforma ({@code platformAnchored}, p. ej. SYSTEM_ADMIN): siempre
 *       {@link AccountScope#PLATFORM}; no se consulta tenant.</li>
 *   <li>Con membresía activa: {@link AccountScope#TENANT}, con tenant, roles y permisos de la
 *       membresía.</li>
 *   <li>Sin membresía activa: {@link AccountScope#PLATFORM} como invitado, con los roles/permisos
 *       de Identity (p. ej. GUEST_USER).</li>
 * </ul>
 */
@Component
public class HermesPrincipalResolver {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;
    private static final int NOT_FOUND = 404;

    private final IdentityAuthProperties properties;
    private final RestClient tenantClient;

    public HermesPrincipalResolver(
            IdentityAuthProperties properties,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.tenantClient = restClientBuilder.baseUrl(properties.tenantBaseUrl()).build();
    }

    /**
     * Construye el principal del usuario verificado, o {@link Optional#empty()} si la cuenta no es
     * válida (no autenticada / deshabilitada / bloqueada). Una cuenta válida siempre resuelve a un
     * principal: de tenant si tiene membresía activa, o de plataforma (invitado) si no.
     */
    public Optional<HermesUserPrincipal> resolve(CredentialVerificationResponse user) {
        if (user == null || !user.authenticated() || !user.enabled() || user.locked()) {
            return Optional.empty();
        }

        if (user.platformAnchored()) {
            return Optional.of(platformPrincipal(user));
        }

        return Optional.of(activeMembership(user.userId())
                .map(context -> tenantPrincipal(user, context))
                .orElseGet(() -> platformPrincipal(user)));
    }

    /**
     * Construye el principal del usuario para un tenant <b>concreto</b> (cambio de organización activa).
     * Vacío si el usuario no es miembro activo de ese tenant. El llamante valida aparte que la cuenta
     * no esté anclada a plataforma.
     */
    public Optional<HermesUserPrincipal> resolveForTenant(CredentialVerificationResponse user, UUID tenantId) {
        if (user == null || !user.authenticated() || !user.enabled() || user.locked()) {
            return Optional.empty();
        }
        return membershipInTenant(user.userId(), tenantId).map(context -> tenantPrincipal(user, context));
    }

    /** Contexto del usuario en un tenant concreto, o vacío si no es miembro activo (404). */
    private Optional<TenantContextResponse> membershipInTenant(UUID userId, UUID tenantId) {
        TenantContextResponse context = tenantClient.get()
                .uri("/internal/users/{userId}/tenant-context/{tenantId}", userId, tenantId)
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .retrieve()
                .onStatus(status -> status.value() == NOT_FOUND, (request, response) -> { })
                .body(TenantContextResponse.class);
        return context == null || context.tenantId() == null ? Optional.empty() : Optional.of(context);
    }

    /** Membresía/tenant por defecto del usuario, o vacío si no tiene ninguna activa (404). */
    private Optional<TenantContextResponse> activeMembership(UUID userId) {
        TenantContextResponse context = tenantClient.get()
                .uri("/internal/users/{userId}/tenant-context/default", userId)
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .retrieve()
                // "Sin tenant activo" es un caso normal (invitado), no un error: el tenant-service
                // responde 404 y aquí lo tratamos como ausencia de membresía.
                .onStatus(status -> status.value() == NOT_FOUND, (request, response) -> { })
                .body(TenantContextResponse.class);

        return context == null || context.tenantId() == null ? Optional.empty() : Optional.of(context);
    }

    private HermesUserPrincipal platformPrincipal(CredentialVerificationResponse user) {
        return new HermesUserPrincipal(
                user.userId(),
                AccountScope.PLATFORM,
                new HermesUserPrincipal.TenantRef(null, null, null),
                new HermesUserPrincipal.UserProfile(user.username(), user.email(), user.name()),
                user.roles(),
                user.permissions(),
                roleAuthorities(user.roles())
        );
    }

    private HermesUserPrincipal tenantPrincipal(CredentialVerificationResponse user, TenantContextResponse context) {
        return new HermesUserPrincipal(
                user.userId(),
                AccountScope.TENANT,
                new HermesUserPrincipal.TenantRef(context.tenantId(), context.tenantSlug(), context.tenantName()),
                new HermesUserPrincipal.UserProfile(user.username(), user.email(), user.name()),
                context.roles(),
                context.permissions(),
                roleAuthorities(context.roles())
        );
    }

    private static List<GrantedAuthority> roleAuthorities(List<String> roles) {
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
