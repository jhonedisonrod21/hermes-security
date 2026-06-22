package co.com.hermes.calendar.identity.admin;

import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * Gestión de cuentas de usuario por el administrador del sistema.
 */
@Service
public class UserAdminService {

    private final UserAccountRepository users;

    public UserAdminService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String query, Pageable pageable) {
        Page<UserAccount> page = StringUtils.hasText(query)
                ? users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query.trim(), query.trim(), pageable)
                : users.findAll(pageable);
        return page.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(require(id));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        UserAccount user = require(id);
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }
        if (users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        String phone = request.phone() == null || request.phone().trim().isEmpty() ? null : request.phone().trim();
        user.updateProfile(username, email, phone);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeLock(UUID id, boolean locked) {
        UserAccount user = require(id);
        user.setLocked(locked);
        return UserResponse.from(user);
    }

    private UserAccount require(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
