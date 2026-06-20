package co.com.hermes.calendar.auth.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class HermesUserPrincipal extends User {

    private final UUID userId;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String tenantName;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;

    public HermesUserPrincipal(
            UUID userId,
            UUID tenantId,
            String tenantSlug,
            String tenantName,
            String username,
            String email,
            List<String> roles,
            List<String> permissions,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, "N/A", true, true, true, true, authorities);
        this.userId = userId;
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.tenantName = tenantName;
        this.email = email;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    public UUID getUserId() {
        return userId;
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

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
