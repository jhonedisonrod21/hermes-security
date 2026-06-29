package co.com.hermes.calendar.identity.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Perfil propio del usuario autenticado. Tras el gateway: {@code /identity/me}. Accesible a cualquier
 * usuario autenticado (incluido GUEST_USER); el usuario se toma del token, así que solo ve/edita el suyo.
 */
@RestController
@RequestMapping("/me")
@Tag(name = "Self profile", description = "Perfil propio del usuario autenticado.")
@SecurityRequirement(name = "bearer-jwt")
public class SelfProfileController {

    private final SelfProfileService service;

    public SelfProfileController(SelfProfileService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta mi perfil")
    public SelfProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.get(callerUserId(jwt));
    }

    @PutMapping
    @Operation(summary = "Actualiza mi perfil (teléfono para recordatorios SMS)")
    public SelfProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody SelfProfileUpdateRequest request) {
        return service.updatePhone(callerUserId(jwt), request.phone());
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
