package co.com.hermes.calendar.tenant.self;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** Una organización (tenant) a la que pertenece el usuario, con su(s) rol(es) en ella. */
@Schema(description = "Organización a la que pertenece el usuario.")
public record OrganizationResponse(UUID tenantId, String slug, String name, List<String> roles) {
}
