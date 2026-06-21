package co.com.hermes.calendar.tenant.self;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Alta de un usuario como TENANT_PARTNER en el establecimiento del administrador.")
public record PartnerCreateRequest(
        @NotNull @Schema(description = "Usuario ya registrado.", example = "00000000-0000-0000-0000-000000000200")
        UUID userId
) {
}
