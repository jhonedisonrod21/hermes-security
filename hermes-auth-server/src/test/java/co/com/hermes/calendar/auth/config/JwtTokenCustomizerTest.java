package co.com.hermes.calendar.auth.config;

import co.com.hermes.calendar.auth.identity.HermesPrincipalResolver;
import co.com.hermes.calendar.auth.identity.HermesUserPrincipal;
import co.com.hermes.calendar.auth.identity.IdentityAuthProperties;
import co.com.hermes.calendar.shared.security.AccountScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JwtTokenCustomizerTest {

    private static final String INTERNAL_KEY = "test-internal-key";
    private static final String IDENTITY_BASE_URL = "http://hermes-identity-service";
    private static final String TENANT_BASE_URL = "http://hermes-tenant-service";

    @Test
    void addsHermesClaimsFromAuthenticatedLoginPrincipal() {
        UUID userId = UUID.fromString("30e6f847-7f3e-4b08-82d2-66c7cbf3f85d");
        UUID tenantId = UUID.fromString("aef1ecb8-3f07-4795-812e-929b4a6d4e76");
        HermesUserPrincipal principal = new HermesUserPrincipal(
                userId,
                AccountScope.TENANT,
                tenantId,
                "ada-company",
                "Ada Company",
                "ada@hermes.test",
                "ada@hermes.test",
                "Ada Lovelace",
                List.of("TENANT_ADMIN"),
                List.of("calendar:read", "calendar:write"),
                List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))
        );
        JwtEncodingContext context = jwtContext()
                .principal(UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                ))
                .build();

        IdentityAuthProperties properties = new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL);
        RestClient.Builder builder = RestClient.builder();
        new AuthorizationServerConfig().jwtTokenCustomizer(
                properties,
                new HermesPrincipalResolver(properties, builder),
                builder
        ).customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat((Object) claims.getClaim("account_scope")).isEqualTo("TENANT");
        assertThat((Object) claims.getClaim("user_id")).isEqualTo(userId.toString());
        assertThat((Object) claims.getClaim("preferred_username")).isEqualTo("ada@hermes.test");
        assertThat((Object) claims.getClaim("email")).isEqualTo("ada@hermes.test");
        assertThat((Object) claims.getClaim("name")).isEqualTo("Ada Lovelace");
        assertThat((Object) claims.getClaim("tenant_id")).isEqualTo(tenantId.toString());
        assertThat((Object) claims.getClaim("tenant_slug")).isEqualTo("ada-company");
        assertThat((Object) claims.getClaim("tenant_name")).isEqualTo("Ada Company");
        assertThat((List<String>) claims.getClaim("roles")).containsExactly("TENANT_ADMIN");
        assertThat((List<String>) claims.getClaim("permissions")).containsExactly("calendar:read", "calendar:write");
    }

    @Test
    void resolvesHermesClaimsDuringAuthorizationCodeTokenExchange() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID userId = UUID.fromString("30e6f847-7f3e-4b08-82d2-66c7cbf3f85d");
        UUID tenantId = UUID.fromString("aef1ecb8-3f07-4795-812e-929b4a6d4e76");
        RegisteredClient client = registeredClient();
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName("ada@hermes.test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid", "profile"))
                .build();
        JwtEncodingContext context = jwtContext()
                .registeredClient(client)
                .authorization(authorization)
                .build();

        server.expect(once(), requestTo(IDENTITY_BASE_URL + "/internal/auth/users/ada%40hermes.test"))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "authenticated": true,
                          "userId": "30e6f847-7f3e-4b08-82d2-66c7cbf3f85d",
                          "username": "ada@hermes.test",
                          "email": "ada@hermes.test",
                          "enabled": true,
                          "locked": false,
                          "platformAnchored": false,
                          "roles": []
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TENANT_BASE_URL + "/internal/users/" + userId + "/tenant-context/default"))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "tenantId": "aef1ecb8-3f07-4795-812e-929b4a6d4e76",
                          "tenantSlug": "ada-company",
                          "tenantName": "Ada Company",
                          "roles": ["TENANT_ADMIN"],
                          "permissions": ["calendar:read"]
                        }
                        """, MediaType.APPLICATION_JSON));

        IdentityAuthProperties properties = new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL);
        new AuthorizationServerConfig().jwtTokenCustomizer(
                properties,
                new HermesPrincipalResolver(properties, builder),
                builder
        ).customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat((Object) claims.getClaim("tenant_id")).isEqualTo(tenantId.toString());
        assertThat((List<String>) claims.getClaim("roles")).containsExactly("TENANT_ADMIN");
        assertThat((List<String>) claims.getClaim("permissions")).containsExactly("calendar:read");

        server.verify();
    }

    @Test
    void addsPlatformClaimsForSystemAdminWithoutTenant() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000100");
        RegisteredClient client = registeredClient();
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName("admin@hermes.local")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid", "profile"))
                .build();
        JwtEncodingContext context = jwtContext()
                .registeredClient(client)
                .authorization(authorization)
                .build();

        server.expect(once(), requestTo(IDENTITY_BASE_URL + "/internal/auth/users/admin%40hermes.local"))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "authenticated": true,
                          "userId": "00000000-0000-0000-0000-000000000100",
                          "username": "admin@hermes.local",
                          "email": "admin@hermes.local",
                          "enabled": true,
                          "locked": false,
                          "platformAnchored": true,
                          "roles": ["SYSTEM_ADMIN"],
                          "permissions": ["platform:admin"]
                        }
                        """, MediaType.APPLICATION_JSON));

        IdentityAuthProperties properties = new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL);
        new AuthorizationServerConfig().jwtTokenCustomizer(
                properties,
                new HermesPrincipalResolver(properties, builder),
                builder
        ).customize(context);

        JwtClaimsSet claims = context.getClaims().build();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat((Object) claims.getClaim("account_scope")).isEqualTo("PLATFORM");
        assertThat((Object) claims.getClaim("tenant_id")).isNull();
        assertThat((List<String>) claims.getClaim("roles")).containsExactly("SYSTEM_ADMIN");
        assertThat((List<String>) claims.getClaim("permissions")).containsExactly("platform:admin");

        // Only the identity profile lookup happens; no tenant-context call for platform accounts.
        server.verify();
    }

    private JwtEncodingContext.Builder jwtContext() {
        return JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256),
                        JwtClaimsSet.builder()
                )
                .registeredClient(registeredClient())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid", "profile"));
    }

    private RegisteredClient registeredClient() {
        return RegisteredClient.withId("test-client-id")
                .clientId("hermes-web-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .scope("openid")
                .scope("profile")
                .build();
    }
}
