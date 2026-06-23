package co.com.hermes.calendar.tenant.self;

import co.com.hermes.calendar.tenant.admin.MembershipResponse;
import co.com.hermes.calendar.tenant.admin.TenantResponse;
import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.role.TenantRoleRepository;
import co.com.hermes.calendar.tenant.tenant.GeoLocation;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Operaciones que un TENANT_ADMIN realiza sobre <b>su propio</b> establecimiento. El {@code tenantId}
 * siempre proviene del token (claim {@code tenant_id}), nunca de input del cliente, de modo que un
 * administrador no puede actuar sobre un tenant ajeno. Solo puede gestionar membresías TENANT_PARTNER.
 */
@Service
public class TenantSelfService {

    private static final String PARTNER_ROLE = "TENANT_PARTNER";
    private static final String ADMIN_ROLE = "TENANT_ADMIN";

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final TenantRoleRepository roles;

    public TenantSelfService(
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            TenantRoleRepository roles
    ) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID tenantId) {
        return TenantResponse.from(requireTenant(tenantId));
    }

    @Transactional
    public TenantResponse updateContact(UUID tenantId, TenantContactUpdateRequest request) {
        Tenant tenant = requireTenant(tenantId);
        String taxId = request.taxId().trim();
        if (!taxId.equalsIgnoreCase(tenant.getTaxId()) && tenants.existsByTaxId(taxId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax id already registered");
        }
        GeoLocation location = request.location() == null
                ? null
                : new GeoLocation(request.location().latitude(), request.location().longitude());
        tenant.editContactInfo(taxId, trimOrNull(request.address()), trimOrNull(request.description()),
                normalizeZone(request.timeZone()), location);
        return TenantResponse.from(tenant);
    }

    /** Valida que la zona horaria sea un IANA ZoneId; devuelve null si viene vacía. */
    private static String normalizeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return null;
        }
        String trimmed = timeZone.trim();
        try {
            java.time.ZoneId.of(trimmed);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time zone: " + trimmed);
        }
        return trimmed;
    }

    @Transactional(readOnly = true)
    public Page<MembershipResponse> listMembers(UUID tenantId, Pageable pageable) {
        requireTenant(tenantId);
        return memberships.findByTenant_Id(tenantId, pageable).map(MembershipResponse::from);
    }

    @Transactional
    public MembershipResponse addPartner(UUID tenantId, PartnerCreateRequest request) {
        Tenant tenant = requireTenant(tenantId);
        if (memberships.existsByTenant_IdAndUserId(tenantId, request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this tenant");
        }
        TenantRole partner = roles.findByName(PARTNER_ROLE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_PARTNER role is not configured"));
        TenantMembership membership = TenantMembership.activeMember(UUID.randomUUID(), request.userId(), tenant, partner);
        return MembershipResponse.from(memberships.save(membership));
    }

    /** Revoca a un TENANT_PARTNER. No permite revocar a otro TENANT_ADMIN. */
    @Transactional
    public void removePartner(UUID tenantId, UUID userId) {
        TenantMembership membership = memberships.findByTenant_IdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        boolean isAdmin = membership.getRoles().stream().map(TenantRole::getName).anyMatch(ADMIN_ROLE::equals);
        if (isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot revoke a tenant administrator");
        }
        memberships.delete(membership);
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenants.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
