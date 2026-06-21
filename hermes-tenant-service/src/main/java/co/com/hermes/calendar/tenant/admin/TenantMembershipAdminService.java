package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.role.TenantRoleRepository;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * Gestión de membresías (asociación usuario↔establecimiento + rol de tenant) por el SYSTEM_ADMIN.
 * Es el alta/baja de un usuario ya registrado como TENANT_ADMIN (o TENANT_PARTNER) de un tenant.
 */
@Service
public class TenantMembershipAdminService {

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final TenantRoleRepository roles;

    public TenantMembershipAdminService(
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            TenantRoleRepository roles
    ) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.roles = roles;
    }

    @Transactional
    public MembershipResponse addMember(UUID tenantId, MembershipCreateRequest request) {
        Tenant tenant = requireTenant(tenantId);
        if (memberships.existsByTenant_IdAndUserId(tenantId, request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this tenant");
        }
        TenantRole role = roles.findByName(request.role().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tenant role"));

        TenantMembership membership = TenantMembership.activeMember(UUID.randomUUID(), request.userId(), tenant, role);
        return MembershipResponse.from(memberships.save(membership));
    }

    @Transactional(readOnly = true)
    public Page<MembershipResponse> listMembers(UUID tenantId, Pageable pageable) {
        requireTenant(tenantId);
        return memberships.findByTenant_Id(tenantId, pageable).map(MembershipResponse::from);
    }

    /** Revoca la membresía (y con ella el rol de tenant y la asociación usuario→tenant). */
    @Transactional
    public void removeMember(UUID tenantId, UUID userId) {
        TenantMembership membership = memberships.findByTenant_IdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        memberships.delete(membership);
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenants.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }
}
