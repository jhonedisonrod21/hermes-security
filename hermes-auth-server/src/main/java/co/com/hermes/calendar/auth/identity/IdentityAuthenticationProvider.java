package co.com.hermes.calendar.auth.identity;

import co.com.hermes.calendar.shared.contract.CredentialVerificationRequest;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class IdentityAuthenticationProvider implements AuthenticationProvider {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final IdentityAuthProperties properties;
    private final RestClient identityClient;
    private final HermesPrincipalResolver principalResolver;

    public IdentityAuthenticationProvider(
            IdentityAuthProperties properties,
            HermesPrincipalResolver principalResolver,
            @Qualifier("hermesLoadBalancedRestClientBuilder") RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.principalResolver = principalResolver;
        this.identityClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
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

        HermesUserPrincipal principal = principalResolver.resolve(response)
                .orElseThrow(() -> new BadCredentialsException("User has no authorization context"));

        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
