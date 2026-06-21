package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cambio de estado operativo del establecimiento.")
public record TenantStatusUpdateRequest(
        @NotNull @Schema(example = "INACTIVE") TenantStatus status
) {
}
