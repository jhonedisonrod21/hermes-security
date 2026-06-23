package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.tenant.GeoLocation;
import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * Gestión de establecimientos (tenants) por el administrador del sistema.
 */
@Service
public class TenantAdminService {

    private static final int SLUG_MAX = 80;

    private final TenantRepository tenants;

    public TenantAdminService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional
    public TenantResponse create(TenantCreateRequest request) {
        String taxId = request.taxId().trim();
        if (tenants.existsByTaxId(taxId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax id already registered");
        }
        Tenant tenant = Tenant.register(
                UUID.randomUUID(),
                uniqueSlug(request.name()),
                request.name().trim(),
                taxId,
                normalizeCountry(request.country()),
                request.city().trim(),
                trimOrNull(request.address()),
                trimOrNull(request.description()),
                toGeoLocation(request.location())
        );
        return TenantResponse.from(tenants.save(tenant));
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> list(Pageable pageable) {
        return tenants.findAll(pageable).map(TenantResponse::from);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return TenantResponse.from(require(id));
    }

    @Transactional
    public TenantResponse update(UUID id, TenantUpdateRequest request) {
        Tenant tenant = require(id);
        String taxId = request.taxId().trim();
        if (!taxId.equalsIgnoreCase(tenant.getTaxId()) && tenants.existsByTaxId(taxId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax id already registered");
        }
        tenant.update(
                request.name().trim(),
                taxId,
                normalizeCountry(request.country()),
                request.city().trim(),
                trimOrNull(request.address()),
                trimOrNull(request.description()),
                normalizeZone(request.timeZone()),
                toGeoLocation(request.location())
        );
        return TenantResponse.from(tenant);
    }

    /** Valida que la zona horaria sea un IANA ZoneId; devuelve null si viene vacía. */
    private static String normalizeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return null;
        }
        String trimmed = timeZone.trim();
        try {
            java.time.ZoneId.of(trimmed);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid time zone: " + trimmed);
        }
        return trimmed;
    }

    @Transactional
    public TenantResponse changeStatus(UUID id, TenantStatusUpdateRequest request) {
        Tenant tenant = require(id);
        tenant.changeStatus(request.status());
        return TenantResponse.from(tenant);
    }

    private Tenant require(UUID id) {
        return tenants.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private static GeoLocation toGeoLocation(GeoPointDto location) {
        return location == null ? null : new GeoLocation(location.latitude(), location.longitude());
    }

    private static String normalizeCountry(String country) {
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String uniqueSlug(String name) {
        String base = slugify(name);
        String slug = base;
        int suffix = 2;
        while (tenants.existsBySlug(slug)) {
            String numeric = "-" + suffix++;
            slug = base.substring(0, Math.min(base.length(), SLUG_MAX - numeric.length())) + numeric;
        }
        return slug;
    }

    private static String slugify(String name) {
        String slug = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            slug = "tenant";
        }
        return slug.length() > SLUG_MAX ? slug.substring(0, SLUG_MAX) : slug;
    }
}
