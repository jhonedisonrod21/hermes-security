package co.com.hermes.calendar.identity.registration;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.role.RoleRepository;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserRegistrationService service = new UserRegistrationService(users, roles, encoder);

    private void guestRoleExists() {
        when(roles.findByName("GUEST_USER")).thenReturn(Optional.of(mock(Role.class)));
        when(encoder.encode(any())).thenReturn("{noop}x");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void usernameIsEmailLocalPart() {
        guestRoleExists();
        when(users.existsByUsernameIgnoreCaseOrEmailIgnoreCase(any(), any())).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("ana.gomez")).thenReturn(false);

        UserRegistrationResponse response = service.register(
                new UserRegistrationRequest("Ana Gómez", "Ana.Gomez@Acme.test", "secret123", null));

        assertThat(response.username()).isEqualTo("ana.gomez");
        assertThat(response.email()).isEqualTo("ana.gomez@acme.test");
    }

    @Test
    void usernameGetsNumericSuffixWhenTaken() {
        guestRoleExists();
        when(users.existsByUsernameIgnoreCaseOrEmailIgnoreCase(any(), any())).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("ana")).thenReturn(true);
        when(users.existsByUsernameIgnoreCase("ana1")).thenReturn(false);

        UserRegistrationResponse response = service.register(
                new UserRegistrationRequest("Ana Gómez", "ana@other.test", "secret123", null));

        assertThat(response.username()).isEqualTo("ana1");
    }
}
