package co.com.hermes.calendar.tenant.membership;

import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenant_memberships")
@Schema(description = "Relacion entre un usuario y un tenant, con roles funcionales.")
public class TenantMembership {

    @Id
    @Schema(description = "Identificador unico de la membresia.")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "Identificador del usuario en Identity Service.", example = "00000000-0000-0000-0000-000000000100")
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 30)
    @Schema(description = "Estado de la membresia.", example = "ACTIVE")
    private String status;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Fecha de creacion de la membresia.")
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tenant_membership_roles",
            joinColumns = @JoinColumn(name = "membership_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<TenantRole> roles = new LinkedHashSet<>();

    protected TenantMembership() {
    }

    public static TenantMembership activeMember(UUID id, UUID userId, Tenant tenant, TenantRole role) {
        TenantMembership membership = new TenantMembership();
        membership.id = id;
        membership.userId = userId;
        membership.tenant = tenant;
        membership.status = "ACTIVE";
        membership.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        membership.roles.add(role);
        return membership;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public Set<TenantRole> getRoles() {
        return roles;
    }
}
