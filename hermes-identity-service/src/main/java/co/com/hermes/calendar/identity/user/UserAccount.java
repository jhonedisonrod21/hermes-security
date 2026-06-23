package co.com.hermes.calendar.identity.user;

import co.com.hermes.calendar.identity.role.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "identity_users")
@Schema(description = "Cuenta de usuario gestionada por Identity Service.")
public class UserAccount {

    @Id
    @Schema(description = "Identificador unico del usuario.", example = "00000000-0000-0000-0000-000000000100")
    private UUID id;

    @Column(name = "tenant_id")
    @Schema(description = "Tenant principal asociado desde Identity.", example = "00000000-0000-0000-0000-000000000010")
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 160)
    @Schema(description = "Username canonico del usuario.", example = "admin@hermes.local")
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    @Schema(description = "Email del usuario.", example = "admin@hermes.local")
    private String email;

    @Column(length = 40)
    @Schema(description = "Telefono de contacto (para notificaciones por SMS).", example = "+573001112233")
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    @Schema(description = "Hash de password. No debe exponerse en APIs publicas.", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String passwordHash;

    @Column(nullable = false)
    @Schema(description = "Indica si la cuenta puede autenticarse.", example = "true")
    private boolean enabled;

    @Column(nullable = false)
    @Schema(description = "Indica si la cuenta esta bloqueada.", example = "false")
    private boolean locked;

    @Column(name = "created_at", nullable = false)
    @Schema(description = "Fecha de creacion de la cuenta.")
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "identity_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    protected UserAccount() {
    }

    public static UserAccount registeredUser(UUID id, String email, String passwordHash, Role role) {
        UserAccount user = new UserAccount();
        user.id = id;
        user.username = email;
        user.email = email;
        user.passwordHash = passwordHash;
        user.enabled = true;
        user.locked = false;
        user.createdAt = OffsetDateTime.now();
        user.roles.add(role);
        return user;
    }

    public void assignTenant(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /** Fija el username (handle) del usuario; en el registro se deriva de la parte local del correo. */
    public void assignUsername(String username) {
        this.username = username;
    }

    /** Fija el teléfono de contacto (normalizado por el llamante). */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Actualiza el perfil editable por el administrador del sistema. */
    public void updateProfile(String username, String email, String phone) {
        this.username = username;
        this.email = email;
        this.phone = phone;
    }

    /** Bloquea o desbloquea la cuenta (un usuario bloqueado no puede autenticarse). */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /** Reemplaza el hash de la contraseña (el llamante ya lo codificó). */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
