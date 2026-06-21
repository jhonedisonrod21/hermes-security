package co.com.hermes.calendar.tenant.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Ubicacion georreferenciada (WGS84). O ambos valores o ninguno.")
public record GeoPointDto(
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        @Schema(example = "4.710989") BigDecimal latitude,

        @NotNull @DecimalMin("-180") @DecimalMax("180")
        @Schema(example = "-74.072092") BigDecimal longitude
) {
}
