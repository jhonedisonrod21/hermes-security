package co.com.hermes.calendar.identity.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña del propio usuario: requiere la contraseña actual para autorizarlo. */
@Schema(description = "Cambio de contraseña con verificación de la contraseña actual.")
public record SelfPasswordChangeRequest(
        @Schema(description = "Contraseña actual del usuario.", example = "miClaveActual1")
        @NotBlank(message = "La contraseña actual es obligatoria")
        String currentPassword,

        @Schema(description = "Nueva contraseña (mínimo 8 caracteres).", example = "miNuevaClave2")
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
        String newPassword
) {
}
