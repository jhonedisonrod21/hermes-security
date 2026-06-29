package co.com.hermes.calendar.tenant.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Schema(description = "Establecimiento (tenant) gestionado por Hermes.")
public class Tenant {

    @Id
    @Schema(description = "Identificador unico del tenant.", example = "00000000-0000-0000-0000-000000000010")
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    @Schema(description = "Slug unico usado en URLs, claims y seleccion de tenant.", example = "hermes-local")
    private String slug;

    @Column(nullable = false, length = 160)
    @Schema(description = "Nombre visible del establecimiento.", example = "Hermes Local Company")
    private String name;

    @Column(name = "tax_id", unique = true, length = 40)
    @Schema(description = "Numero de identificacion tributaria.", example = "900123456-7")
    private String taxId;

    @Column(length = 80)
    @Schema(description = "Pais (ISO-3166 alpha-2).", example = "CO")
    private String country;

    @Column(length = 120)
    @Schema(description = "Ciudad.", example = "Bogota")
    private String city;

    @Column(length = 200)
    @Schema(description = "Direccion.", example = "Cra 7 # 71-21")
    private String address;

    @Column(length = 500)
    @Schema(description = "Descripcion del establecimiento.")
    private String description;

    @Column(name = "time_zone", length = 60)
    @Schema(description = "Zona horaria IANA (disponibilidad y recordatorios).", example = "America/Bogota")
    private String timeZone;

    @Embedded
    @Schema(description = "Ubicacion georreferenciada (opcional).")
    private GeoLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Schema(description = "Estado operativo del tenant.", example = "ACTIVE")
    private TenantStatus status;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Fecha de creacion del tenant.")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Fecha de ultima modificacion.")
    private OffsetDateTime updatedAt;

    protected Tenant() {
    }

    /** Alta de un establecimiento por el administrador del sistema. */
    public static Tenant register(
            UUID id,
            String slug,
            String name,
            String taxId,
            String country,
            String city,
            String address,
            String description,
            GeoLocation location
    ) {
        Tenant tenant = new Tenant();
        tenant.id = id;
        tenant.slug = slug;
        tenant.name = name;
        tenant.taxId = taxId;
        tenant.country = country;
        tenant.city = city;
        tenant.address = address;
        tenant.description = description;
        tenant.location = location;
        tenant.status = TenantStatus.ACTIVE;
        tenant.createdAt = OffsetDateTime.now();
        tenant.updatedAt = tenant.createdAt;
        return tenant;
    }

    /** Tenant mínimo activo (usado por el aprovisionamiento interno; sin datos de negocio). */
    public static Tenant active(UUID id, String slug, String name) {
        Tenant tenant = new Tenant();
        tenant.id = id;
        tenant.slug = slug;
        tenant.name = name;
        tenant.status = TenantStatus.ACTIVE;
        tenant.createdAt = OffsetDateTime.now();
        tenant.updatedAt = tenant.createdAt;
        return tenant;
    }

    public void update(
            String name,
            String taxId,
            String country,
            String city,
            String address,
            String description,
            String timeZone,
            GeoLocation location
    ) {
        this.name = name;
        this.taxId = taxId;
        this.country = country;
        this.city = city;
        this.address = address;
        this.description = description;
        this.timeZone = timeZone;
        this.location = location;
        this.updatedAt = OffsetDateTime.now();
    }

    public void changeStatus(TenantStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    /** Edición acotada que puede hacer el TENANT_ADMIN sobre su propio establecimiento. */
    public void editContactInfo(String taxId, String city, String address, String description, String timeZone, GeoLocation location) {
        this.taxId = taxId;
        this.city = city;
        this.address = address;
        this.description = description;
        this.timeZone = timeZone;
        this.location = location;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getTimeZone() {
        return timeZone;
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

    public String getTaxId() {
        return taxId;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
