package co.com.hermes.calendar.identity.registration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Usuario registrado y tenant inicial aprovisionado.")
public record UserRegistrationResponse(
        @Schema(description = "Identificador del usuario.", example = "ad7bb6e9-30ff-4f3e-a9e9-2d5df04cc59d")
        UUID userId,
        @Schema(description = "Correo/username registrado.", example = "ana@acme.test")
        String email,
        @Schema(description = "Tenant inicial creado para el usuario.")
        UUID tenantId,
        @Schema(description = "Slug del tenant inicial.", example = "user-ad7bb6e9")
        String tenantSlug,
        @Schema(description = "Rol global asignado en Identity.", example = "USER")
        String role
) {
}
