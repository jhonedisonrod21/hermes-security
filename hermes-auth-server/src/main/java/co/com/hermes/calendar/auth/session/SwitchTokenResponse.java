package co.com.hermes.calendar.auth.session;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** Token re-emitido para la nueva organización activa. */
@Schema(description = "Token con los claims de la organización seleccionada.")
public record SwitchTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID tenantId,
        String tenantName,
        List<String> roles
) {
}
