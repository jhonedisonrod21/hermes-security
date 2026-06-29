package co.com.hermes.calendar.identity.password;

import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Restablecimiento de contraseña con un estándar seguro:
 * <ul>
 *   <li>Token aleatorio de 256 bits; se guarda solo su <b>hash</b> SHA-256 (no el valor en claro).</li>
 *   <li>Caducidad corta y <b>un solo uso</b>.</li>
 *   <li><b>Sin enumeración de usuarios</b>: la solicitud siempre responde igual, exista o no el correo.</li>
 *   <li>El correo se entrega vía notification-service.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotificationClient notifications;
    private final int ttlMinutes;
    private final String frontBaseUrl;

    public PasswordResetService(
            UserAccountRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            PasswordResetNotificationClient notifications,
            @Value("${hermes.password-reset.ttl-minutes:30}") int ttlMinutes,
            @Value("${hermes.front.base-url:http://127.0.0.1:5173}") String frontBaseUrl
    ) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.ttlMinutes = ttlMinutes;
        this.frontBaseUrl = frontBaseUrl;
    }

    /**
     * Inicia el restablecimiento. Responde siempre igual (no revela si el correo existe). Si la cuenta
     * existe y está activa, genera un token y envía el correo con el enlace.
     */
    @Transactional
    public void requestReset(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<UserAccount> match = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(normalized, normalized)
                .filter(user -> user.isEnabled() && !user.isLocked());
        if (match.isEmpty()) {
            log.info("Password reset requested for unknown/inactive account; no email sent");
            return; // silencioso: sin enumeración
        }

        UserAccount user = match.get();
        String rawToken = generateToken();
        tokens.save(PasswordResetToken.issue(user.getId(), sha256(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(ttlMinutes)));

        String resetUrl = frontBaseUrl + "/reset-password?token=" + rawToken;
        notifications.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetUrl, ttlMinutes);
    }

    /**
     * Confirma el restablecimiento con el token recibido por correo y fija la nueva contraseña. El token
     * debe existir, no estar usado ni caducado. Mensaje de error genérico para no dar pistas.
     */
    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(OffsetDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        UserAccount user = users.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32]; // 256 bits
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
