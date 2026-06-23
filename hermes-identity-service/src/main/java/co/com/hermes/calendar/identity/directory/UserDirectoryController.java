package co.com.hermes.calendar.identity.directory;

import co.com.hermes.calendar.identity.user.UserAccount;
import co.com.hermes.calendar.identity.user.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Directorio de usuarios para los operadores del establecimiento (TENANT_ADMIN / TENANT_PARTNER):
 * resuelve un UUID de usuario a su ficha (username, correo) para no mostrar el id en las pantallas
 * de citas y pagos. Solo expone datos mínimos de identificación; el tenant sale del JWT del llamante.
 */
@RestController
@RequestMapping("/directory/users")
@PreAuthorize("hasAnyAuthority('calendar:read','calendar:write')")
@Tag(name = "User directory", description = "Resolución de usuarios por id para el personal del establecimiento.")
@SecurityRequirement(name = "bearer-jwt")
public class UserDirectoryController {

    private static final int MAX_BATCH = 100;

    private final UserAccountRepository users;

    public UserDirectoryController(UserAccountRepository users) {
        this.users = users;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha de un usuario por id (nombre y correo)")
    public UserCardResponse get(@PathVariable UUID id) {
        return users.findById(id)
                .map(UserCardResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping
    @Operation(summary = "Fichas de varios usuarios por id (resolución en lote para listados)")
    public List<UserCardResponse> batch(@RequestParam(name = "ids", required = false) List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_BATCH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demasiados ids (máximo " + MAX_BATCH + ")");
        }
        return users.findAllById(ids).stream().map(UserCardResponse::from).toList();
    }
}
