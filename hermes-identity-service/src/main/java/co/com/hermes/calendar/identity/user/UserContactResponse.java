package co.com.hermes.calendar.identity.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Datos de contacto de un usuario, para que el servicio de notificaciones le haga llegar avisos. */
@Schema(description = "Contacto de un usuario (email y telefono).")
public record UserContactResponse(UUID userId, String email, String phone) {

    public static UserContactResponse from(UserAccount user) {
        return new UserContactResponse(user.getId(), user.getEmail(), user.getPhone());
    }
}
