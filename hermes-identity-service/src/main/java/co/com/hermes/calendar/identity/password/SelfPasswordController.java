package co.com.hermes.calendar.identity.password;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Cambio de contraseña del usuario autenticado. Tras el gateway: {@code PUT /identity/me/password}.
 * El usuario se toma del token (solo puede cambiar la suya).
 */
@RestController
@RequestMapping("/me/password")
@Tag(name = "Self password", description = "Cambio de contraseña del usuario autenticado.")
@SecurityRequirement(name = "bearer-jwt")
public class SelfPasswordController {

    private final SelfPasswordService service;

    public SelfPasswordController(SelfPasswordService service) {
        this.service = service;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia mi contraseña verificando la actual")
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody SelfPasswordChangeRequest request) {
        service.changePassword(callerUserId(jwt), request.currentPassword(), request.newPassword());
    }

    /** Usuario del llamante, del token (claim user_id, con fallback a sub). */
    private static UUID callerUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user identity");
        }
    }
}
