package co.com.hermes.calendar.tenant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI tenantServiceOpenAPI() {
        return new OpenAPI()
                // Servers relativos: el primero enruta "Try it out" por el gateway (prefijo /tenant)
                // para la vista agregada; el segundo prueba el servicio directamente (Swagger UI propio).
                .servers(List.of(
                        new Server().url("/tenant").description("Via API Gateway"),
                        new Server().url("/").description("Acceso directo al servicio")))
                .info(new Info()
                        .title("Hermes Tenant Service API")
                        .version("0.0.1")
                        .description("Gestiona tenants, membresias, roles de tenant y permisos funcionales. "
                                + "El endpoint interno de contexto de tenant es consumido por hermes-auth-server."))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("hermes-internal-key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Hermes-Internal-Key")));
    }
}
