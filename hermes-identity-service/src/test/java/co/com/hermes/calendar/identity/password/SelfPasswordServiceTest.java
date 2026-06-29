package co.com.hermes.calendar.identity.password;

import co.com.hermes.calendar.identity.audit.SecurityAuditService;
import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfPasswordServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final SecurityAuditService audit = mock(SecurityAuditService.class);
    private final SelfPasswordService service = new SelfPasswordService(users, encoder, audit);

    private final UUID userId = UUID.randomUUID();

    private UserAccount user() {
        UserAccount u = UserAccount.registeredUser(userId, "ana@acme.test", "HASH_OLD", mock(Role.class));
        when(users.findById(userId)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void changesPasswordAndAuditsSuccess() {
        UserAccount u = user();
        when(encoder.matches("current", "HASH_OLD")).thenReturn(true);
        when(encoder.matches("newPass12", "HASH_OLD")).thenReturn(false);
        when(encoder.encode("newPass12")).thenReturn("HASH_NEW");

        service.changePassword(userId, "current", "newPass12");

        assertThat(u.getPasswordHash()).isEqualTo("HASH_NEW");
        verify(audit).recordEvent(eq(userId), eq(SecurityAuditService.PASSWORD_CHANGE),
                eq(SecurityAuditService.Outcome.SUCCESS), any());
    }

    @Test
    void rejectsWrongCurrentPasswordAndAuditsFailure() {
        user();
        when(encoder.matches("wrong", "HASH_OLD")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(userId, "wrong", "newPass12"))
                .isInstanceOf(ResponseStatusException.class);

        verify(audit).recordEvent(eq(userId), eq(SecurityAuditService.PASSWORD_CHANGE),
                eq(SecurityAuditService.Outcome.FAILURE), any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void rejectsNewPasswordEqualToCurrent() {
        user();
        when(encoder.matches("current", "HASH_OLD")).thenReturn(true);
        when(encoder.matches("current", "HASH_OLD")).thenReturn(true); // same value reused as new

        assertThatThrownBy(() -> service.changePassword(userId, "current", "current"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
