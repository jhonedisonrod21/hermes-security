package co.com.hermes.calendar.identity.registration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Usuario invitado registrado (sin organizacion).")
public record UserRegistrationResponse(
        @Schema(description = "Identificador del usuario.", example = "ad7bb6e9-30ff-4f3e-a9e9-2d5df04cc59d")
        UUID userId,
        @Schema(description = "Username (handle) derivado de la parte local del correo.", example = "ana")
        String username,
        @Schema(description = "Correo registrado.", example = "ana@acme.test")
        String email,
        @Schema(description = "Rol global asignado en Identity.", example = "GUEST_USER")
        String role
) {
}
