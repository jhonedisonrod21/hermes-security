package co.com.hermes.calendar.tenant.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenant_roles")
@Schema(description = "Rol asignable dentro de un tenant, con permisos funcionales.")
public class TenantRole {

    @Id
    @Schema(description = "Identificador unico del rol de tenant.")
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    @Schema(description = "Nombre canonico del rol.", example = "OWNER")
    private String name;

    @Column(nullable = false, length = 200)
    @Schema(description = "Descripcion funcional del rol.", example = "Propietario del tenant")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 120)
    @Schema(description = "Permisos efectivos entregados por este rol.",
            example = "[\"tenant:manage\",\"users:manage\",\"calendar:read\"]")
    private Set<String> permissions = new LinkedHashSet<>();

    protected TenantRole() {
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
