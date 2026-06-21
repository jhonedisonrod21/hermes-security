package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestión de membresías de un establecimiento por el administrador del sistema. Tras el gateway
 * (StripPrefix=1) queda bajo {@code /tenant/admin/tenants/{tenantId}/members}. Solo SYSTEM_ADMIN.
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/members")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "Tenant membership administration", description = "Alta/baja de usuarios en establecimientos (solo SYSTEM_ADMIN).")
@SecurityRequirement(name = "bearer-jwt")
public class TenantMembershipAdminController {

    private final TenantMembershipAdminService service;

    public TenantMembershipAdminController(TenantMembershipAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta a un usuario en el establecimiento (p. ej. como TENANT_ADMIN)")
    public MembershipResponse addMember(@PathVariable UUID tenantId, @Valid @RequestBody MembershipCreateRequest request) {
        return service.addMember(tenantId, request);
    }

    @GetMapping
    @Operation(summary = "Lista las membresías del establecimiento")
    public Page<MembershipResponse> listMembers(@PathVariable UUID tenantId, Pageable pageable) {
        return service.listMembers(tenantId, pageable);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoca la membresía (elimina la asociacion usuario->tenant)")
    public void removeMember(@PathVariable UUID tenantId, @PathVariable UUID userId) {
        service.removeMember(tenantId, userId);
    }
}
