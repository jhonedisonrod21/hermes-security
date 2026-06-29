package co.com.hermes.calendar.identity.profile;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** Perfil propio del usuario autenticado. */
@Schema(description = "Perfil del usuario autenticado.")
public record SelfProfileResponse(UUID id, String username, String email, String name, String phone, List<String> roles) {

    public static SelfProfileResponse from(UserAccount user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new SelfProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName(), user.getPhone(), roles);
    }
}
