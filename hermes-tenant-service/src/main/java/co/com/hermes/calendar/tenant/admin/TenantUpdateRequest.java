package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos modificables de un establecimiento (el estado se cambia aparte).")
public record TenantUpdateRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 40) String taxId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "country must be an ISO-3166 alpha-2 code") String country,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 200) String address,
        @Size(max = 500) String description,
        @Size(max = 60) @Schema(description = "Zona horaria IANA.", example = "America/Bogota") String timeZone,
        @Valid GeoPointDto location
) {
}
