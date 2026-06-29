package co.com.hermes.calendar.tenant.publicapi;

import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Lectura pública de un establecimiento activo (para la vitrina/exploración, sin sesión). */
@Service
public class PublicOrganizationService {

    private final TenantRepository tenants;

    public PublicOrganizationService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public PublicOrganizationResponse get(UUID id) {
        Tenant tenant = tenants.findById(id)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return PublicOrganizationResponse.from(tenant);
    }
}
