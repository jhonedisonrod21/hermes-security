package co.com.hermes.calendar.identity.admin;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UserAdminService service = new UserAdminService(users);

    private UserAccount guest(UUID id) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("GUEST_USER");
        UserAccount user = UserAccount.registeredUser(id, "ada@hermes.test", "{noop}x", role);
        return user;
    }

    @Test
    void updatesUsernameAndEmail() {
        UUID id = UUID.randomUUID();
        UserAccount user = guest(id);
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(users.existsByUsernameIgnoreCaseAndIdNot("ada.perez", id)).thenReturn(false);
        when(users.existsByEmailIgnoreCaseAndIdNot("ada@acme.test", id)).thenReturn(false);

        UserResponse response = service.update(id, new UserUpdateRequest("ada.perez", "ada@acme.test"));

        assertThat(response.username()).isEqualTo("ada.perez");
        assertThat(response.email()).isEqualTo("ada@acme.test");
    }

    @Test
    void rejectsUpdateWhenEmailTaken() {
        UUID id = UUID.randomUUID();
        UserAccount user = guest(id);
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(users.existsByUsernameIgnoreCaseAndIdNot(eq("ada.perez"), eq(id))).thenReturn(false);
        when(users.existsByEmailIgnoreCaseAndIdNot("taken@acme.test", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new UserUpdateRequest("ada.perez", "taken@acme.test")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void locksAndUnlocksUser() {
        UUID id = UUID.randomUUID();
        UserAccount user = guest(id);
        when(users.findById(id)).thenReturn(Optional.of(user));

        assertThat(service.changeLock(id, true).locked()).isTrue();
        assertThat(user.isLocked()).isTrue();
        assertThat(service.changeLock(id, false).locked()).isFalse();
    }

    @Test
    void failsOnMissingUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
