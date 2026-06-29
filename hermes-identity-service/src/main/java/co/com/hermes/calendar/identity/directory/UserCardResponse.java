package co.com.hermes.calendar.identity.directory;

import co.com.hermes.calendar.identity.user.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Ficha mínima de un usuario para mostrar su nombre (no su UUID) en pantallas del establecimiento. */
@Schema(description = "Ficha pública mínima de un usuario (para resolver id -> nombre).")
public record UserCardResponse(
        @Schema(description = "Identificador del usuario.") UUID id,
        @Schema(description = "Username (handle) visible.", example = "ana.gomez") String username,
        @Schema(description = "Correo del usuario.", example = "ana.gomez@acme.test") String email,
        @Schema(description = "Nombre visible del usuario.", example = "Ana Gómez") String name
) {
    public static UserCardResponse from(UserAccount user) {
        return new UserCardResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName());
    }
}
