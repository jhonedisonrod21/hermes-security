package co.com.hermes.calendar.tenant.internal;

import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import co.com.hermes.calendar.shared.contract.TenantContextResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal tenant context", description = "Endpoints internos usados por Auth Server para construir claims de tenant y permisos.")
public class InternalTenantContextController {

    private static final String ACTIVE = "ACTIVE";
    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final InternalTenantProperties properties;
    private final TenantMembershipRepository memberships;

    public InternalTenantContextController(InternalTenantProperties properties, TenantMembershipRepository memberships) {
        this.properties = properties;
        this.memberships = memberships;
    }

    @GetMapping("/{userId}/tenant-context/default")
    @Operation(
            summary = "Obtiene el contexto de tenant por defecto de un usuario",
            description = "Busca la primera membresia ACTIVE del usuario y devuelve tenant, roles y permisos efectivos. "
                    + "Este endpoint es interno y requiere X-Hermes-Internal-Key.",
            security = @SecurityRequirement(name = "hermes-internal-key")
    )
    @ApiResponse(responseCode = "200", description = "Contexto de tenant encontrado.",
            content = @Content(schema = @Schema(implementation = TenantContextResponse.class)))
    @ApiResponse(responseCode = "401", description = "Llave interna invalida.", content = @Content)
    @ApiResponse(responseCode = "404", description = "El usuario no tiene tenant activo.", content = @Content)
    public TenantContextResponse defaultTenantContext(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Llave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @Parameter(description = "Identificador del usuario.", example = "00000000-0000-0000-0000-000000000100")
            @PathVariable UUID userId
    ) {
        assertInternalKey(apiKey);

        TenantMembership membership = memberships.findByUserIdAndStatusOrderByCreatedAtAsc(userId, ACTIVE)
                .stream()
                // Un tenant marcado INACTIVE por el SYSTEM_ADMIN deja de otorgar contexto a sus usuarios.
                .filter(candidate -> candidate.getTenant().getStatus() == TenantStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User has no active tenant"));

        return toContext(membership);
    }

    @GetMapping("/{userId}/tenant-context/{tenantId}")
    @Operation(
            summary = "Obtiene el contexto de un usuario en un tenant concreto",
            description = "Valida que el usuario tenga membresia ACTIVE en ese tenant (y que el tenant este ACTIVE) y "
                    + "devuelve roles y permisos efectivos. Lo usa Auth Server al cambiar de organizacion activa.",
            security = @SecurityRequirement(name = "hermes-internal-key")
    )
    @ApiResponse(responseCode = "200", description = "Contexto encontrado.",
            content = @Content(schema = @Schema(implementation = TenantContextResponse.class)))
    @ApiResponse(responseCode = "404", description = "El usuario no es miembro activo de ese tenant.", content = @Content)
    public TenantContextResponse tenantContext(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Llave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @PathVariable UUID userId,
            @PathVariable UUID tenantId
    ) {
        assertInternalKey(apiKey);

        TenantMembership membership = memberships.findByTenant_IdAndUserId(tenantId, userId)
                .filter(m -> ACTIVE.equals(m.getStatus()) && m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not an active member of this tenant"));

        return toContext(membership);
    }

    private static TenantContextResponse toContext(TenantMembership membership) {
        List<String> roles = membership.getRoles().stream()
                .map(TenantRole::getName)
                .sorted()
                .toList();
        List<String> permissions = membership.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .distinct()
                .sorted()
                .toList();
        return new TenantContextResponse(
                membership.getTenant().getId(),
                membership.getTenant().getSlug(),
                membership.getTenant().getName(),
                roles,
                permissions
        );
    }

    private void assertInternalKey(String apiKey) {
        if (!HermesInternalKeys.matches(properties.apiKey(), apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
