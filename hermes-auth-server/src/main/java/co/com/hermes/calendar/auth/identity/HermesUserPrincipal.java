package co.com.hermes.calendar.auth.identity;

import co.com.hermes.calendar.shared.security.AccountScope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HermesUserPrincipal extends User {

    /** Establecimiento al que pertenece el principal; todos los campos null si es de plataforma. */
    public record TenantRef(UUID id, String slug, String name) {
    }

    /** Atributos humanos del usuario (login, contacto y nombre visible). */
    public record UserProfile(String username, String email, String name) {
    }

    private final UUID userId;
    private final AccountScope scope;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String tenantName;
    private final String email;
    private final String name;
    private final List<String> roles;
    private final List<String> permissions;

    public HermesUserPrincipal(
            UUID userId,
            AccountScope scope,
            TenantRef tenant,
            UserProfile profile,
            List<String> roles,
            List<String> permissions,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(profile.username(), "N/A", true, true, true, true, authorities);
        this.userId = userId;
        this.scope = scope;
        this.tenantId = tenant.id();
        this.tenantSlug = tenant.slug();
        this.tenantName = tenant.name();
        this.email = profile.email();
        this.name = profile.name();
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    public UUID getUserId() {
        return userId;
    }

    public AccountScope getScope() {
        return scope;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    // La identidad real es el userId; User.equals solo compara username, insuficiente en multi-tenant.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HermesUserPrincipal that) || !super.equals(o)) {
            return false;
        }
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId);
    }
}
