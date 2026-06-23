package co.com.hermes.calendar.identity.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Campos editables por el propio usuario. El teléfono habilita los recordatorios por SMS. */
@Schema(description = "Datos editables del propio perfil.")
public record SelfProfileUpdateRequest(
        @Size(max = 40) @Schema(description = "Teléfono de contacto (SMS).", example = "+573001112233") String phone
) {
}
