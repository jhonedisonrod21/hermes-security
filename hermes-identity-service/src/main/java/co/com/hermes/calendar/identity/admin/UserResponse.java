package co.com.hermes.calendar.identity.admin;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vista de una cuenta de usuario.")
public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        boolean locked,
        List<String> roles,
        OffsetDateTime createdAt
) {

    public static UserResponse from(UserAccount user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.isLocked(),
                roles,
                user.getCreatedAt()
        );
    }
}
