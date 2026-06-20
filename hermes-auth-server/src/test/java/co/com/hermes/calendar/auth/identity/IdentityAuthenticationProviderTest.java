package co.com.hermes.calendar.auth.identity;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityAuthenticationProviderTest {

    private static final String INTERNAL_KEY = "test-internal-key";
    private static final String IDENTITY_BASE_URL = "http://hermes-identity-service";
    private static final String TENANT_BASE_URL = "http://hermes-tenant-service";

    @Test
    void authenticatesUserWithTenantContext() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID userId = UUID.fromString("30e6f847-7f3e-4b08-82d2-66c7cbf3f85d");
        UUID tenantId = UUID.fromString("aef1ecb8-3f07-4795-812e-929b4a6d4e76");

        server.expect(once(), requestTo(IDENTITY_BASE_URL + "/internal/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "authenticated": true,
                          "userId": "30e6f847-7f3e-4b08-82d2-66c7cbf3f85d",
                          "tenantId": "aef1ecb8-3f07-4795-812e-929b4a6d4e76",
                          "username": "ada@hermes.test",
                          "email": "ada@hermes.test",
                          "enabled": true,
                          "locked": false,
                          "roles": ["USER"]
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TENANT_BASE_URL + "/internal/users/" + userId + "/tenant-context/default"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "tenantId": "aef1ecb8-3f07-4795-812e-929b4a6d4e76",
                          "tenantSlug": "ada-company",
                          "tenantName": "Ada Company",
                          "roles": ["OWNER", "ADMIN"],
                          "permissions": ["tenant:manage", "calendar:read"]
                        }
                        """, MediaType.APPLICATION_JSON));

        IdentityAuthenticationProvider provider = new IdentityAuthenticationProvider(
                new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL),
                builder
        );

        var authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada@hermes.test", "secret")
        );

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(HermesUserPrincipal.class);
        HermesUserPrincipal principal = (HermesUserPrincipal) authentication.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getTenantId()).isEqualTo(tenantId);
        assertThat(principal.getTenantSlug()).isEqualTo("ada-company");
        assertThat(principal.getEmail()).isEqualTo("ada@hermes.test");
        assertThat(principal.getRoles()).containsExactly("OWNER", "ADMIN");
        assertThat(principal.getPermissions()).containsExactly("tenant:manage", "calendar:read");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_OWNER", "ROLE_ADMIN", FactorGrantedAuthority.PASSWORD_AUTHORITY);

        server.verify();
    }

    @Test
    void rejectsInvalidIdentityResponseBeforeTenantLookup() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), requestTo(IDENTITY_BASE_URL + "/internal/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Hermes-Internal-Key", INTERNAL_KEY))
                .andRespond(withSuccess("""
                        {
                          "authenticated": false,
                          "enabled": true,
                          "locked": false,
                          "roles": []
                        }
                        """, MediaType.APPLICATION_JSON));

        IdentityAuthenticationProvider provider = new IdentityAuthenticationProvider(
                new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL),
                builder
        );

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada@hermes.test", "bad-secret")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        server.verify();
    }

    @Test
    void rejectsUsersWithoutActiveTenantContext() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID userId = UUID.fromString("30e6f847-7f3e-4b08-82d2-66c7cbf3f85d");

        server.expect(once(), requestTo(IDENTITY_BASE_URL + "/internal/auth/credentials/verify"))
                .andRespond(withSuccess("""
                        {
                          "authenticated": true,
                          "userId": "30e6f847-7f3e-4b08-82d2-66c7cbf3f85d",
                          "username": "ada@hermes.test",
                          "email": "ada@hermes.test",
                          "enabled": true,
                          "locked": false,
                          "roles": ["USER"]
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TENANT_BASE_URL + "/internal/users/" + userId + "/tenant-context/default"))
                .andRespond(withSuccess("""
                        {
                          "roles": [],
                          "permissions": []
                        }
                        """, MediaType.APPLICATION_JSON));

        IdentityAuthenticationProvider provider = new IdentityAuthenticationProvider(
                new IdentityAuthProperties(IDENTITY_BASE_URL, INTERNAL_KEY, TENANT_BASE_URL),
                builder
        );

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada@hermes.test", "secret")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("User has no active tenant");

        server.verify();
    }
}
