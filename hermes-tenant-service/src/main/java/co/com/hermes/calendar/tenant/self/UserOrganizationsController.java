package co.com.hermes.calendar.tenant.self;

import co.com.hermes.calendar.tenant.membership.TenantMembership;
import co.com.hermes.calendar.tenant.membership.TenantMembershipRepository;
import co.com.hermes.calendar.tenant.role.TenantRole;
import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Organizaciones a las que pertenece el usuario autenticado. Tras el gateway: {@code /tenant/me/organizations}.
 * Accesible a <b>cualquier usuario autenticado</b> (no depende del tenant activo ni del rol): se basa en el
 * id de usuario del token. Es la base para que el front muestre el selector de organización y haga el cambio.
 */
@RestController
@RequestMapping("/me/organizations")
@Tag(name = "My organizations", description = "Organizaciones del usuario (para cambiar de tenant activo).")
@SecurityRequirement(name = "bearer-jwt")
public class UserOrganizationsController {

    private static final String ACTIVE = "ACTIVE";

    private final TenantMembershipRepository memberships;

    public UserOrganizationsController(TenantMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @GetMapping
    @Operation(summary = "Lista las organizaciones a las que pertenezco")
    public List<OrganizationResponse> myOrganizations(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = callerUserId(jwt);
        return memberships.findByUserIdAndStatusOrderByCreatedAtAsc(userId, ACTIVE).stream()
                .filter(m -> m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .map(UserOrganizationsController::toResponse)
                .toList();
    }

    private static OrganizationResponse toResponse(TenantMembership membership) {
        List<String> roles = membership.getRoles().stream().map(TenantRole::getName).sorted().toList();
        return new OrganizationResponse(
                membership.getTenant().getId(),
                membership.getTenant().getSlug(),
                membership.getTenant().getName(),
                roles);
    }

    private static UUID callerUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user identity");
        }
    }
}
