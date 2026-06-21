package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Alta de un usuario en un establecimiento con un rol de tenant.")
public record MembershipCreateRequest(
        @NotNull @Schema(description = "Usuario ya registrado (GUEST_USER).", example = "00000000-0000-0000-0000-000000000200")
        UUID userId,
        @NotBlank @Schema(description = "Rol de tenant a otorgar.", example = "TENANT_ADMIN")
        String role
) {
}
