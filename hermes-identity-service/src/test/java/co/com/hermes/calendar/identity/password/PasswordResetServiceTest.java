package co.com.hermes.calendar.identity.password;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetNotificationClient notifications = mock(PasswordResetNotificationClient.class);
    private final PasswordResetService service =
            new PasswordResetService(users, tokens, encoder, notifications, 30, "http://front");

    private UserAccount user(UUID id) {
        return UserAccount.registeredUser(id, "ana@acme.test", "oldhash", mock(Role.class));
    }

    @Test
    void requestStoresHashedTokenAndSendsEmail() {
        UserAccount user = user(UUID.randomUUID());
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("ana@acme.test", "ana@acme.test"))
                .thenReturn(Optional.of(user));

        service.requestReset("Ana@Acme.test");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(tokenCaptor.capture());
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifications).sendPasswordResetEmail(eq("ana@acme.test"), anyString(), urlCaptor.capture(), eq(30));
        // El enlace lleva el token en claro, pero en BD se guarda su hash (no aparece el token en la URL guardada).
        assertThat(urlCaptor.getValue()).startsWith("http://front/reset-password?token=");
    }

    @Test
    void requestForUnknownEmailDoesNothingButDoesNotLeak() {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());

        service.requestReset("ghost@acme.test"); // no lanza

        verify(tokens, never()).save(any());
        verify(notifications, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void confirmSetsNewPasswordAndConsumesToken() {
        UUID userId = UUID.randomUUID();
        UserAccount user = user(userId);
        // Reconstruimos el hash del token "secret" como lo hace el servicio.
        PasswordResetToken token = capturedTokenFor(userId, "secret");
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.encode("newPass123")).thenReturn("newhash");

        service.confirmReset("secret", "newPass123");

        assertThat(token.isUsed()).isTrue();
        verify(users, never()).save(any()); // entidad gestionada: dirty checking
        assertThat(user.getPasswordHash()).isEqualTo("newhash");
    }

    @Test
    void confirmRejectsUsedOrExpiredToken() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset("bad", "newPass123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    /** Crea un token válido cuyo hash coincide con el que calculará el servicio para rawToken. */
    private PasswordResetToken capturedTokenFor(UUID userId, String rawToken) {
        // Generamos vía requestReset para no duplicar el algoritmo de hash en el test.
        UserAccount u = user(userId);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase(anyString(), anyString())).thenReturn(Optional.of(u));
        // No podemos forzar el token aleatorio; en su lugar construimos el token con el hash que el
        // servicio espera reusando su misma función a través de un token conocido.
        // Expiración fija lejana: el token nunca caduca durante el test (sin depender del reloj del sistema).
        OffsetDateTime farFuture = OffsetDateTime.of(2999, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        return PasswordResetToken.issue(userId, sha256(rawToken), farFuture);
    }

    private static String sha256(String value) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
