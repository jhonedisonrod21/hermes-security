package co.com.hermes.calendar.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authServerOpenAPI() {
        return new OpenAPI()
                // Servers relativos: "Via API Gateway" (prefijo /auth) para la vista agregada;
                // "Acceso directo" para los flujos OAuth2/OIDC contra el issuer real.
                .servers(List.of(
                        new Server().url("/auth").description("Via API Gateway"),
                        new Server().url("/").description("Acceso directo (issuer OAuth2)")))
                .info(new Info()
                        .title("Hermes Auth Server API")
                        .version("0.0.1")
                        .description("Servidor OAuth2/OIDC de Hermes. Valida credenciales contra Identity Service, "
                                + "consulta Tenant Service y emite access_token, refresh_token e id_token con claims "
                                + "Hermes de usuario, tenant, roles y permisos."))
                .components(new Components()
                        .addSecuritySchemes("client-basic", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Autenticacion de cliente OAuth2 para /oauth2/token."))
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .paths(new Paths()
                        .addPathItem("/oauth2/authorize", authorizePath())
                        .addPathItem("/login", loginPath())
                        .addPathItem("/oauth2/token", tokenPath())
                        .addPathItem("/oauth2/jwks", jwksPath())
                        .addPathItem("/.well-known/openid-configuration", discoveryPath())
                        .addPathItem("/userinfo", userInfoPath()));
    }

    private PathItem authorizePath() {
        return new PathItem().get(new Operation()
                .tags(List.of("OAuth2 Authorization Code"))
                .summary("Inicia Authorization Code con PKCE")
                .description("Redirige al formulario de login si no existe sesion. Tras autenticar, devuelve un "
                        + "authorization code al redirect_uri registrado.")
                .addParametersItem(query("response_type", "Debe ser code.", "code", true))
                .addParametersItem(query("client_id", "Cliente OAuth2 registrado.", "hermes-web-client", true))
                .addParametersItem(query("redirect_uri", "URI registrada del cliente.", "https://oauth.pstmn.io/v1/callback", true))
                .addParametersItem(query("scope", "Scopes solicitados.", "openid profile", true))
                .addParametersItem(query("state", "Valor opaco para correlacion del cliente.", "postman-state", false))
                .addParametersItem(query("nonce", "Nonce OIDC para id_token.", "postman-nonce", false))
                .addParametersItem(query("code_challenge", "PKCE code challenge S256.", "6zxA75iB7CNu8QTsB4HBsy4zaigq8l21bb0PVFgGhi4", true))
                .addParametersItem(query("code_challenge_method", "Metodo PKCE.", "S256", true))
                .responses(new ApiResponses()
                        .addApiResponse("302", new ApiResponse().description("Redireccion a /login o al redirect_uri con code."))
                        .addApiResponse("400", new ApiResponse().description("Solicitud OAuth2 invalida."))));
    }

    private PathItem loginPath() {
        Schema<?> loginSchema = new Schema<>()
                .type("object")
                .addProperty("username", new Schema<>().type("string").example("admin@hermes.local"))
                .addProperty("password", new Schema<>().type("string").format("password").example("admin123"))
                .addProperty("_csrf", new Schema<>().type("string").description("Token CSRF generado por Spring Security."));

        return new PathItem()
                .get(new Operation()
                        .tags(List.of("OAuth2 Login"))
                        .summary("Formulario de login")
                        .description("Pagina HTML generada por Spring Security para autenticar al resource owner.")
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("Formulario HTML de login."))))
                .post(new Operation()
                        .tags(List.of("OAuth2 Login"))
                        .summary("Procesa credenciales del usuario")
                        .description("Valida credenciales usando Identity Service y contexto de tenant usando Tenant Service.")
                        .requestBody(new RequestBody().required(true).content(form(loginSchema)))
                        .responses(new ApiResponses()
                                .addApiResponse("302", new ApiResponse().description("Login aceptado y redireccion al flujo OAuth2."))
                                .addApiResponse("401", new ApiResponse().description("Credenciales invalidas."))));
    }

    private PathItem tokenPath() {
        Schema<?> tokenSchema = new Schema<>()
                .type("object")
                .addProperty("grant_type", new Schema<>().type("string").example("authorization_code"))
                .addProperty("code", new Schema<>().type("string").description("Authorization code recibido en redirect_uri."))
                .addProperty("redirect_uri", new Schema<>().type("string").example("https://oauth.pstmn.io/v1/callback"))
                .addProperty("code_verifier", new Schema<>().type("string").example("hermes-local-code-verifier-123456789012345678901234567890"));

        return new PathItem().post(new Operation()
                .tags(List.of("OAuth2 Authorization Code"))
                .summary("Intercambia authorization code por tokens")
                .description("Devuelve access_token, refresh_token e id_token. El access_token incluye user_id, tenant_id, "
                        + "tenant_slug, roles y permissions.")
                .addSecurityItem(new SecurityRequirement().addList("client-basic"))
                .requestBody(new RequestBody().required(true).content(form(tokenSchema)))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Tokens emitidos."))
                        .addApiResponse("400", new ApiResponse().description("Grant invalido, code expirado o PKCE invalido."))
                        .addApiResponse("401", new ApiResponse().description("Cliente OAuth2 invalido."))));
    }

    private PathItem jwksPath() {
        return new PathItem().get(new Operation()
                .tags(List.of("OIDC Discovery"))
                .summary("Publica las llaves JWK")
                .description("Llaves publicas RSA usadas por gateway y microservicios para validar JWT.")
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("JWK Set."))));
    }

    private PathItem discoveryPath() {
        return new PathItem().get(new Operation()
                .tags(List.of("OIDC Discovery"))
                .summary("Metadata OIDC del Auth Server")
                .description("Describe issuer, authorization_endpoint, token_endpoint, jwks_uri y capacidades OIDC.")
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Documento de descubrimiento OIDC."))));
    }

    private PathItem userInfoPath() {
        return new PathItem().get(new Operation()
                .tags(List.of("OIDC UserInfo"))
                .summary("Consulta informacion del usuario autenticado")
                .description("Endpoint OIDC protegido con Bearer token.")
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Claims del usuario."))
                        .addApiResponse("401", new ApiResponse().description("Token ausente o invalido."))));
    }

    private Parameter query(String name, String description, String example, boolean required) {
        return new Parameter()
                .in("query")
                .name(name)
                .description(description)
                .required(required)
                .schema(new Schema<>().type("string").example(example));
    }

    private Content form(Schema<?> schema) {
        return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                new MediaType().schema(schema));
    }
}
