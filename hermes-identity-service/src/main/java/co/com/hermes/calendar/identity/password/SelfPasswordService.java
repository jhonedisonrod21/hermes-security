package co.com.hermes.calendar.identity.password;

import co.com.hermes.calendar.identity.audit.SecurityAuditService;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Cambio de contraseña del usuario autenticado (RF-03): verifica la contraseña actual, aplica la nueva
 * y deja registro de auditoría tanto del éxito como del intento fallido.
 */
@Service
public class SelfPasswordService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService audit;

    public SelfPasswordService(UserAccountRepository users, PasswordEncoder passwordEncoder,
                               SecurityAuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            audit.record(userId, SecurityAuditService.PASSWORD_CHANGE,
                    SecurityAuditService.Outcome.FAILURE, "contraseña actual incorrecta");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        audit.record(userId, SecurityAuditService.PASSWORD_CHANGE,
                SecurityAuditService.Outcome.SUCCESS, null);
    }
}
