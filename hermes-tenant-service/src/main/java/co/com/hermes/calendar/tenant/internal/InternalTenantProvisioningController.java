package co.com.hermes.calendar.tenant.internal;

import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.role.TenantRoleRepository;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import co.com.hermes.calendar.shared.contract.TenantProvisioningRequest;
import co.com.hermes.calendar.shared.contract.TenantProvisioningResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/internal/registrations")
@Tag(name = "Internal tenant provisioning", description = "Endpoints internos para aprovisionar tenants durante el registro.")
public class InternalTenantProvisioningController {

    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String INTERNAL_KEY_HEADER = HermesInternalHeaders.INTERNAL_KEY;

    private final InternalTenantProperties properties;
    private final TenantRepository tenants;
    private final TenantRoleRepository roles;
    private final TenantMembershipRepository memberships;

    public InternalTenantProvisioningController(
            InternalTenantProperties properties,
            TenantRepository tenants,
            TenantRoleRepository roles,
            TenantMembershipRepository memberships
    ) {
        this.properties = properties;
        this.tenants = tenants;
        this.roles = roles;
        this.memberships = memberships;
    }

    @PostMapping("/default-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(
            summary = "Aprovisiona tenant inicial",
            description = "Crea un tenant para un usuario y le asigna una membresia ACTIVE con rol TENANT_ADMIN.",
            security = @SecurityRequirement(name = "hermes-internal-key")
    )
    @ApiResponse(responseCode = "201", description = "Tenant aprovisionado.",
            content = @Content(schema = @Schema(implementation = TenantProvisioningResponse.class)))
    @ApiResponse(responseCode = "401", description = "Llave interna invalida.", content = @Content)
    public TenantProvisioningResponse provisionDefaultTenant(
            @Parameter(name = INTERNAL_KEY_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "Llave compartida para llamadas internas entre microservicios.")
            @RequestHeader(name = INTERNAL_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody TenantProvisioningRequest request
    ) {
        assertInternalKey(apiKey);

        TenantRole adminRole = roles.findByName(TENANT_ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_ADMIN role is not configured"));

        UUID tenantId = UUID.randomUUID();
        String slug = uniqueSlug(request.userId());
        String tenantName = tenantName(request.email());
        Tenant tenant = tenants.save(Tenant.active(tenantId, slug, tenantName));
        memberships.save(TenantMembership.activeMember(UUID.randomUUID(), request.userId(), tenant, adminRole));

        return new TenantProvisioningResponse(tenant.getId(), tenant.getSlug(), tenant.getName());
    }

    private void assertInternalKey(String apiKey) {
        if (!HermesInternalKeys.matches(properties.apiKey(), apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }

    private String uniqueSlug(UUID userId) {
        String baseSlug = "user-" + userId.toString().substring(0, 8);
        String slug = baseSlug;
        int suffix = 2;
        while (tenants.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }
        return slug;
    }

    private String tenantName(String email) {
        if (!StringUtils.hasText(email)) {
            return "Hermes Workspace";
        }
        String localPart = email.split("@")[0].replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (!StringUtils.hasText(localPart)) {
            return "Hermes Workspace";
        }
        return localPart.substring(0, 1).toUpperCase(Locale.ROOT) + localPart.substring(1) + " Workspace";
    }
}
