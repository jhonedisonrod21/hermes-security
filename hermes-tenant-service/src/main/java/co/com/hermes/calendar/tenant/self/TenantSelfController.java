package co.com.hermes.calendar.tenant.self;

import co.com.hermes.calendar.tenant.admin.MembershipResponse;
import co.com.hermes.calendar.tenant.admin.TenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Self-service del TENANT_ADMIN sobre su propio establecimiento. Tras el gateway (StripPrefix=1)
 * queda bajo {@code /tenant/me}. El tenant se toma del claim {@code tenant_id} del token, así un
 * administrador nunca puede operar sobre un tenant ajeno.
 */
@RestController
@RequestMapping("/me")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@Tag(name = "Tenant self-service", description = "Gestión del propio establecimiento por el TENANT_ADMIN.")
@SecurityRequirement(name = "bearer-jwt")
public class TenantSelfController {

    private final TenantSelfService service;

    public TenantSelfController(TenantSelfService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtiene mi establecimiento")
    public TenantResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(callerTenant(jwt));
    }

    @PutMapping
    @Operation(summary = "Modifica mi establecimiento (id tributario, direccion, descripcion, ubicacion)")
    public TenantResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TenantContactUpdateRequest request) {
        return service.updateContact(callerTenant(jwt), request);
    }

    @GetMapping("/members")
    @Operation(summary = "Lista las membresías de mi establecimiento")
    public Page<MembershipResponse> listMembers(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return service.listMembers(callerTenant(jwt), pageable);
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta a un usuario como TENANT_PARTNER en mi establecimiento")
    public MembershipResponse addPartner(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PartnerCreateRequest request) {
        return service.addPartner(callerTenant(jwt), request);
    }

    @DeleteMapping("/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoca la membresía de un TENANT_PARTNER de mi establecimiento")
    public void removePartner(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        service.removePartner(callerTenant(jwt), userId);
    }

    /** Tenant del llamante, tomado del token (no de input del cliente). */
    private static UUID callerTenant(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenant context in token");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid tenant context");
        }
    }
}
