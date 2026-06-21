package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un establecimiento (tenant).")
public record TenantCreateRequest(
        @NotBlank @Size(max = 160) @Schema(example = "Cafe Central") String name,
        @NotBlank @Size(max = 40) @Schema(example = "900123456-7") String taxId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "country must be an ISO-3166 alpha-2 code")
        @Schema(example = "CO") String country,
        @NotBlank @Size(max = 120) @Schema(example = "Bogota") String city,
        @Size(max = 200) @Schema(example = "Cra 7 # 71-21") String address,
        @Size(max = 500) String description,
        @Valid GeoPointDto location
) {
}
