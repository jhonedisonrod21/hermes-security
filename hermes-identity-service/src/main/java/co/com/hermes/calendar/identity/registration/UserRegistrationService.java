package co.com.hermes.calendar.identity.registration;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.role.RoleRepository;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private static final String GUEST_USER_ROLE = "GUEST_USER";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserAccountRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCaseOrEmailIgnoreCase(email, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role guestRole = roles.findByName(GUEST_USER_ROLE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "GUEST_USER role is not configured"));

        // El invitado se registra como cuenta de plataforma (sin tenant). Unirse o crear una
        // organizacion es un paso posterior; no se aprovisiona ningun tenant en el registro.
        UserAccount user = UserAccount.registeredUser(UUID.randomUUID(), email, passwordEncoder.encode(request.password()), guestRole);
        // El username (handle visible) es la parte local del correo (antes de la @), más ilustrativo
        // que el UUID; se desambigua con un sufijo numérico si ya existe.
        user.assignUsername(uniqueUsernameFrom(email));
        user.setName(request.name().trim());
        user.setPhone(normalizePhone(request.phone()));

        UserAccount saved = users.save(user);
        return new UserRegistrationResponse(saved.getId(), saved.getUsername(), saved.getEmail(), GUEST_USER_ROLE);
    }

    /** Deriva un username único a partir de la parte local del correo (p. ej. {@code ana.gomez}). */
    private String uniqueUsernameFrom(String email) {
        String base = email.substring(0, email.indexOf('@'));
        String candidate = base;
        int suffix = 1;
        while (users.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    /** Recorta el teléfono y lo deja en {@code null} si viene vacío. */
    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
