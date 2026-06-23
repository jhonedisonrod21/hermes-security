package co.com.hermes.calendar.tenant.self;

import co.com.hermes.calendar.tenant.admin.GeoPointDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Campos que el TENANT_ADMIN puede modificar de su establecimiento.")
public record TenantContactUpdateRequest(
        @NotBlank @Size(max = 40) @Schema(example = "900123456-7") String taxId,
        @Size(max = 200) @Schema(example = "Cra 7 # 71-21") String address,
        @Size(max = 500) String description,
        @Size(max = 60) @Schema(description = "Zona horaria IANA.", example = "America/Bogota") String timeZone,
        @Valid GeoPointDto location
) {
}
