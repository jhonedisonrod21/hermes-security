package co.com.hermes.calendar.identity.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud publica para registrar un usuario con correo y password.")
public record UserRegistrationRequest(
        @Schema(description = "Nombre visible del usuario.", example = "Ana Gómez")
        @NotBlank
        @Size(min = 2, max = 120)
        // Letras Unicode (con acentos/marcas), espacios y . ' - ; debe empezar por letra.
        // Rechaza dígitos, caracteres de control, emojis y símbolos.
        @Pattern(regexp = "^\\p{L}[\\p{L}\\p{M} .'-]{1,119}$",
                message = "El nombre solo admite letras, espacios y los signos . ' -")
        String name,
        @Schema(description = "Correo electronico que sera usado como username.", example = "ana@acme.test")
        @NotBlank
        @Size(max = 254)
        // Formato práctico: parte local sin caracteres extraños y dominio con TLD de 2+ letras.
        // Más estricto que @Email (que acepta a@b sin TLD). Sin cuantificadores anidados (evita ReDoS).
        @Pattern(regexp = "^[A-Za-z0-9._+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "El correo no tiene un formato válido")
        String email,
        @Schema(description = "Password del usuario. Minimo 8 caracteres.", example = "localPass123")
        @NotBlank
        @Size(min = 8, max = 128)
        String password,
        @Schema(description = "Telefono de contacto para notificaciones por SMS (opcional).", example = "+573001112233")
        @Size(max = 40)
        // Opcional: si viene, dígitos con + inicial opcional y separadores; el vacío también se admite.
        @Pattern(regexp = "^$|^\\+?\\d[\\d ()-]{6,19}$",
                message = "El teléfono solo admite dígitos, espacios y los signos + ( ) -")
        String phone
) {
}
