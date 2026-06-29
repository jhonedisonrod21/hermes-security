package co.com.hermes.calendar.identity.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos editables de una cuenta de usuario.")
public record UserUpdateRequest(
        @NotBlank @Size(max = 120) @Schema(example = "Ana Pérez") String name,
        @NotBlank @Size(max = 160) @Schema(example = "ana.perez") String username,
        @NotBlank @Email @Size(max = 254) @Schema(example = "ana@acme.test") String email,
        @Size(max = 40) @Schema(description = "Telefono de contacto (SMS).", example = "+573001112233") String phone
) {
}
