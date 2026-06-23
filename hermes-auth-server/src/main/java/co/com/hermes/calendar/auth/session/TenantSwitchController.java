package co.com.hermes.calendar.auth.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cambio de organización activa. {@code POST /session/switch-tenant} (público en el gateway, pero
 * exige el bearer actual; el auth-server lo valida). Devuelve un token nuevo con los roles/permisos y
 * el tenant de la organización elegida. El front reemplaza su token por el devuelto.
 */
@RestController
@RequestMapping("/session")
@Tag(name = "Tenant switch", description = "Re-emite el token para la organización activa elegida.")
@SecurityRequirement(name = "bearer-jwt")
public class TenantSwitchController {

    private final TenantSwitchService service;

    public TenantSwitchController(TenantSwitchService service) {
        this.service = service;
    }

    @PostMapping("/switch-tenant")
    @Operation(summary = "Cambia la organización activa y re-emite el token")
    public SwitchTokenResponse switchTenant(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SwitchTenantRequest request) {
        return service.switchTenant(jwt.getClaimAsString("preferred_username"), request.tenantId());
    }
}
