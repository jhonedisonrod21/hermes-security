package co.com.hermes.calendar.tenant.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Información pública de un establecimiento para la vitrina/exploración (sin sesión). Tras el gateway
 * (StripPrefix=1) queda bajo {@code /tenant/public}. Solo expone datos no sensibles de tenants activos.
 */
@RestController
@RequestMapping("/public")
@Tag(name = "Public organization", description = "Información pública de establecimientos (sin autenticación).")
public class PublicOrganizationController {

    private final PublicOrganizationService service;

    public PublicOrganizationController(PublicOrganizationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Datos públicos de un establecimiento activo (incluida su ubicación)")
    public PublicOrganizationResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
