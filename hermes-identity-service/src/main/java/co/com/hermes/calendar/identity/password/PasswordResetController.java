package co.com.hermes.calendar.identity.password;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restablecimiento de contraseña, público (el usuario no está autenticado). Tras el gateway:
 * {@code /identity/users/password-reset/**}.
 */
@RestController
@RequestMapping("/users/password-reset")
@Tag(name = "Password reset", description = "Restablecimiento de contraseña por correo.")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicita el correo de restablecimiento",
            description = "Responde siempre 202, exista o no la cuenta (no revela si el correo está registrado). "
                    + "Si la cuenta existe y está activa, envía un correo con un enlace de un solo uso y caducidad.")
    @ApiResponse(responseCode = "202", description = "Solicitud aceptada (se envió el correo solo si la cuenta existe).",
            content = @Content)
    @ApiResponse(responseCode = "400", description = "Correo con formato inválido.", content = @Content)
    public void request(@Valid @RequestBody PasswordResetRequest request) {
        service.requestReset(request.email());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Fija la nueva contraseña con el token recibido",
            description = "Valida el token (existente, no usado y no caducado) y establece la nueva contraseña. "
                    + "El token queda consumido (un solo uso).")
    @ApiResponse(responseCode = "204", description = "Contraseña restablecida.", content = @Content)
    @ApiResponse(responseCode = "400", description = "Token inválido/caducado/ya usado, o contraseña que no cumple la política.",
            content = @Content)
    public void confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        service.confirmReset(request.token(), request.newPassword());
    }
}
