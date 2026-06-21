package co.com.hermes.calendar.identity.role;

import co.com.hermes.calendar.shared.security.AccountScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "identity_roles")
@Schema(description = "Rol global asignable a usuarios dentro de Identity Service.")
public class Role {

    @Id
    @Schema(description = "Identificador unico del rol.", example = "00000000-0000-0000-0000-000000000001")
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    @Schema(description = "Nombre canonico del rol.", example = "ADMIN")
    private String name;

    @Column(nullable = false, length = 200)
    @Schema(description = "Descripcion funcional del rol.", example = "Administrador global de Hermes")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Schema(description = "Alcance del rol: PLATFORM (global, sin tenant) o TENANT.", example = "PLATFORM")
    private AccountScope scope = AccountScope.TENANT;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "identity_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 120)
    @Schema(description = "Permisos efectivos entregados por este rol.",
            example = "[\"platform:tenants:manage\",\"platform:users:manage\"]")
    private Set<String> permissions = new LinkedHashSet<>();

    protected Role() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AccountScope getScope() {
        return scope;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
