package co.com.hermes.calendar.auth.identity;

import co.com.hermes.calendar.shared.contract.CredentialVerificationRequest;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.contract.TenantContextResponse;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class IdentityAuthenticationProvider implements AuthenticationProvider {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final IdentityAuthProperties properties;
    private final RestClient identityClient;
    private final RestClient tenantClient;

    public IdentityAuthenticationProvider(
            IdentityAuthProperties properties,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.identityClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.tenantClient = restClientBuilder.baseUrl(properties.tenantBaseUrl()).build();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        CredentialVerificationResponse response = identityClient.post()
                .uri("/internal/auth/credentials/verify")
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .body(new CredentialVerificationRequest(username, password))
                .retrieve()
                .body(CredentialVerificationResponse.class);

        if (response == null || !response.authenticated() || !response.enabled() || response.locked()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        TenantContextResponse tenantContext = tenantClient.get()
                .uri("/internal/users/{userId}/tenant-context/default", response.userId())
                .header(INTERNAL_KEY_HEADER, properties.internalApiKey())
                .retrieve()
                .body(TenantContextResponse.class);

        if (tenantContext == null || tenantContext.tenantId() == null) {
            throw new BadCredentialsException("User has no active tenant");
        }

        List<GrantedAuthority> authorities = new ArrayList<>(tenantContext.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));

        HermesUserPrincipal principal = new HermesUserPrincipal(
                response.userId(),
                tenantContext.tenantId(),
                tenantContext.tenantSlug(),
                tenantContext.tenantName(),
                response.username(),
                response.email(),
                tenantContext.roles(),
                tenantContext.permissions(),
                authorities
        );

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
