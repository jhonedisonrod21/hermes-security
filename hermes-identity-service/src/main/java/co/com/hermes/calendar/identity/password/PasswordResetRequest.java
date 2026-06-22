package co.com.hermes.calendar.identity.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud para iniciar el restablecimiento de contraseña.")
public record PasswordResetRequest(
        @Email @NotBlank @Schema(example = "ana@acme.test") String email
) {
}
