package co.com.hermes.calendar.tenant.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Schema(description = "Empresa o tenant gestionado por Hermes.")
public class Tenant {

    @Id
    @Schema(description = "Identificador unico del tenant.", example = "00000000-0000-0000-0000-000000000010")
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    @Schema(description = "Slug unico usado en URLs, claims y seleccion de tenant.", example = "hermes-local")
    private String slug;

    @Column(nullable = false, length = 160)
    @Schema(description = "Nombre visible de la empresa.", example = "Hermes Local Company")
    private String name;

    @Column(nullable = false, length = 30)
    @Schema(description = "Estado operativo del tenant.", example = "ACTIVE")
    private String status;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Fecha de creacion del tenant.")
    private OffsetDateTime createdAt;

    protected Tenant() {
    }

    public static Tenant active(UUID id, String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.id = id;
        tenant.slug = slug;
        tenant.name = name;
        tenant.status = "ACTIVE";
        tenant.createdAt = OffsetDateTime.now();
        return tenant;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
