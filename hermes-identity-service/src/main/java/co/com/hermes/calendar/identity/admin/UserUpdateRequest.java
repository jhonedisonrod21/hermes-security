package co.com.hermes.calendar.identity.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos editables de una cuenta de usuario.")
public record UserUpdateRequest(
        @NotBlank @Size(max = 160) @Schema(example = "ana.perez") String username,
        @NotBlank @Email @Size(max = 254) @Schema(example = "ana@acme.test") String email
) {
}
