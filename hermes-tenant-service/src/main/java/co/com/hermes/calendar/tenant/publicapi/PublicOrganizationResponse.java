package co.com.hermes.calendar.tenant.publicapi;

import co.com.hermes.calendar.tenant.admin.GeoPointDto;
import co.com.hermes.calendar.tenant.tenant.GeoLocation;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Información pública de un establecimiento (sin datos sensibles: ni id tributario, ni estado interno). */
@Schema(description = "Datos públicos de un establecimiento para la vitrina/exploración.")
public record PublicOrganizationResponse(
        UUID id,
        String slug,
        String name,
        String country,
        String city,
        String address,
        String description,
        GeoPointDto location
) {

    public static PublicOrganizationResponse from(Tenant tenant) {
        GeoLocation location = tenant.getLocation();
        GeoPointDto point = location == null || location.getLatitude() == null
                ? null
                : new GeoPointDto(location.getLatitude(), location.getLongitude());
        return new PublicOrganizationResponse(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.getCountry(),
                tenant.getCity(),
                tenant.getAddress(),
                tenant.getDescription(),
                point);
    }
}
