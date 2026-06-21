package co.com.hermes.calendar.identity.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Bloqueo/desbloqueo de una cuenta.")
public record UserLockRequest(
        @NotNull @Schema(example = "true") Boolean locked
) {
}
