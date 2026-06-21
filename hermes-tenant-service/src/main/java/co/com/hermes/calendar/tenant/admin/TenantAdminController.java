package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestión de establecimientos por el administrador del sistema. Tras el gateway (StripPrefix=1)
 * estas rutas quedan bajo {@code /tenant/admin/tenants}. Solo accesible con rol SYSTEM_ADMIN.
 */
@RestController
@RequestMapping("/admin/tenants")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "Tenant administration", description = "Alta, edición y estado de establecimientos (solo SYSTEM_ADMIN).")
@SecurityRequirement(name = "bearer-jwt")
public class TenantAdminController {

    private final TenantAdminService service;

    public TenantAdminController(TenantAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un establecimiento")
    public TenantResponse create(@Valid @RequestBody TenantCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lista los establecimientos")
    public Page<TenantResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un establecimiento")
    public TenantResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un establecimiento")
    public TenantResponse update(@PathVariable UUID id, @Valid @RequestBody TenantUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambia el estado (ACTIVE/INACTIVE)")
    public TenantResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody TenantStatusUpdateRequest request) {
        return service.changeStatus(id, request);
    }
}
