package co.com.hermes.calendar.identity.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
