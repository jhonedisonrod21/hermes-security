package co.com.hermes.calendar.identity.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirmación del restablecimiento con el token recibido y la nueva contraseña.")
public record PasswordResetConfirmRequest(
        @NotBlank @Schema(description = "Token recibido por correo.") String token,
        @NotBlank @Size(min = 8, max = 128) @Schema(description = "Nueva contraseña (mínimo 8 caracteres).") String newPassword
) {
}
