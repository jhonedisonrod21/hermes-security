package co.com.hermes.calendar.identity.auth;

import co.com.hermes.calendar.identity.role.Role;
import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import co.com.hermes.calendar.shared.contract.CredentialVerificationRequest;
import co.com.hermes.calendar.shared.contract.CredentialVerificationResponse;
import co.com.hermes.calendar.shared.security.AccountScope;
import co.com.hermes.calendar.shared.security.HermesInternalHeaders;
import co.com.hermes.calendar.shared.security.HermesInternalKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/internal/auth")
@Tag(name = "Internal identity authentication", description = "Endpoints internos usados por Auth Server para validar usuarios.")
public class InternalCredentialController {

    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final InternalAuthProperties properties;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public InternalCredentialController(
            InternalAuthProperties properties,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/credentials/verify")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Valida credenciales de usuario",
            description = "Verifica username/email y password contra las tablas identity_users e identity_user_roles. "
                    + "Este endpoint es interno y requiere X-Hermes-Internal-Key.",
            security = @SecurityRequirement(name = "hermes-internal-key")
    )
    @ApiResponse(responseCode = "200", description = "Credenciales procesadas.",
            content = @Content(schema = @Schema(implementation = CredentialVerificationResponse.class)))
    @ApiResponse(responseCode = "401", description = "Llave interna invalida.", content = @Content)
    public CredentialVerificationResponse verify(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Llave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody CredentialVerificationRequest request
    ) {
        assertInternalKey(apiKey);

        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(request.username(), request.username())
                .filter(user -> user.isEnabled() && !user.isLocked())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(this::authenticated)
                .orElseGet(CredentialVerificationResponse::failed);
    }

    @GetMapping("/users/{username}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Consulta perfil interno por username",
            description = "Devuelve el perfil basico de Identity sin validar password. Auth Server lo usa durante "
                    + "la emision de tokens para reconstruir claims Hermes.",
            security = @SecurityRequirement(name = "hermes-internal-key")
    )
    @ApiResponse(responseCode = "200", description = "Usuario encontrado.",
            content = @Content(schema = @Schema(implementation = CredentialVerificationResponse.class)))
    @ApiResponse(responseCode = "401", description = "Llave interna invalida.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado o no activo.", content = @Content)
    public CredentialVerificationResponse profile(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Llave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @Parameter(description = "Username o email del usuario.", example = "admin@hermes.local")
            @PathVariable String username
    ) {
        assertInternalKey(apiKey);

        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .filter(user -> user.isEnabled() && !user.isLocked())
                .map(this::authenticated)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void assertInternalKey(String apiKey) {
        if (!HermesInternalKeys.matches(properties.apiKey(), apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }

    private CredentialVerificationResponse authenticated(UserAccount user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .distinct()
                .sorted()
                .toList();
        // Una cuenta queda anclada a plataforma si tiene algún rol de alcance PLATFORM (p. ej.
        // SYSTEM_ADMIN). El resto deja que la membresía activa decida el alcance en Auth Server.
        boolean platformAnchored = user.getRoles().stream()
                .anyMatch(role -> role.getScope() == AccountScope.PLATFORM);

        return new CredentialVerificationResponse(
                true,
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                permissions,
                platformAnchored,
                user.isEnabled(),
                user.isLocked()
        );
    }
}
