package co.com.hermes.calendar.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthClientSeederTest {

    @Test
    void seedsAuthorizationCodeClientForPostmanAndWebLogin() throws Exception {
        TestRegisteredClientRepository clients = new TestRegisteredClientRepository();
        ApplicationRunner runner = new OAuthClientSeeder().seedWebClient(
                clients,
                "hermes-web-client",
                "{noop}hermes-web-secret",
                "http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client, https://oauth.pstmn.io/v1/callback",
                Duration.ofMinutes(15),
                Duration.ofHours(8)
        );

        runner.run(new EmptyApplicationArguments());

        RegisteredClient client = clients.findByClientId("hermes-web-client");
        assertThat(client).isNotNull();
        assertThat(client.getClientSecret()).isEqualTo("{noop}hermes-web-secret");
        assertThat(client.getClientAuthenticationMethods()).containsExactly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        assertThat(client.getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(client.getRedirectUris())
                .containsExactlyInAnyOrder(
                        "http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client",
                        "https://oauth.pstmn.io/v1/callback"
                );
        assertThat(client.getScopes())
                .containsExactlyInAnyOrder(OidcScopes.OPENID, OidcScopes.PROFILE, "calendar.read", "calendar.write");
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getClientSettings().isRequireAuthorizationConsent()).isFalse();
        assertThat(client.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofMinutes(15));
        assertThat(client.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofHours(8));
        assertThat(client.getTokenSettings().isReuseRefreshTokens()).isFalse();
    }

    @Test
    void doesNotOverwriteAnExistingClient() throws Exception {
        RegisteredClient existingClient = RegisteredClient.withId("existing-id")
                .clientId("hermes-web-client")
                .clientSecret("{noop}existing-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://existing.example/callback")
                .scope(OidcScopes.OPENID)
                .build();
        TestRegisteredClientRepository clients = new TestRegisteredClientRepository(existingClient);

        new OAuthClientSeeder().seedWebClient(
                clients,
                "hermes-web-client",
                "{noop}new-secret",
                "https://oauth.pstmn.io/v1/callback",
                Duration.ofMinutes(15),
                Duration.ofHours(8)
        ).run(new EmptyApplicationArguments());

        RegisteredClient client = clients.findByClientId("hermes-web-client");
        assertThat(client.getId()).isEqualTo("existing-id");
        assertThat(client.getClientSecret()).isEqualTo("{noop}existing-secret");
        assertThat(client.getRedirectUris()).containsExactly("https://existing.example/callback");
    }

    private static final class TestRegisteredClientRepository implements RegisteredClientRepository {

        private RegisteredClient registeredClient;

        private TestRegisteredClientRepository() {
        }

        private TestRegisteredClientRepository(RegisteredClient registeredClient) {
            this.registeredClient = registeredClient;
        }

        @Override
        public void save(RegisteredClient registeredClient) {
            this.registeredClient = registeredClient;
        }

        @Override
        public RegisteredClient findById(String id) {
            if (registeredClient == null || !registeredClient.getId().equals(id)) {
                return null;
            }
            return registeredClient;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            if (registeredClient == null || !registeredClient.getClientId().equals(clientId)) {
                return null;
            }
            return registeredClient;
        }
    }

    private static final class EmptyApplicationArguments implements ApplicationArguments {

        @Override
        public String[] getSourceArgs() {
            return new String[0];
        }

        @Override
        public java.util.Set<String> getOptionNames() {
            return java.util.Set.of();
        }

        @Override
        public boolean containsOption(String name) {
            return false;
        }

        @Override
        public java.util.List<String> getOptionValues(String name) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> getNonOptionArgs() {
            return java.util.List.of();
        }
    }
}
