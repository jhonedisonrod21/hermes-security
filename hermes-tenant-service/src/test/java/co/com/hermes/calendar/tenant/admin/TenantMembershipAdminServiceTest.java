package co.com.hermes.calendar.tenant.admin;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantMembershipAdminServiceTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
    private final TenantRoleRepository roles = mock(TenantRoleRepository.class);
    private final TenantMembershipAdminService service =
            new TenantMembershipAdminService(tenants, memberships, roles);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Tenant tenant() {
        return Tenant.active(tenantId, "cafe-central", "Cafe Central");
    }

    @Test
    void addsUserAsTenantAdmin() {
        TenantRole role = mock(TenantRole.class);
        when(role.getName()).thenReturn("TENANT_ADMIN");
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(memberships.existsByTenant_IdAndUserId(tenantId, userId)).thenReturn(false);
        when(roles.findByName("TENANT_ADMIN")).thenReturn(Optional.of(role));
        when(memberships.save(any(TenantMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipResponse response = service.addMember(tenantId, new MembershipCreateRequest(userId, "tenant_admin"));

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.roles()).containsExactly("TENANT_ADMIN");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsDuplicateMembership() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(memberships.existsByTenant_IdAndUserId(tenantId, userId)).thenReturn(true);

        var request = new MembershipCreateRequest(userId, "TENANT_ADMIN");
        assertThatThrownBy(() -> service.addMember(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsUnknownRole() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(memberships.existsByTenant_IdAndUserId(tenantId, userId)).thenReturn(false);
        when(roles.findByName("WIZARD")).thenReturn(Optional.empty());

        var request = new MembershipCreateRequest(userId, "wizard");
        assertThatThrownBy(() -> service.addMember(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsMembershipForMissingTenant() {
        when(tenants.findById(tenantId)).thenReturn(Optional.empty());

        var request = new MembershipCreateRequest(userId, "TENANT_ADMIN");
        assertThatThrownBy(() -> service.addMember(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void revokesMembership() {
        TenantMembership membership = mock(TenantMembership.class);
        when(memberships.findByTenant_IdAndUserId(tenantId, userId)).thenReturn(Optional.of(membership));

        service.removeMember(tenantId, userId);

        verify(memberships).delete(membership);
    }

    @Test
    void failsToRevokeMissingMembership() {
        when(memberships.findByTenant_IdAndUserId(tenantId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(tenantId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberships, never()).delete(any());
    }
}
