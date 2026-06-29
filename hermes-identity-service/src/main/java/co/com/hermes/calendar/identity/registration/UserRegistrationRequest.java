package co.com.hermes.calendar.identity.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud publica para registrar un usuario con correo y password.")
public record UserRegistrationRequest(
        @Schema(description = "Nombre visible del usuario.", example = "Ana Gómez")
        @NotBlank
        @Size(max = 120)
        String name,
        @Schema(description = "Correo electronico que sera usado como username.", example = "ana@acme.test")
        @Email
        @NotBlank
        String email,
        @Schema(description = "Password del usuario. Minimo 8 caracteres.", example = "localPass123")
        @NotBlank
        @Size(min = 8, max = 128)
        String password,
        @Schema(description = "Telefono de contacto para notificaciones por SMS (opcional).", example = "+573001112233")
        @Size(max = 40)
        String phone
) {
}
