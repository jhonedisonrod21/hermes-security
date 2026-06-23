package co.com.hermes.calendar.auth.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Organización destino a la que cambiar el token activo.")
public record SwitchTenantRequest(@NotNull UUID tenantId) {
}
