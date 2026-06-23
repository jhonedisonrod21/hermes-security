package co.com.hermes.calendar.tenant.self;

import co.com.hermes.calendar.tenant.admin.MembershipResponse;
import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.role.TenantRoleRepository;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantSelfServiceTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
    private final TenantRoleRepository roles = mock(TenantRoleRepository.class);
    private final TenantSelfService service = new TenantSelfService(tenants, memberships, roles);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Tenant tenant() {
        return Tenant.register(tenantId, "cafe-central", "Cafe Central", "900123456-7", "CO", "Bogota", null, null, null);
    }

    private TenantRole role(String name) {
        TenantRole role = mock(TenantRole.class);
        when(role.getName()).thenReturn(name);
        return role;
    }

    @Test
    void addsPartnerToOwnTenant() {
        TenantRole partner = role("TENANT_PARTNER");
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(memberships.existsByTenant_IdAndUserId(tenantId, userId)).thenReturn(false);
        when(roles.findByName("TENANT_PARTNER")).thenReturn(Optional.of(partner));
        when(memberships.save(any(TenantMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipResponse response = service.addPartner(tenantId, new PartnerCreateRequest(userId));

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.roles()).containsExactly("TENANT_PARTNER");
    }

    @Test
    void rejectsDuplicatePartner() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(memberships.existsByTenant_IdAndUserId(tenantId, userId)).thenReturn(true);

        assertThatThrownBy(() -> service.addPartner(tenantId, new PartnerCreateRequest(userId)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void revokesPartner() {
        TenantRole partner = role("TENANT_PARTNER");
        TenantMembership membership = mock(TenantMembership.class);
        when(membership.getRoles()).thenReturn(Set.of(partner));
        when(memberships.findByTenant_IdAndUserId(tenantId, userId)).thenReturn(Optional.of(membership));

        service.removePartner(tenantId, userId);

        verify(memberships).delete(membership);
    }

    @Test
    void refusesToRevokeATenantAdmin() {
        TenantRole admin = role("TENANT_ADMIN");
        TenantMembership membership = mock(TenantMembership.class);
        when(membership.getRoles()).thenReturn(Set.of(admin));
        when(memberships.findByTenant_IdAndUserId(tenantId, userId)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.removePartner(tenantId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN);
        verify(memberships, never()).delete(any());
    }

    @Test
    void updatesContactInfo() {
        Tenant tenant = tenant();
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));

        var response = service.updateContact(tenantId,
                new TenantContactUpdateRequest("900123456-7", "Nueva direccion", "Cafe renovado", "America/Bogota", null));

        assertThat(response.address()).isEqualTo("Nueva direccion");
        assertThat(response.description()).isEqualTo("Cafe renovado");
        assertThat(response.timeZone()).isEqualTo("America/Bogota");
        assertThat(tenant.getAddress()).isEqualTo("Nueva direccion");
    }
}
