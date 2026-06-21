package co.com.hermes.calendar.tenant.admin;

import co.com.hermes.calendar.tenant.tenant.Tenant;
import co.com.hermes.calendar.tenant.tenant.TenantRepository;
import co.com.hermes.calendar.tenant.tenant.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantAdminServiceTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantAdminService service = new TenantAdminService(tenants);

    private TenantCreateRequest sampleCreate() {
        return new TenantCreateRequest(
                "Cafe Central",
                "900123456-7",
                "co",
                "Bogota",
                "Cra 7 # 71-21",
                "Cafeteria de especialidad",
                new GeoPointDto(new BigDecimal("4.710989"), new BigDecimal("-74.072092"))
        );
    }

    @Test
    void registersTenantWithSlugCountryNormalizedAndActiveStatus() {
        when(tenants.existsByTaxId("900123456-7")).thenReturn(false);
        when(tenants.existsBySlug(anyString())).thenReturn(false);
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponse response = service.create(sampleCreate());

        assertThat(response.name()).isEqualTo("Cafe Central");
        assertThat(response.slug()).isEqualTo("cafe-central");
        assertThat(response.taxId()).isEqualTo("900123456-7");
        assertThat(response.country()).isEqualTo("CO");
        assertThat(response.city()).isEqualTo("Bogota");
        assertThat(response.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(response.location().latitude()).isEqualByComparingTo("4.710989");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateTaxIdOnCreate() {
        when(tenants.existsByTaxId("900123456-7")).thenReturn(true);

        assertThatThrownBy(() -> service.create(sampleCreate()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void appendsSuffixWhenSlugAlreadyExists() {
        when(tenants.existsByTaxId(anyString())).thenReturn(false);
        when(tenants.existsBySlug("cafe-central")).thenReturn(true);
        when(tenants.existsBySlug("cafe-central-2")).thenReturn(false);
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantResponse response = service.create(sampleCreate());

        assertThat(response.slug()).isEqualTo("cafe-central-2");
    }

    @Test
    void marksTenantInactive() {
        UUID id = UUID.randomUUID();
        Tenant tenant = Tenant.register(id, "cafe-central", "Cafe Central", "900123456-7", "CO", "Bogota", null, null, null);
        when(tenants.findById(id)).thenReturn(Optional.of(tenant));

        TenantResponse response = service.changeStatus(id, new TenantStatusUpdateRequest(TenantStatus.INACTIVE));

        assertThat(response.status()).isEqualTo(TenantStatus.INACTIVE);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);
    }

    @Test
    void failsToUpdateMissingTenant() {
        UUID id = UUID.randomUUID();
        when(tenants.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new TenantUpdateRequest(
                "X", "111", "CO", "Bogota", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
