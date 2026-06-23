package co.com.hermes.calendar.auth.session;

import co.com.hermes.calendar.auth.identity.HermesPrincipalResolver;
import co.com.hermes.calendar.auth.identity.HermesUserPrincipal;
import co.com.hermes.calendar.auth.identity.IdentityAuthProperties;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Cambia la organización activa del usuario re-emitiendo un token. Valida (re-consultando identity) que
 * la cuenta siga activa y que sea miembro del tenant destino, y firma un JWT nuevo con los claims de esa
 * organización (mismo emisor y clave que los tokens normales, por lo que los resource servers lo aceptan).
 */
@Service
public class TenantSwitchService {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final IdentityAuthProperties properties;
    private final HermesPrincipalResolver principalResolver;
    private final RestClient identityClient;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;

    public TenantSwitchService(
            IdentityAuthProperties properties,
            HermesPrincipalResolver principalResolver,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder,
            JwtEncoder jwtEncoder,
            @Value("${hermes.auth.issuer-uri}") String issuer,
            @Value("${hermes.auth.switch-token-ttl:PT1H}") Duration ttl
    ) {
        this.properties = properties;
        this.principalResolver = principalResolver;
        this.identityClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public SwitchTokenResponse switchTenant(String username, UUID tenantId) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user in token");
        }
        CredentialVerificationResponse user = identityClient.get()
                .uri("/internal/auth/users/{username}", username)
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .retrieve()
                .body(CredentialVerificationResponse.class);

        if (user == null || !user.authenticated() || !user.enabled() || user.locked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }
        if (user.platformAnchored()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Platform accounts cannot switch organizations");
        }

        HermesUserPrincipal principal = principalResolver.resolveForTenant(user, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not an active member of that organization"));

        return mint(principal);
    }

    private SwitchTokenResponse mint(HermesUserPrincipal principal) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(principal.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("user_id", principal.getUserId().toString())
                .claim("preferred_username", principal.getUsername())
                .claim("email", principal.getEmail())
                .claim("account_scope", principal.getScope().name())
                .claim("roles", principal.getRoles())
                .claim("permissions", principal.getPermissions())
                .claim("tenant_id", principal.getTenantId().toString())
                .claim("tenant_slug", principal.getTenantSlug())
                .claim("tenant_name", principal.getTenantName())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new SwitchTokenResponse(
                token, "Bearer", ttl.toSeconds(),
                principal.getTenantId(), principal.getTenantName(), principal.getRoles());
    }
}
