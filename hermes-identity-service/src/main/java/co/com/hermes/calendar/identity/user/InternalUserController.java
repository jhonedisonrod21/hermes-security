package co.com.hermes.calendar.identity.user;

import co.com.hermes.calendar.identity.auth.InternalAuthProperties;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import co.com.hermes.calendar.shared.security.HermesInternalKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Lectura interna del contacto de un usuario por su UUID, usada por notification-service para dirigir
 * los avisos (email y SMS). Protegido por la clave interna compartida; el gateway bloquea /internal/**.
 */
@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal users", description = "Lectura interna del contacto de usuarios.")
public class InternalUserController {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final UserAccountRepository users;
    private final InternalAuthProperties properties;

    public InternalUserController(UserAccountRepository users, InternalAuthProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    @GetMapping("/{id}/contact")
    @Operation(summary = "Contacto de un usuario por id (uso interno)",
            security = @SecurityRequirement(name = "hermes-internal-key"))
    public UserContactResponse contact(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Clave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @PathVariable UUID id
    ) {
        if (!HermesInternalKeys.matches(properties.apiKey(), apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
        return users.findById(id)
                .map(UserContactResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
