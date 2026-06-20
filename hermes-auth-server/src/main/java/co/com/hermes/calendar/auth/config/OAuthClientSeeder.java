package co.com.hermes.calendar.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

@Configuration
public class OAuthClientSeeder {

    @Bean
    ApplicationRunner seedWebClient(
            RegisteredClientRepository clients,
            @Value("${hermes.oauth.web-client.client-id}") String clientId,
            @Value("${hermes.oauth.web-client.client-secret}") String clientSecret,
            @Value("${hermes.oauth.web-client.redirect-uris}") String redirectUris
    ) {
        return args -> {
            if (clients.findByClientId(clientId) != null) {
                return;
            }

            RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUris(uris -> {
                        for (String redirectUri : redirectUris.split(",")) {
                            String normalized = redirectUri.trim();
                            if (!normalized.isBlank()) {
                                uris.add(normalized);
                            }
                        }
                    })
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope("calendar.read")
                    .scope("calendar.write")
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build())
                    .build();

            clients.save(webClient);
        };
    }
}
