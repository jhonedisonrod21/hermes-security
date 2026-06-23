package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.tenant.GeoLocation;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Vista de un establecimiento (tenant).")
public record TenantResponse(
        UUID id,
        String slug,
        String name,
        String taxId,
        String country,
        String city,
        String address,
        String description,
        String timeZone,
        GeoPointDto location,
        TenantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TenantResponse from(Tenant tenant) {
        GeoLocation location = tenant.getLocation();
        GeoPointDto point = location == null || location.getLatitude() == null
                ? null
                : new GeoPointDto(location.getLatitude(), location.getLongitude());
        return new TenantResponse(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.getTaxId(),
                tenant.getCountry(),
                tenant.getCity(),
                tenant.getAddress(),
                tenant.getDescription(),
                tenant.getTimeZone(),
                point,
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
