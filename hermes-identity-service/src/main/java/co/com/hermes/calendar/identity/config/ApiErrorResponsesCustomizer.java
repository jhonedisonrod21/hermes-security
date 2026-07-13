package co.com.hermes.calendar.identity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Documenta en el OpenAPI las respuestas de error estándar (400/401/403/404/500) con cuerpo
 * {@code ProblemDetail} (RFC 7807) en toda operación que no las declare explícitamente. Springdoc
 * detecta este bean {@link OpenApiCustomizer} automáticamente. Complementa al {@link GlobalExceptionHandler}.
 */
@Component
class ApiErrorResponsesCustomizer implements OpenApiCustomizer {

    private static final String PROBLEM_SCHEMA = "ProblemDetail";
    private static final Map<String, String> ERRORS = Map.of(
            "400", "Petición inválida (entrada malformada o que incumple validación)",
            "401", "No autenticado (falta token o es inválido)",
            "403", "Autenticado pero sin permisos sobre el recurso",
            "404", "Recurso no encontrado",
            "500", "Error interno del servidor");

    @Override
    public void customise(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        components.addSchemas(PROBLEM_SCHEMA, problemDetailSchema());

        Content problemContent = new Content().addMediaType(
                "application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA)));

        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    ApiResponses responses = operation.getResponses();
                    ERRORS.forEach((code, description) -> {
                        if (!responses.containsKey(code)) {
                            responses.addApiResponse(code,
                                    new ApiResponse().description(description).content(problemContent));
                        }
                    });
                }));
    }

    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema().format("uri"));
    }
}
