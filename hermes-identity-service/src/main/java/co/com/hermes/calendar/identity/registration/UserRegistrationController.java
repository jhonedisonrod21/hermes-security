package co.com.hermes.calendar.identity.registration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "User registration", description = "Registro publico de usuarios con correo y password.")
public class UserRegistrationController {

    private final UserRegistrationService registrations;

    public UserRegistrationController(UserRegistrationService registrations) {
        this.registrations = registrations;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra un usuario",
            description = "Crea una cuenta habilitada en Identity, asigna el rol global USER y aprovisiona un tenant "
                    + "inicial con membresia OWNER para que el usuario pueda iniciar sesion inmediatamente."
    )
    @ApiResponse(responseCode = "201", description = "Usuario registrado.",
            content = @Content(schema = @Schema(implementation = UserRegistrationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Solicitud invalida.", content = @Content)
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado.", content = @Content)
    public UserRegistrationResponse register(@Valid @RequestBody UserRegistrationRequest request) {
        return registrations.register(request);
    }
}
