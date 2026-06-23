package co.com.hermes.calendar.identity.profile;

import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Lectura y edición del perfil propio del usuario autenticado. */
@Service
public class SelfProfileService {

    private final UserAccountRepository users;

    public SelfProfileService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public SelfProfileResponse get(UUID userId) {
        return SelfProfileResponse.from(require(userId));
    }

    @Transactional
    public SelfProfileResponse updatePhone(UUID userId, String phone) {
        UserAccount user = require(userId);
        String trimmed = phone == null || phone.trim().isEmpty() ? null : phone.trim();
        user.setPhone(trimmed);
        return SelfProfileResponse.from(user);
    }

    private UserAccount require(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
