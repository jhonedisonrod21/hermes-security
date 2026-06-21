package co.com.hermes.calendar.identity.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestión de cuentas de usuario por el administrador del sistema. Tras el gateway (StripPrefix=1)
 * queda bajo {@code /identity/admin/users}. Solo accesible con rol SYSTEM_ADMIN.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "User administration", description = "Listado, edición y bloqueo de usuarios (solo SYSTEM_ADMIN).")
@SecurityRequirement(name = "bearer-jwt")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios (filtro opcional por username/email)")
    public Page<UserResponse> list(@RequestParam(name = "q", required = false) String query, Pageable pageable) {
        return service.list(query, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario")
    public UserResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un usuario")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/lock")
    @Operation(summary = "Bloquea o desbloquea un usuario")
    public UserResponse changeLock(@PathVariable UUID id, @Valid @RequestBody UserLockRequest request) {
        return service.changeLock(id, request.locked());
    }
}
