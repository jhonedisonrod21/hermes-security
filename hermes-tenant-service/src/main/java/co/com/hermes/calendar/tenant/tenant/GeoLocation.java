package co.com.hermes.calendar.tenant.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Ubicación georreferenciada del establecimiento (latitud/longitud, WGS84). Es opcional como
 * conjunto: o se definen ambos valores o ninguno.
 */
@Embeddable
public class GeoLocation {

    @Column(precision = 9, scale = 6)
    @Schema(description = "Latitud (WGS84).", example = "4.710989")
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    @Schema(description = "Longitud (WGS84).", example = "-74.072092")
    private BigDecimal longitude;

    protected GeoLocation() {
    }

    public GeoLocation(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }
}
