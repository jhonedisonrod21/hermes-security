package co.com.hermes.calendar.identity.auth;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import co.com.hermes.calendar.shared.contract.CredentialVerificationRequest;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalCredentialControllerTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final InternalCredentialController controller = new InternalCredentialController(
            new InternalAuthProperties(INTERNAL_KEY),
            users,
            passwordEncoder
    );

    @Test
    void verifiesValidCredentialsAndReturnsUserProfile() {
        UUID userId = UUID.fromString("30e6f847-7f3e-4b08-82d2-66c7cbf3f85d");
        UUID tenantId = UUID.fromString("aef1ecb8-3f07-4795-812e-929b4a6d4e76");
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("USER");
        UserAccount user = UserAccount.registeredUser(
                userId,
                "ada@hermes.test",
                passwordEncoder.encode("secret"),
                role
        );
        user.assignTenant(tenantId);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("ada@hermes.test", "ada@hermes.test"))
                .thenReturn(Optional.of(user));

        CredentialVerificationResponse response = controller.verify(
                INTERNAL_KEY,
                new CredentialVerificationRequest("ada@hermes.test", "secret")
        );

        assertThat(response.authenticated()).isTrue();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.username()).isEqualTo("ada@hermes.test");
        assertThat(response.email()).isEqualTo("ada@hermes.test");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.enabled()).isTrue();
        assertThat(response.locked()).isFalse();
        verify(users).findByUsernameIgnoreCaseOrEmailIgnoreCase("ada@hermes.test", "ada@hermes.test");
    }

    @Test
    void returnsFailedResponseForWrongPassword() {
        Role role = mock(Role.class);
        UserAccount user = UserAccount.registeredUser(
                UUID.randomUUID(),
                "ada@hermes.test",
                passwordEncoder.encode("secret"),
                role
        );
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("ada@hermes.test", "ada@hermes.test"))
                .thenReturn(Optional.of(user));

        CredentialVerificationResponse response = controller.verify(
                INTERNAL_KEY,
                new CredentialVerificationRequest("ada@hermes.test", "wrong-secret")
        );

        assertThat(response.authenticated()).isFalse();
        assertThat(response.enabled()).isFalse();
        assertThat(response.locked()).isFalse();
    }

    @Test
    void rejectsInvalidInternalKey() {
        assertThatThrownBy(() -> controller.verify(
                "wrong-key",
                new CredentialVerificationRequest("ada@hermes.test", "secret")
        )).isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
