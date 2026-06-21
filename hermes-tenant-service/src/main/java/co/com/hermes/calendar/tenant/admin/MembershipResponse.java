package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.role.TenantRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Membresía de un usuario en un establecimiento.")
public record MembershipResponse(
        UUID id,
        UUID userId,
        UUID tenantId,
        String tenantSlug,
        List<String> roles,
        String status,
        OffsetDateTime createdAt
) {

    public static MembershipResponse from(TenantMembership membership) {
        List<String> roles = membership.getRoles().stream().map(TenantRole::getName).sorted().toList();
        return new MembershipResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getTenant().getId(),
                membership.getTenant().getSlug(),
                roles,
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }
}
